package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Contains information about the current/most recent tests as well as an interface for generating
 * sine wave audio
 *
 * @author redekopp, alexscott
 */
@SuppressWarnings("JavadocReference")
public class Model {

    private ArrayList<ModelListener> subscribers;

    private AudioManager audioManager;

    // vars/values for hearing test
    private static final float HEARING_TEST_REDUCE_RATE = 0.2f; // reduce by this percentage each time
    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;   // number of times listener must fail to hear a tone in the
                                                        // reduction phase of the hearing test before the volume is
                                                        // considered "inaudible"
    static final int NUMBER_OF_VOLS_PER_FREQ = 6;  // number of volumes to test for each frequency
    static final int NUMBER_OF_TESTS_PER_VOL = 8;  // number of times to repeat each freq-vol combination in the test
    static final int TEST_PHASE_RAMP = 0;       // for identifying which test phase (if any) we are currently in
    static final int TEST_PHASE_REDUCE = 1;
    static final int TEST_PHASE_MAIN = 2;
    static final int TEST_PHASE_CONF = 3;
    static final int TEST_PHASE_NULL = -1;
    private int testPhase = TEST_PHASE_NULL;
    ArrayList<FreqVolPair> topVolEstimates;     // The rough estimates for volumes which have P(heard) = 1
    ArrayList<FreqVolPair> bottomVolEstimates;  // The rough estimates for volumes which have P(heard) = 0
    ArrayList<FreqVolPair> currentVolumes;      // The current volumes being tested
    HashMap<Float, Integer> timesNotHeardPerFreq;   // how many times each frequency was not heard
                                                    // (for finding bottom estimates)
    ArrayList<FreqVolPair> testPairs;  // all the freq-vol combinations that will be tested in the main test
    HearingTestResultsContainer hearingTestResults;   // final results of test
    HearingTestResultsContainer calibrationReslts;
    private boolean testPaused = false; // has the user paused the test?
    boolean testThreadActive = false; // is a thread currently performing a hearing test?
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, /*8000*/};   // From British Society of
                                                                                        // Audiology

    // Vars/values for audio
    AudioTrack lineOut;
    public static final int OUTPUT_SAMPLE_RATE  = 44100; // output samples at 44.1 kHz always
    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;
    private boolean audioPlaying;

    // Vars for file io
    private int subjectId = -1;     // -1 indicates not set
    private boolean resultsSaved = false;       // have hearing test results been saved since the model was initialized?
    private boolean confResultsSaved = false;   // have conf test results been saved since the model was initialized?

    // vars for confidence test
    static final int CONF_NUMBER_OF_TRIALS_PER_FVP = 20;
    ArrayList<FreqVolPair> confidenceTestPairs;  // freq-vol pairs to be tested in the next confidence test
    ConfidenceTestResultsContainer confidenceTestResults;
    ArrayList<ConfidenceTestResultsContainer.StatsAnalysisResultsContainer> analysisResults;
    public static final float[] CONF_FREQS  = {220, 880, 1760, 3520}; // 4 octaves of A
    public static final float[][] CONF_SUBSETS = {  // subsets of hearing test frequencies to test against conf results
            FREQUENCIES,
            {200, 1000, 4000},
            {500, 1000, 2000},
            {500, 2000},
            {1000, 4000},
            {200, 1000}
    };

    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        reset();
    }

    /**
     * Resets this model to its just-initialized state
     */
    public void reset() {
        this.topVolEstimates = new ArrayList<>();
        this.bottomVolEstimates = new ArrayList<>();
        this.currentVolumes = new ArrayList<>();
        this.confidenceTestResults = new ConfidenceTestResultsContainer();
        this.confidenceTestPairs = new ArrayList<>();
        this.analysisResults = new ArrayList<>();
        this.testPairs = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        for (float freq : FREQUENCIES) timesNotHeardPerFreq.put(freq, 0);
        this.hearingTestResults = new HearingTestResultsContainer();
        this.resultsSaved = false;
        this.confResultsSaved = false;
    }

    /**
     * @return True if this model has hearing test results saved to it, else false
     */
    public boolean hasResults() {
        return ! this.hearingTestResults.isEmpty();
    }

    /**
     * @return True if this model has confidence test results saved to it, else false
     */
    public boolean hasConfResults() {
        try {
            return ! this.confidenceTestResults.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean hasAnalysisResults() {
        try {
            return ! this.analysisResults.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
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
            for (double vol = bottomVolEst;
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / NUMBER_OF_VOLS_PER_FREQ) {
                testPairs.add(new FreqVolPair(freq, vol));
            }
        }
        // fill CurrentVolumes with one freqvolpair for each individual tone that will be played in the test
        this.currentVolumes = new ArrayList<>();
        for (int i = 0; i < Model.NUMBER_OF_TESTS_PER_VOL; i++) this.currentVolumes.addAll(this.testPairs);
        Collections.shuffle(this.currentVolumes);
    }

    /**
     * Populate model.confidenceTestPairs with all freqvolpairs that will be tested in the next confidence test
     */
    public void configureConfidenceTestPairs() {

        ArrayList<Float> confFreqs = new ArrayList<>();
        for (float freq : CONF_FREQS) confFreqs.add(freq);

        // randomize the order of test frequencies
        Collections.shuffle(confFreqs);

        // for each frequency, add a new fvp to confidenceTestPairs with the frequency and a volume some percentage
        // of the way between completely inaudible and perfectly audible
        float pct = 0;  // the percentage of the way between the lowest and highest tested vol that this test will be
        float jumpSize = 1.0f / CONF_FREQS.length;
        for (Float freq : confFreqs) {
            double volFloor = this.hearingTestResults.getVolFloorEstimateForFreq(freq);
            double volCeiling = this.hearingTestResults.getVolCeilingEstimateForFreq(freq);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.confidenceTestPairs.add(new FreqVolPair(freq, testVol));
            pct += jumpSize;
        }

        // prepare list of all trials
        ArrayList<FreqVolPair> allTrials = new ArrayList<>();
        for (int i = 0; i < Model.CONF_NUMBER_OF_TRIALS_PER_FVP; i++) {
            allTrials.addAll(this.confidenceTestPairs);
        }
        Collections.shuffle(allTrials);
        this.confidenceTestPairs = allTrials;
    }

    /**
     * Populate this.analysisResults using estimates generated based on the given subset of the tested volumes
     *
     * @param subset The subset of the tested frequencies to be used to generate estimates
     * @throws IllegalStateException If no confidence results are stored in the model
     * @throws IllegalArgumentException If the given subset is not a subset of the tested frequencies
     */
    public void analyzeConfidenceResults(float[] subset) throws IllegalStateException, IllegalArgumentException {
        if (! this.hasConfResults()) throw new IllegalStateException("No confidence results stored");
        this.analysisResults = new ArrayList<>();
        for (FreqVolPair fvp : this.confidenceTestResults.getTestedFVPs())
            this.analysisResults.add(
                    this.confidenceTestResults.performAnalysis(fvp, this.getProbabilityFVP(fvp, subset)));
    }

    /**
     * Find the probability of hearing the given frequency-volume pair given the calibration results
     *
     * @param freq The frequency to be queried
     * @param vol The volume to be queried
     * @return The probability of the given freq-vol pair being heard given the calibration results
     * @throws IllegalStateException If there are no calibration results stored in the model
     */
    public float getProbabilityFVP(float freq, double vol) throws IllegalStateException {
        if (! this.hasResults()) throw new IllegalStateException("No data stored in model");
        return this.hearingTestResults.getProbOfHearingFVP(freq, vol);
    }

    public float getProbabilityFVP(FreqVolPair fvp) throws IllegalStateException {
        return this.getProbabilityFVP(fvp.getFreq(), fvp.getVol());
    }

    public float getProbabilityFVP(FreqVolPair fvp, float[] subset) {
        return this.hearingTestResults.getProbOfHearingFVP(fvp.getFreq(), fvp.getVol(), subset);
    }

    /**
     * Configure the audio in preparation for a hearing test - only call directly before a test
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
//        return sineWave(1500, size, 1f);
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
     * Reduce all elements of currentVolumes by [element * HEARING_TEST_REDUCE_RATE]
     */
    public void reduceCurrentVolumes() {
        ArrayList<FreqVolPair> newVols = new ArrayList<>();
        for (FreqVolPair fvp : currentVolumes) {
            // only reduce volumes of frequencies still being tested
            if (timesNotHeardPerFreq.get(fvp.getFreq()) >= TIMES_NOT_HEARD_BEFORE_STOP) newVols.add(fvp);
            else newVols.add(new FreqVolPair(fvp.getFreq(), fvp.getVol() * (1 - HEARING_TEST_REDUCE_RATE)));
        }
        this.currentVolumes = newVols;
    }

    /**
     * Perform first time setup of the audio track - does nothing if audio track already initialized
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
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != maxVol)
            audioManager.setStreamVolume( // pin volume to max if not already done
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_PLAY_SOUND);
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

    public void setResultsSaved(boolean b) {
        this.resultsSaved = b;
        this.notifySubscribers();
    }

    public boolean resultsSaved() {
        return resultsSaved;
    }

    public void setConfResultsSaved(boolean b) {
        this.confResultsSaved = b;
        this.notifySubscribers();
    }

    public boolean confResultsSaved() {
        return confResultsSaved;
    }

    public void stopAudio() {
        this.audioPlaying = false;
        this.lineOut.pause();
        this.lineOut.flush();
    }

    public void startAudio() {
        this.audioPlaying = true;
        this.lineOut.play();
    }

    public int getTestPhase() {
        return this.testPhase;
    }

    public void setTestPhase(int phase) {
        this.testPhase = phase;
        this.notifySubscribers();
    }

    public void setTestPaused(boolean b) {
        this.testPaused = b;
        this.notifySubscribers();
    }

    public boolean testPaused() {
        return this.testPaused;
    }

    public boolean testing() {
        return this.testPhase != TEST_PHASE_NULL;
    }

    public ArrayList<FreqVolPair> getCurrentVolumes() {
        return currentVolumes;
    }

    public void setCurrentVolumes(ArrayList<FreqVolPair> currentVolumes) {
        this.currentVolumes = currentVolumes;
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
        Log.i("printResultsToConsole", "Subject ID: " + this.subjectId);
        if (hearingTestResults.isEmpty()) Log.i("printResultsToConsole", "No results stored in model");
        else Log.i("printResultsToConsole", hearingTestResults.toString());
    }

    public HearingTestResultsContainer getHearingTestResults() {
        return this.hearingTestResults;
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

    public void notifySubscribers() {
        for (ModelListener m : this.subscribers) m.modelChanged();
    }

    public static float min(float[] arr) {
        float min = Float.MAX_VALUE;
        for (float f : arr) if (f < min) min = f;
        return min;
    }

    public static float max(float[] arr) {
        float max = Float.MIN_VALUE;
        for (float f : arr) if (f > max) max = f;
        return max;
    }

}