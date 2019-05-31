package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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

    // todo force volume to max while app open

    public enum TestType {PureTone, Ramp}       // enum for types of hearing tests

    private ArrayList<ModelListener> subscribers;

    // Vars for audio
    AudioTrack line;
    public static final int SAMPLE_RATE = 44100; // sample at 44.1 kHz always
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, 8000}; // From British Society of Audiology
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;

    // Vars for storing results
    ArrayList<FreqVolPair> hearingTestResults;  // The "just audible" volume for each frequency tested in the most 
                                                // recent pure/ramp test (or loaded from file)
    private int subjectId = -1;     // -1 indicates not set
    private TestType lastTestType;

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
        this.setUpLine();
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
     * Configure the audio in preparation for a PureTone test
     */
    public void configureAudio() {
        this.duration_ms = 1500;
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
    private void setUpLine() {
        if (line == null) {
            int minBufferSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat format =
                    new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                            .setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            line = new AudioTrack(audioAttributes, format, minBufferSize,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            line.setVolume(1.5f);
        }
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

    public void setLastTestType(TestType type) {
        this.lastTestType = type;
    }

    public TestType getLastTestType() {
        return this.lastTestType;
    }

    public ArrayList<FreqVolPair> getHearingTestResults() {
        return this.hearingTestResults;
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
