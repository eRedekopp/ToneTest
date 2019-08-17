package ca.usask.cs.tonesetandroid.Control;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.SinglePitchTone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Contains methods and values for audio output and stores/handles saved test results and the mathematical model for
 * calculating probabilities
 *
 * @author redekopp, alexscott
 */
@SuppressWarnings("JavadocReference")
public class Model {

    private ArrayList<ModelListener> subscribers;

    private AudioManager audioManager;

    /////////////// Stored hearing test results ///////////////
    CalibrationTestResults calibrationTestResults;   // final results of test

    /////////////// Vars/values for audio ///////////////
    public AudioTrack lineOut;
    public static final int OUTPUT_SAMPLE_RATE  = 44100;  // output samples at 44.1 kHz always
    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    public int duration_ms; // how long to play each tone in a test
    public static int MIN_AUDIO_BUF_SIZE = AudioTrack.getMinBufferSize(OUTPUT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                                                                         AudioFormat.ENCODING_PCM_16BIT);
    public static byte[] buf = new byte[MIN_AUDIO_BUF_SIZE];    // a byte buffer that will always be in memory
                                                                // For some reason, using a buf saved in a function's
                                                                // scope will occasionally crash the entire app

    /////////////// Vars for file io ///////////////
    private int subjectId = -1;     // -1 indicates not set

    public Model() {
        subscribers = new ArrayList<>();
        reset();
    }

    /**
     * Resets this model to its just-initialized state.
     */
    public void reset() {
        resetCalibrationTestResults();
    }

    public void resetCalibrationTestResults() {
        this.calibrationTestResults = null;
    }

