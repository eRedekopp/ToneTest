package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayList;
import java.util.Arrays;
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

    /////////////// vars/values for hearing test ///////////////
    private static final float HEARING_TEST_REDUCE_RATE = 0.2f; // reduce by this percentage each time
    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;   // number of times listener must fail to hear a tone in the
                                                        // reduction phase of the hearing test before the volume is
                                                        // considered "inaudible"
    static final int NUMBER_OF_VOLS_PER_FREQ = 5;   // number of volumes to test for each frequency
    static final int NUMBER_OF_TESTS_PER_VOL = 5;  // number of times to repeat each freq-vol combination in the test
    static final int TEST_PHASE_RAMP = 0;       // for identifying which test phase (if any) we are currently in
    static final int TEST_PHASE_REDUCE = 1;
    static final int TEST_PHASE_MAIN = 2;
    static final int TEST_PHASE_CONF = 3;
    static final int TEST_PHASE_NULL = -1;
    private int testPhase;
    ArrayList<FreqVolPair> topVolEstimates;     // The rough estimates for volumes which have P(heard) = 1
    ArrayList<FreqVolPair> bottomVolEstimates;  // The rough estimates for volumes which have P(heard) = 0
    ArrayList<FreqVolPair> currentVolumes;      // The current volumes being tested
    HashMap<Float, Integer> timesNotHeardPerFreq;   // how many times each frequency was not heard
    // (for finding bottom estimates)
    ArrayList<FreqVolPair> testPairs;  // all the freq-vol combinations that will be tested in the main test
    HearingTestResultsContainer hearingTestResults;   // final results of test
    private boolean testPaused = false; // has the user paused the test?
    boolean testThreadActive = false; // is a thread currently performing a hearing test?
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, /*8000*/};   // From British Society of
    // Audiology
    static final float INTERVAL_FREQ_RATIO = 1.25f; // 5:4 ratio = major third


    /////////////// Vars/values for audio ///////////////
    AudioTrack lineOut;
    public static final int OUTPUT_SAMPLE_RATE  = 44100;  // output samples at 44.1 kHz always
    public static final int INPUT_SAMPLE_RATE = 16384;    // smaller input sample rate for faster fft
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;
    private boolean audioPlaying;

    /////////////// Vars for file io ///////////////
    private int subjectId = -1;     // -1 indicates not set
    private boolean resultsSaved = false;       // have hearing test results been saved since the model was initialized?
    private boolean confResultsSaved = false;   // have conf test results been saved since the model was initialized?

    /////////////// vars/values for confidence test ///////////////
    static final int CONF_NUMBER_OF_TRIALS_PER_EARCON = 20;
    ArrayList<Earcon> confidenceTestEarcons;  // freq-vol pairs to be tested in the next confidence test
    ConfidenceTestResultsContainer confidenceTestResults;
    ArrayList<ConfidenceTestResultsContainer.StatsAnalysisResultsContainer> analysisResults;

    public static final int[] CONF_SAMP_SIZES = {1, 3, 5, 7, 8, 9, 10}; // alternate values of NUMBER_OF_TESTS_PER_VOL
    // to be tested while analyzing data

    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        reset();
    }

    /**
     * Resets this model to its just-initialized state. Only resets hearingTestResults if it is null - reset those
     * with this.resetHearingTestResults
     */
    public void reset() {
        this.topVolEstimates = new ArrayList<>();
        this.bottomVolEstimates = new ArrayList<>();
        this.currentVolumes = new ArrayList<>();
        this.confidenceTestResults = new ConfidenceTestResultsContainer();
        this.confidenceTestEarcons = new ArrayList<>();
        this.analysisResults = new ArrayList<>();
        this.testPairs = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        for (float freq : FREQUENCIES) timesNotHeardPerFreq.put(freq, 0);
        this.confResultsSaved = false;
        this.testThreadActive = false;
        this.setTestPhase(TEST_PHASE_NULL);
        if (this.hearingTestResults == null) {
            this.hearingTestResults = new HearingTestResultsContainer();
            this.resultsSaved = false;
        }
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
            double topVolEst = getVolForFreq(topVolEstimates, freq) * 1.2;  // Bump up by 20% because ramp stage gives
            for (double vol = bottomVolEst;                                 // low estimates
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

    public void resetConfidenceResults() {
        this.confidenceTestResults = new ConfidenceTestResultsContainer();
    }

    public void resetHearingTestResults() {
        this.hearingTestResults = new HearingTestResultsContainer();
        this.resultsSaved = false;
    }

    /**
     * Populate model.confidenceTestIntervals with all freqvolpairs that will be tested in the next confidence test
     */
    @SuppressWarnings("ConstantConditions")
    public void configureConfidenceTestEarcons() {

       ///////////////////////////////////////////////////
       ////// Earcon selection gets adjusted here ////////
       ///////////////////////////////////////////////////

        // select frequencies / build earcon maps
        Float[] confFreqs = {523f, 1046f, 2093f, 3136f};
        ArrayList<Float> freqList = new ArrayList<>(Arrays.asList(confFreqs));
        HashMap<Float, Integer> earconsUp   = new HashMap<>(),          // set resource IDs for each earcon
                                earconsDown = new HashMap<>(),
                                earconsFlat = new HashMap<>();
        earconsUp.put(523f, R.raw.ec523hzmaritriadupshort);
        earconsUp.put(1046f, R.raw.ec1046hzmaritriadupshort);
        earconsUp.put(2093f, R.raw.ec2093hzmaritriadupshort);
        earconsUp.put(3136f, R.raw.ec3136hzmaritriadupshort);
        earconsDown.put(523f, R.raw.ec523hzmaritriaddownshort);
        earconsDown.put(1046f, R.raw.ec1046hzmaritriaddownshort);
        earconsDown.put(2093f, R.raw.ec2093hzmaritriaddownshort);
        earconsDown.put(3136f, R.raw.ec3136hzmaritriaddownshort);
        earconsFlat.put(523f, R.raw.ec523hzclavneg);
        earconsFlat.put(1046f, R.raw.ec1046hzclavneg);
        earconsFlat.put(2093f, R.raw.ec2093hzclavneg);
        earconsFlat.put(3136f, R.raw.ec3136hzclavneg);

        // set list to contain 2 copies of each frequency
        freqList.addAll(Arrays.asList(confFreqs));
        // randomize the order of test frequencies
        Collections.shuffle(freqList);

        int numEarconsToTest = 6;

        // create list to randomize order of up/down/flat earcons
        ArrayList<Integer> directionList = new ArrayList<>();
        for (int i = 0; i < 2; i++) directionList.addAll(Arrays.asList(
                                    Earcon.DIRECTION_UP, Earcon.DIRECTION_DOWN, Earcon.DIRECTION_NONE));
        Collections.shuffle(directionList);

        // for each frequency, add an upward, downward, and flat earcon at a volume some percentage of the way
        // between estimates for "completely inaudible" and "completely audible" volumes.
        float pct = 0;  // percentage of the way between volFloor and volCeiling at which trial should be conducted
        float jumpSize = 1f / numEarconsToTest;
        for (int i = 0; i < numEarconsToTest; i++) {
            float freq = freqList.get(i);
            int direction = directionList.get(i);
            HashMap<Float, Integer> earconIdMap;
            switch (direction) {
                case Earcon.DIRECTION_DOWN: earconIdMap = earconsDown; break;
                case Earcon.DIRECTION_UP:   earconIdMap = earconsUp; break;
                case Earcon.DIRECTION_NONE: earconIdMap = earconsFlat; break;
                default: throw new RuntimeException("Unknown direction value found : " + direction);
            }

            double volFloor   = this.hearingTestResults.getVolFloorEstimateForEarcon(earconIdMap.get(freq));
            double volCeiling = this.hearingTestResults.getVolCeilingEstimateForEarcon(earconIdMap.get(freq));
            Log.d("configureConfEarcons", "volFloor = " + volFloor + " volCeiling = " + volCeiling);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.confidenceTestEarcons.add(new Earcon(freq, earconIdMap.get(freq), testVol, direction));

            pct += jumpSize;
        }

        // populate list of all trials
        ArrayList<Earcon> allTrials = new ArrayList<>();
        for (int i = 0; i < CONF_NUMBER_OF_TRIALS_PER_EARCON; i++) allTrials.addAll(this.confidenceTestEarcons);
        Collections.shuffle(allTrials);
        this.confidenceTestEarcons = allTrials;
    }

    /**
     * Populate this.analysisResults using estimates generated based on the given subset of the tested freqs
     *
     * @param subset The subset of the tested frequencies to be used to generate estimates
     * @throws IllegalStateException If no confidence results are stored in the model
     * @throws IllegalArgumentException If the given subset is not a subset of the tested frequencies
     */
    public void analyzeConfidenceResults(float[] subset) throws IllegalStateException, IllegalArgumentException {
        if (! this.hasConfResults()) throw new IllegalStateException("No confidence results stored");
        this.analysisResults = new ArrayList<>();
        for (Earcon earcon : this.confidenceTestResults.getTestedEarcons())
            this.analysisResults.add(
                    this.confidenceTestResults.performAnalysis(
                            earcon, this.getProbabilityForEarcon(earcon, subset)));
    }

    /**
     * Populate this.analysisResults using estimates generated based on all frequencies tested in calibration test
     *
     * @throws IllegalStateException If no confidence results are stored in the model
     */
    public void analyzeConfidenceResults() throws IllegalStateException {
        analyzeConfidenceResults(FREQUENCIES);
    }

    /**
     * Find the probability of hearing the given frequency-volume pair given the calibration results
     *
     * @param freq The frequency to be queried
     * @param vol The volume to be queried
     * @return The probability of the given freq-vol pair being heard given the calibration results
     * @throws IllegalStateException If there are no calibration results stored in the model
     */
    public double getProbabilityForEarcon(Earcon earcon) throws IllegalStateException {
        return this.getProbabilityForEarcon(earcon, FREQUENCIES);
    }

    /**
     * Same as other method except calculates probability as though only the frequencies in the given subset were tested
     * @throws IllegalArgumentException if the given subset was not a subset of the tested frequencies
     */
    public double getProbabilityForEarcon(Earcon earcon, float[] subset)
            throws IllegalStateException, IllegalArgumentException {

        if (! this.hasResults()) throw new IllegalStateException("No data stored in model");

        // todo test

//        return this.hearingTestResults.getProbOfCorrectAnswer(
//        earcon.frequency, earcon.direction, earcon.volume, subset);
        return this.hearingTestResults.getProbOfCorrectAnswer(earcon, subset);
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
    @SuppressWarnings("ConstantConditions")
    public void reduceCurrentVolumes() {
        ArrayList<FreqVolPair> newVols = new ArrayList<>();
        for (FreqVolPair fvp : currentVolumes) {
            // only reduce volumes of frequencies still being tested
            if (timesNotHeardPerFreq.get(fvp.freq) >= TIMES_NOT_HEARD_BEFORE_STOP) newVols.add(fvp);
            else newVols.add(new FreqVolPair(fvp.freq, fvp.vol * (1 - HEARING_TEST_REDUCE_RATE)));
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
                            .setSampleRate(OUTPUT_SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
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
                    maxVol,
                    AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * Accessor method to return the confidenceTest ArrayList
     * @return the confidence test array list
     */
    public List<Earcon> getConfidenceTestEarcons(){
        return confidenceTestEarcons;
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
        this.audioPlaying = phase != TEST_PHASE_NULL;
        this.notifySubscribers();
    }

    public void setTestPaused(boolean b) {
        this.testPaused = b;
        this.audioPlaying = !b;
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
        for (FreqVolPair fvp : list) if (fvp.freq == freq) return fvp.vol;
        throw new IllegalArgumentException("Requested frequency not present in list");
    }

    /**
     * Print the contents of hearingTestResults to the console (for testing)
     */
    public void printResultsToConsole() {
        Log.i("printResultsToConsole", String.format("Subject ID: %d\nCalibration Background Noise Type: %s",
                this.subjectId,
                this.hearingTestResults.getBackgroundNoise() == null ? "N/A" :      // show noise type if
                        this.hearingTestResults.getBackgroundNoise().toString()));  // applicable
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