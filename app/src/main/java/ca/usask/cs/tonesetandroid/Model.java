package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Contains information about the current/most recent test as well as an interface for generating
 * sine wave audio
 *
 * @author redekopp, alexscott
 */
public class Model {

    private ArrayList<ModelListener> subscribers;

    private AudioManager audioManager;

    private boolean audioPlaying;

    // Vars for audio
    AudioTrack lineOut;
    public static final int OUTPUT_SAMPLE_RATE  = 44100; // output samples at 44.1 kHz always
    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, 8000}; // From British Society of Audiology
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;

    // Vars for storing results
    // todo clean up getters and setters messed up cause of this
    ArrayList<FreqVolPair> topVolEstimates;     // The rough estimates for volumes which have P(heard) = 1
    ArrayList<FreqVolPair> bottomVolEstimates;  // The rough estimates for volumes which have P(heard) = 0
    ArrayList<FreqVolPair> currentVolumes;      // The current volumes being tested
    private int subjectId = -1;     // -1 indicates not set

    // vars for confidence test
    private ArrayList<FreqVolPair> confidenceTestPairs;  // freq-vol pairs to be tested in the next confidence test
    private ArrayList<FreqVolPair> confidenceCalibPairs; // freq-vol pairs used to calibrate the next confidence test
    public static final float[] CONF_FREQS   = {220, 440, 880, 1760, 3520, 7040};  // 6 octaves of A
    ArrayList<ConfidenceSingleTestResult> confidenceTestResults;

    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        hearingTestResults = new ArrayList<>();
        confidenceTestPairs = new ArrayList<>();
        confidenceTestResults = new ArrayList<>();
    }

    /**
     * Clear any results saved in the model
     */
    public void clearResults() {
        hearingTestResults = new ArrayList<>();
    }

    /**
     * @return True if this model has results saved to it, else false
     */
    public boolean hasResults() {
        return hearingTestResults.size() != 0;
    }

    /**
     * Configure the audio in preparation for a PureTone test - only call directly before a test
     */
    public void configureAudio() {
        this.setUpLineOut();
        this.enforceMaxVoume();
        this.duration_ms = 1500;
        this.audioPlaying = true;
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
        this.audioPlaying = false;
    }

    /**
     * Gets a short clip of audio from the microphone and returns it as an array of floats
     *
     * @param size The number of samples in the clip
     * @return A float array of the given size representing the PCM values of the recorded clip
     */
    public float[] getAudioSample(int size) {

        int minBufferSize = AudioRecord.getMinBufferSize(
                INPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (size < minBufferSize)
            throw new IllegalArgumentException("Audio sample size must have length >= " + minBufferSize);

        // build AudioRecord: input from mic and output as floats
        AudioRecord recorder;
        if (Build.VERSION.SDK_INT >= 23)
            recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(INPUT_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(size)
                .build();
        else
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    INPUT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size
            );
        if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED)
            throw new IllegalStateException("AudioRecord not properly initialized");

        // Record the audio
        short[] lineData = new short[size];
        recorder.startRecording();
        for (int i = 0; i < size; i++) {
            recorder.read(lineData, i, 1);
        }
        recorder.stop();
        recorder.release();

        // convert to float[]
        float[] lineDataFloat = new float[size];
        for (int i = 0; i < size; i++) {
            lineDataFloat[i] = (float) lineData[i] / (float) Short.MAX_VALUE;
            if (lineDataFloat[i] > 1) lineDataFloat[i] = 1;
            else if (lineDataFloat[i] < -1) lineDataFloat[i] = -1;
        }

        return lineDataFloat;
    }

    /**
     * For testing: return PCM samples of a sine wave with the given frequency at INPUT_SAMPLE_RATE
     *
     * @param nSamples The number of samples to generate
     * @param freq The frequency of the sine wave
     * @return PCM float values corresponding to a sine wave of the given frequency
     */
    public float[] sineWave(int freq, int nSamples) {
        float[] output = new float[nSamples];
        float period = Model.INPUT_SAMPLE_RATE / (float) freq;

        for (int i = 0; i < nSamples; i++) {
            float angle = 2.0f * (float) Math.PI * i / period;
            output[i] = (float) Math.sin(angle);
        }
        return output;
    }

    /**
     * Configure the array of frequency volume pairs that are to be used during the confidence test
     * This includes +/- 0%, +/-20%, -30%, and +/-40% above/below the calibration volume of
     * the nearest frequency
     */
    public void configureConfidenceTestPairs(){
        configureConfidenceTestPairs(hearingTestResults);
    }

    /**
     * Same as the other method except calibrate based on the results in calibList instead of hearingTestResults
     *
     * @param calibList A list of calibration frequencies and volumes
     */
    public void configureConfidenceTestPairs(List<FreqVolPair> calibList) {
        confidenceCalibPairs = (ArrayList<FreqVolPair>) calibList;
        for(float freq: CONF_FREQS) {
            double vol = getEstimatedMinVolume(freq, calibList);
            confidenceTestPairs.add(new FreqVolPair(freq, vol));      // +/-0%
            confidenceTestPairs.add(new FreqVolPair(freq, vol*1.2));  // +20%
            confidenceTestPairs.add(new FreqVolPair(freq, vol*0.8));  // -20%
            confidenceTestPairs.add(new FreqVolPair(freq, vol*1.4));  // +40%
            confidenceTestPairs.add(new FreqVolPair(freq, vol*0.6));  // -40%
            confidenceTestPairs.add(new FreqVolPair(freq, vol*0.7));  // -30%
//            confidenceTestPairs.add(new FreqVolPair(freq, vol*1.1));  // +10%
//            confidenceTestPairs.add(new FreqVolPair(freq, vol*0.9));  // -10%
        }
    }

    /**
     * Perform first time setup of the audio track
     */
    public void setUpLineOut() {
        // do not run if line already initialized
        if (lineOut == null || lineOut.getState() == AudioTrack.STATE_UNINITIALIZED) {
            int minBufferSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat format =
                    new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                            .setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            lineOut = new AudioTrack(audioAttributes, format, minBufferSize,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            lineOut.setVolume(1.0f); // unity gain - no amplification
        }
    }

    /**
     * Forces the volume of the output stream to max
     */
    public void enforceMaxVoume() {
        audioManager.setStreamVolume( // pin volume to max
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * Gets the estimated "just audible" volume for the given frequency, given the results of the hearing test
     *
     * @param freq The frequency whose just audible volume is to be estimated
     * @return The estimated just audible volume of freq
     */
    public double getEstimatedMinVolume(float freq) {
        // Estimate by using the volume of the closest calibrated frequency
        if (!this.hasResults()) throw new IllegalStateException("No test results loaded");
        HashMap<Float, Double> resultMap = new HashMap<>();
        for (FreqVolPair p : hearingTestResults) resultMap.put(p.freq, p.vol);
        return resultMap.get(ConfidenceController.getClosestKey(freq, resultMap));
    }

    /**
     * Gets the estimated "just audible" volume for the given frequency, given the results in the calibList
     *
     * @param freq The frequency whose just audible volume is to be estimated
     * @param calibList A list of calibration frequencies and volumes
     * @return The estimated just audible volume of freq
     */
    public double getEstimatedMinVolume(float freq, List<FreqVolPair> calibList) {
        if (calibList.isEmpty()) throw new IllegalArgumentException("Calibration list is empty");
        HashMap<Float, Double> resultMap = new HashMap<>();
        for (FreqVolPair p : calibList) resultMap.put(p.freq, p.vol);
        return resultMap.get(ConfidenceController.getClosestKey(freq, resultMap));
    }

    /**
     * Clear the contents of the arrayList containing the results of the confidence test
     */
    public void clearConfidenceTestResults(){
        this.confidenceTestResults.clear();
    }

    /**
     * Accessor method to return the confidenceTest ArrayList
     * @return the confidence test array list
     */
    public ArrayList<FreqVolPair> getConfidenceTestPairs(){
        return confidenceTestPairs;
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

    public ArrayList<FreqVolPair> getHearingTestResults() {
        return this.hearingTestResults;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public boolean audioPlaying() {
        return audioPlaying;
    }

    public void stopAudio() {
        this.audioPlaying = false;
    }

    /**
     * Print the contents of hearingTestResults to the console (for testing)
     */
    public void printResultsToConsole() {
        StringBuilder builder = new StringBuilder();
        builder.append("### Test Results ###\n");
        builder.append(String.format("Test Subject ID: %d | Test Type: %s\n", this.subjectId, this.lastTestType));
        if (hearingTestResults.isEmpty())
            System.out.println("No results stored in model");
        else for (FreqVolPair fvp : hearingTestResults) {
            builder.append(fvp.toString());
            builder.append('\n');
        }
        Log.i("Model Results", builder.toString());
    }

}