    /**
     * @return True if this model has hearing test results saved to it, else false
     */
    public boolean hasResults() {
        if (this.calibrationTestResults == null) return false;
        else return ! this.calibrationTestResults.isEmpty();
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

    public int getNumCalibrationTrials() {
        if (! this.hasResults()) throw new IllegalStateException("No results stored in model");
        else return this.calibrationTestResults.getNumOfTrials();
    }

    public double getCalibProbability(SinglePitchTone tone, int n) throws IllegalArgumentException {
        if (! this.hasResults()) throw new IllegalStateException("No results stored in model");
        else {
            CalibrationTestResults newCalibResults = this.calibrationTestResults.getSubsetResults(n);
            return newCalibResults.getProbability(tone);
        }
    }

    /**
     * Get the probability of hearing or differentiating the given tone, given the results stored in this model
     *
     * @param tone The tone whose probability is to be determined
     * @param n The number of trials to use for the sample size (ie. will calculate probabilities based on the first
     *          n trials of each tone from the confidence test), n <= confidence test sample size
     * @return The probability of hearing or differentiating the given tone
     * @throws IllegalArgumentException If n > confidence test sample size
     */
    public double getCalibProbability(Tone tone, int n) throws IllegalArgumentException {
        return this.getCalibProbability(new FreqVolPair(tone.freq(), tone.vol()), n);
    }

    public double getRampProbability(SinglePitchTone tone, int n) {
        return 0.010; // todo
    }

    public double getRampProbability(Tone tone, int n) {
        return 0.0; // todo
    }

    // todo decide what to do with this

//    /**
//     * Gets a short clip of audio from the microphone and returns it as an array of floats
//     *
//     * @param size The number of samples in the clip
//     * @return A float array of the given size representing the PCM values of the recorded clip
//     */
//    public float[] getAudioSample(int size) {
//
//        int MIN_AUDIO_BUF_SIZE = AudioRecord.getMinBufferSize(
//                INPUT_SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT
//        );
//        if (size < MIN_AUDIO_BUF_SIZE)
//            throw new IllegalArgumentException("Audio sample size must have length >= " + MIN_AUDIO_BUF_SIZE);
//
//        // build AudioRecord: input from mic and output as floats
//        AudioRecord recorder;
//        if (Build.VERSION.SDK_INT >= 23)
//            recorder = new AudioRecord.Builder()
//                    .setAudioSource(MediaRecorder.AudioSource.MIC)
//                    .setAudioFormat(new AudioFormat.Builder()
//                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                            .setSampleRate(INPUT_SAMPLE_RATE)
//                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
//                            .build())
//                    .setBufferSizeInBytes(size)
//                    .build();
//        else
//            recorder = new AudioRecord(
//                    MediaRecorder.AudioSource.MIC,
//                    INPUT_SAMPLE_RATE,
//                    AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT,
//                    size
//            );
//        if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED)
//            throw new IllegalStateException("AudioRecord not properly initialized");
//
//        // Record the audio
//        short[] lineData = new short[size];
//        recorder.startRecording();
//        for (int i = 0; i < size; i++) {
//            recorder.read(lineData, i, 1);
//        }
//        recorder.stop();
//        recorder.release();
//
//        // convert to float[]
//        float[] lineDataFloat = new float[size];
//        for (int i = 0; i < size; i++) {
//            lineDataFloat[i] = (float) lineData[i] / (float) Short.MAX_VALUE;
//            if (lineDataFloat[i] > 1) lineDataFloat[i] = 1;
//            else if (lineDataFloat[i] < -1) lineDataFloat[i] = -1;
//        }
//
//        return lineDataFloat;
//    }

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
     * For testing: return PCM samples of a sine wave with the given frequency at INPUT_SAMPLE_RATE
     *
     * @param nSamples The number of samples to generate
     * @param freq The frequency of the sine wave
     * @return PCM float values corresponding to a sine wave of the given frequency
     */
    public static float[] sineWave(float freq, int nSamples, float amplitude) {
        float[] output = new float[nSamples];
        float period = Model.INPUT_SAMPLE_RATE / freq;

        for (int i = 0; i < nSamples; i++) {
            float angle = 2.0f * (float) Math.PI * i / period;
            output[i] = (float) Math.sin(angle) * amplitude;
        }
        return output;
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
     * Forces the volume of the output stream to max
     */
    public void enforceMaxVolume() {
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != maxVol)
            audioManager.setStreamVolume( // pin volume to max if not already done
                    AudioManager.STREAM_MUSIC,
                    maxVol,
                    AudioManager.FLAG_PLAY_SOUND);
    }

    public void addSubscriber(ModelListener newSub) {
        subscribers.add(newSub);
    }

    public void setSubjectId(int id) {
        this.subjectId = id;
    }

    public int getSubjectId() {
        return this.subjectId;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public void pauseAudio() {
        try {
            this.lineOut.pause();
            this.lineOut.flush();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setCalibrationTestResults(CalibrationTestResults calibrationTestResults) {
        this.calibrationTestResults = calibrationTestResults;
    }

    public void startAudio() {
        this.lineOut.play();
    }

    /**
     * Print the contents of calibrationTestResults to the console (for testing)
     */
    public void printResultsToConsole() {
        Log.i("printResultsToConsole", String.format("Subject ID: %d", this.subjectId));
        if (calibrationTestResults == null || calibrationTestResults.isEmpty())
            Log.i("printResultsToConsole", "No results stored in model");
        else {
            Log.i("printResultsToConsole", calibrationTestResults.toString());

            for (float freq : this.calibrationTestResults.getTestedFreqs()) {
                for (double vol : this.calibrationTestResults.getTestedVolumesForFreq(freq)) {
                    Log.d("delete me", String.format("freq %.2f vol %.4f loaded %d tests", freq,
                            vol,
                            this.calibrationTestResults.getNumOfTrials(new FreqVolPair(freq, vol))));
                }
            }


            Log.d("calibrationTestResults", "");
        }
    }

    public CalibrationTestResults getCalibrationTestResults() {
        return this.calibrationTestResults;
    }

    public void notifySubscribers() {
        for (ModelListener m : this.subscribers) m.modelChanged();
    }
}