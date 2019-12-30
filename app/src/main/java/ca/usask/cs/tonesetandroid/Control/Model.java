package ca.usask.cs.tonesetandroid.Control;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResultsCollection;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResultsWithFloorInfo;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;
import ca.usask.cs.tonesetandroid.MainActivity;
import ca.usask.cs.tonesetandroid.Participant;

/**
 * Contains methods and values for audio, and stores/handles saved test results
 *
 * @author redekopp, alexscott
 */
@SuppressWarnings("JavadocReference")
public class Model {

    /**
     * A list of ModelListeners to be notified when the state of this Model changes
     */
    private ArrayList<ModelListener> subscribers;

    /**
     * The Participant object containing information about the person currently taking a test
     */
    private Participant currentParticipant;


    /////////////// Vars/values for audio ///////////////

    /**
     * AudioManager for audio output
     */
    private AudioManager audioManager;

    /**
     * The AudioTrack through which to play audio
     */ 
    public AudioTrack lineOut;

    public static final int OUTPUT_SAMPLE_RATE  = 44100;

    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    
    /**
     * Minimum size for the audio buffer for lineOut
     */ 
    public static int MIN_AUDIO_BUF_SIZE =
                AudioTrack.getMinBufferSize(OUTPUT_SAMPLE_RATE,
                                                AudioFormat.CHANNEL_OUT_MONO,
                                                AudioFormat.ENCODING_PCM_16BIT);

    /**
     * A byte buffer that is always in memory - necessary for writing to lineOut because locally stored
     * buffers cause everything to crash 
     */
    public static byte[] buf = new byte[2 * MIN_AUDIO_BUF_SIZE];

    public Model() {
        subscribers = new ArrayList<>();
    }

    /**
     * @return True if this model has ConfidenceTest and RampTest results saved to it, else false
     */
    public boolean hasResults() {
        return ! this.currentParticipant.getResults().isEmpty();
    }

    /**
     * Configure the audio in preparation for a hearing test - only call directly before a test
     */
    public void configureAudio() {
        this.setUpLineOut();
        this.enforceMaxVolume();
    }

    /**
     * Close out audio line after audio play complete - only call directly after a test
     */
    public void audioTrackCleanup() {
        try {
            this.lineOut.stop();
            this.lineOut.flush();
            this.lineOut.release();
        } catch (IllegalStateException e) {
            Log.i("audioTrackCleanup", "IllegalStateException caused");
            e.printStackTrace();
        }
    }

