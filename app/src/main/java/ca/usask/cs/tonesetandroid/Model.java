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
@SuppressWarnings("JavadocReference")
public class Model {

    private ArrayList<ModelListener> subscribers;

    private AudioManager audioManager;

    private boolean audioPlaying;

    // vars/values for hearing test
    private static final float HEARING_TEST_REDUCE_RATE = 0.1f; // reduce by 10% each time
    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;
    static final int NUMBER_OF_VOLS_PER_FREQ = 5;  // number of volumes to test for each frequency
    static final int NUMBER_OF_TESTS_PER_VOL = 3;  // number of times to repeat each freq-vol combination in the test
    ArrayList<FreqVolPair> topVolEstimates;     // The rough estimates for volumes which have P(heard) = 1
    ArrayList<FreqVolPair> bottomVolEstimates;  // The rough estimates for volumes which have P(heard) = 0
    ArrayList<FreqVolPair> currentVolumes;      // The current volumes being tested
    HashMap<Float, Integer> timesNotHeardPerFreq;   // how many times each frequency was not heard
                                                    // (for finding bottom estimates)
    ArrayList<FreqVolPair> testPairs;  // all the freq-vol combinations that will be tested in the main test
    HearingTestResultsContainer testResults;  // final results of test

    // Vars/values for audio
    AudioTrack lineOut;
    public static final int OUTPUT_SAMPLE_RATE  = 44100; // output samples at 44.1 kHz always
    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    // todo uncomment extra frequencies after testing comlpete
    public static final float[] FREQUENCIES = {/*200, 500, */ 1000, /*2000, 4000, 8000*/}; // From British Society of Audiology
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;

    // Vars for participant config
    private int subjectId = -1;     // -1 indicates not set