    /**
     * Applies a Hann Window to the data set to improve the overall accuracy
     * This function is slightly less general than a typical Hann window function
     * Typically, you also want to know the starting index of the data to be windowed
     * In this case, the index will always begin at 0
     *
     * @param data The data that will be windowed
     * @return The windowed data set
     *
     * @author alexscott
     */
    private static float[] applyHannWindow(float[] data) {
        int length = data.length;
        for (int i = 0; i < length; i++) {
            data[i] = (float) (data[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / length)));
        }
        return data;
    }

    /**
     * Get a periodogram from the given raw PCM data
     */
    public static FreqVolPair[] getPeriodogramFromPcmData(float[] rawPCM) {

        int freqBinWidth = Model.INPUT_SAMPLE_RATE / rawPCM.length;

        // Object for performing FFTs: handle real inputs of size sampleSize
        NoiseOptimized noise = Noise.real().optimized().init(rawPCM.length, true);

        // apply Hann window to reduce noise
        float[] fftInput = applyHannWindow(rawPCM);

        // perform FFT
        float[] fftResult = noise.fft(fftInput); // noise.fft() returns from DC bin to Nyquist bin, no slicing req'd

        // convert to power spectral density
        float[] psd =  new float[fftInput.length / 2 + 1];
        for (int i = 0; i < fftResult.length / 2; i++) {
            float realPart = fftResult[i * 2];
            float imagPart = fftResult[i * 2 + 1];

// Power Spectral Density in dB = magnitude(fftResult) ^ 2
// From StackOverflow user Jason R
// https://dsp.stackexchange.com/questions/4691/what-is-the-difference-between-psd-and-squared-magnitude-of-frequency-spectrum?lq=1
            psd[i] = (float) Math.pow(Math.sqrt(Math.pow(realPart, 2) + Math.pow(imagPart, 2)), 2);
        }

        FreqVolPair[] freqBins = new FreqVolPair[psd.length];
        for (int i = 0; i < psd.length; i++) {
            float freq = (float) i * freqBinWidth + freqBinWidth / 2.0f;
            double vol = psd[i];
            freqBins[i] = new FreqVolPair(freq, vol);
        }
        return freqBins;
    }

    /**
     * Given an ID for a .wav file, return the most prominent frequencies in each sample for some number of
     * evenly-spaced samples.
     *
     * @param wavResId The resource ID for the wav file to be tested
     * @param nSamples The number of samples to test from the file (fewer samples -> faster, less precise)
     * @param nFreqsPerSample The number of most prominent frequencies to return for each sample
     * @return An array of length nSamples containing the most prominent frequencies in each sample
     */
    public static float[][] topNFrequencies(int wavResId, int nSamples, int nFreqsPerSample) {
        int sampleSize = 1000;
        InputStream rawPCM = MainActivity.context.getResources().openRawResource(wavResId);
        byte[] buf = new byte[2];
        float[] pcm = new float[sampleSize];
        float[][] results = new float[nSamples][];
        int nSamplesTaken = 0;

        try {
            int size = rawPCM.available() / 2; // /2 because each sample is 2 bytes
            for (int i = 0;
                 i < size - sampleSize;
                 i += (size - nSamples * sampleSize) / nSamples) {

                for (int j = 0; j < sampleSize; j++, i++) {      // populate pcm for current set of samples
                    rawPCM.read(buf, 0, 2);       // read data from stream

                    short sample = (short) (buf[1] << 8 | buf[0] & 0xFF);  // convert to short
                    pcm[j] = (float) sample / (float) Short.MIN_VALUE;
                }

                FreqVolPair[] periodogram = Model.getPeriodogramFromPcmData(pcm);   // get fft of pcm data
                FreqVolPair[] max = FreqVolPair.maxNVols(periodogram, nFreqsPerSample);
                float[] maxFreqs = new float[max.length];
                for (int j = 0; j < nFreqsPerSample; j++) maxFreqs[j] = max[j].freq();
                results[nSamplesTaken++] = maxFreqs;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Given an ID for a .wav file, return the most prominent frequencies present in the audio
     *
     * @param wavResId The resource ID for the wav file to be tested
     * @param nSamples The number of samples to test from the file (fewer samples -> faster, less precise)
     * @return An array of length nSamples containing the most prominent frequencies in each sample
     */
    public static float[] topFrequencies(int wavResId, int nSamples) {
        int sampleSize = 1000;
        InputStream rawPCM = MainActivity.context.getResources().openRawResource(wavResId);
        byte[] buf = new byte[2];
        float[] pcm = new float[sampleSize];
        float[] results = new float[nSamples];
        int nSamplesTaken = 0;

        try {
            int size = rawPCM.available() / 2; // /2 because each sample is 2 bytes
            for (int i = 0;
                 i < size - sampleSize;
                 i += (size - nSamples * sampleSize) / nSamples) {

                for (int j = 0; j < sampleSize; j++, i++) {      // populate pcm for current set of samples
                    rawPCM.read(buf, 0, 2);       // read data from stream

                    byte b = buf[0];              // convert to big-endian
                    buf[0] = buf[1];
                    buf[1] = b;
                    short sample = ByteBuffer.wrap(buf).getShort();           // convert to short
                    pcm[j] = (float) sample / (float) Short.MIN_VALUE;
                }

                FreqVolPair[] periodogram = Model.getPeriodogramFromPcmData(pcm);   // get fft of pcm data
                FreqVolPair max = FreqVolPair.maxVol(periodogram);
                results[nSamplesTaken++] = max.freq();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Perform first time setup of the audio track - does nothing if audio track already initialized
     */
    public void setUpLineOut() {
        // do not run if line already initialized
        if (lineOut == null || lineOut.getState() == AudioTrack.STATE_UNINITIALIZED) {
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat format =
                    new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                            .setSampleRate(OUTPUT_SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            lineOut = new AudioTrack(audioAttributes, format, MIN_AUDIO_BUF_SIZE,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            lineOut.setVolume(1.0f); // unity gain - no amplification
        }
    }

    /**
     * Set the volume of the output stream to max if not already done
     */
    public void enforceMaxVolume() {
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != maxVol)
            audioManager.setStreamVolume( 
                    AudioManager.STREAM_MUSIC,
                    maxVol,
                    AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * Add a new ModelListener to the internal list
     */
    public void addSubscriber(ModelListener newSub) {
        subscribers.add(newSub);
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public void setCurrentParticipant(Participant p) {
        this.currentParticipant = p;
    }

    public Participant getCurrentParticipant() {
        return this.currentParticipant;
    }

    /**
     * Pause the AudioTrack
     */
    public void pauseAudio() {
        try {
            this.lineOut.pause();
            this.lineOut.flush();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void printResultsToConsole() {
        // TODO
        Log.i("Model", "TODO: finish printResultsToConsole()");
    }

    /**
     * Un-pause the AudioTrack 
     */
    public void startAudio() {
        this.lineOut.play();
    }

    /**
     * Notify all ModelListeners that the current state has changed
     */
    public void notifySubscribers() {
        for (ModelListener m : this.subscribers) m.modelChanged();
    }
}