    // vars for confidence test
    static final int CONF_NUMBER_OF_TRIALS_PER_FVP = 6;
    ArrayList<FreqVolPair> confidenceTestPairs;  // freq-vol pairs to be tested in the next confidence test
    ArrayList<FreqVolPair> confidenceCalibPairs; // freq-vol pairs used to calibrate the next confidence test
    ArrayList<ConfidenceSingleTestResult> confidenceTestResults;

    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        clearConfidenceResults();
        clearResults();
    }

    /**
     * Clear any confidence results saved in the model
     */
    public void clearConfidenceResults() {
        confidenceTestPairs = new ArrayList<>();
        confidenceTestResults = new ArrayList<>();
        confidenceCalibPairs = new ArrayList<>();
    }

    /**
     * Clear any calibration results saved in the model
     */
    public void clearResults() {
        this.topVolEstimates = new ArrayList<>();
        this.bottomVolEstimates = new ArrayList<>();
        this.currentVolumes = new ArrayList<>();
        this.confidenceTestResults = new ArrayList<>();
        this.testPairs = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        for (float freq : FREQUENCIES) timesNotHeardPerFreq.put(freq, 0);
        this.testResults = new HearingTestResultsContainer();
    }

    /**
     * @return True if this model has hearing test results saved to it, else false
     */
    public boolean hasResults() {
        return ! this.testResults.isEmpty();
    }

    /**
     * @return True if there are still frequencies that need to be tested, else False
     */
    public boolean continueTest() {
        if (currentVolumes.isEmpty()) return false;
        else
            for (Integer n : timesNotHeardPerFreq.values()) if (n < TIMES_NOT_HEARD_BEFORE_STOP) return true;
        return false;
    }

    /**
     * Set currentVolumes to contain all frequencies and volumes to be tested during the main stage of the hearing test
     */
    public void configureTestPairs() {
        for (float freq : FREQUENCIES) {
            double bottomVolEst = getVolForFreq(bottomVolEstimates, freq);
            double topVolEst = getVolForFreq(topVolEstimates, freq);
            for (double vol = bottomVolEst; // todo does this add an extra one to the list since it's <= ?
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / NUMBER_OF_VOLS_PER_FREQ) {
                testPairs.add(new FreqVolPair(freq, vol));
            }
        }

        // todo delete this after making sure this works
        if (testPairs.size() > NUMBER_OF_VOLS_PER_FREQ * FREQUENCIES.length)
            Log.e(  "ConfigureTestPairs",
                    String.format("TestPairs contains %d pairs, expected %d",
                    testPairs.size(), NUMBER_OF_VOLS_PER_FREQ * FREQUENCIES.length)
            );
    }

    public void configureConfidenceTestPairs() {
        // todo
    }

    public float getProbabilityFVP(float freq, double vol) {
        if (! this.hasResults()) throw new IllegalStateException("No data stored in model");
        return this.testResults.getProbOfHearingFVP(freq, vol);
    }

    public float getProbabilityFVP(FreqVolPair fvp) {
        if (! this.hasResults()) throw new IllegalStateException("No data stored in model");
        return this.testResults.getProbOfHearingFVP(fvp.getFreq(), fvp.getVol());
    }

    /**
     * Configure the audio in preparation for a PureTone test - only call directly before a test
     */
    public void configureAudio() {
        this.setUpLineOut();
        this.enforceMaxVolume();
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
    public static float[] sineWave(int freq, int nSamples) {
        float[] output = new float[nSamples];
        float period = Model.INPUT_SAMPLE_RATE / (float) freq;

        for (int i = 0; i < nSamples; i++) {
            float angle = 2.0f * (float) Math.PI * i / period;
            output[i] = (float) Math.sin(angle);
        }
        return output;
    }

    /**
     * Reduce all elements of currentVolumes by
     */
    public void reduceCurrentVolumes() {
        for (FreqVolPair fvp : currentVolumes) {
            // only reduce volumes of frequencies still being tested
            if (timesNotHeardPerFreq.get(fvp.getFreq()) >= TIMES_NOT_HEARD_BEFORE_STOP) continue;
            currentVolumes.remove(fvp);
            currentVolumes.add(new FreqVolPair(fvp.getFreq(), fvp.getVol() * (1 - HEARING_TEST_REDUCE_RATE)));
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
    public void enforceMaxVolume() {
        audioManager.setStreamVolume( // pin volume to max
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_PLAY_SOUND);
    }

    public float getProbOfHearing(FreqVolPair fvp) {
        float freq = fvp.getFreq();
        double vol = fvp.getVol();

        // find frequencies tested just above and below fvp.freq
        float freqAbove = findNearestAbove(fvp.getFreq(), this.testResults.getFreqs());
        float freqBelow = findNearestBelow(fvp.getFreq(), this.testResults.getFreqs());

        // find the probabilities of each of these frequencies
        float probAbove = this.testResults.getProbOfHearingFVP(freqAbove, vol);
        float probBelow = this.testResults.getProbOfHearingFVP(freqBelow, vol);

        // how far of the way between freqBelow and freqAbove is fvp.freq?
        float pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
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
        return resultMap.get(getClosestKey(freq, resultMap));
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

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public boolean audioPlaying() {
        return audioPlaying;
    }

    public void stopAudio() {
        this.audioPlaying = false;
        this.lineOut.pause();
    }

    public void startAudio() {
        this.audioPlaying = true;
        this.lineOut.play();
    }

    public ArrayList<FreqVolPair> getCurrentVolumes() {
        return currentVolumes;
    }

    public void setCurrentVolumes(ArrayList<FreqVolPair> currentVolumes) {
        this.currentVolumes = currentVolumes;
    }

    /**
     * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being greater than f
     */
    public static float findNearestAbove(float f, Float[] lst) {
        float closest = -1f;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < freq - f && freq - f < distance) {
                closest = freq;
                distance = freq - f;
            }
        }
        return closest;
    }

    /**
     * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being less than f
     */
    public static float findNearestBelow(float f, Float[] lst) {
        float closest = -1f;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < f - freq && f - freq < distance) {
                closest = freq;
                distance = f - freq;
            }
        }
        return closest;
    }

    /**
     * Given a list of FreqVolPairs, return the volume associated with the given frequency in a pair
     *
     * @param list A list of freqvolpairs
     * @param freq The frequency whose corresponding volume is to be returned
     * @throws IllegalArgumentException if there is no pair with the given frequency
     */
    public static double getVolForFreq(List<FreqVolPair> list, Float freq) throws IllegalArgumentException {
        for (FreqVolPair fvp : list) if (fvp.getFreq() == freq) return fvp.getVol();
        throw new IllegalArgumentException("Requested frequency not present in list");
    }

    /**
     * Print the contents of hearingTestResults to the console (for testing)
     */
    public void printResultsToConsole() {
        if (testResults.isEmpty()) Log.i("printResultsToConsole", "No results stored in model");
        else Log.i("printResultsToConsole", testResults.toString());
    }

    /**
     * Given a Float:Double hashmap, return the Float key closest to the given float f
     *
     * @param f The number that the answer should be closest to
     * @param map The map containing Float:Double pairs
     * @return The closest key to f in the map
     */
    public static Float getClosestKey(Float f, HashMap<Float, Double> map) {
        Float closest = Float.POSITIVE_INFINITY;
        for (Float cur : map.keySet()) if (Math.abs(cur - f) < Math.abs(closest - f)) closest = cur;
        return closest;
    }

}