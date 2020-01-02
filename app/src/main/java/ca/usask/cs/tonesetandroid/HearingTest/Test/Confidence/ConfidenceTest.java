package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.ConfidenceTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SingleToneTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Parent class for all "Confidence Tests", in which we find "true" values of P(heard) for some tones by testing them
 * many times, and compare the results to the predictions made by the stored CalibrationTestResults and RampTestResults
 *
 * @param <T> The type of tone being played in this ConfidenceTest
 */
public abstract class ConfidenceTest<T extends Tone> extends SingleToneTest<T> {

    /**
     * The default number of times to test each individual tone
     */
    protected static final int DEFAULT_TRIALS_PER_TONE = 20;

    /**
     * The default number of volumes at which to test each pitch
     */
    protected static final int DEFAULT_VOLS_PER_FREQ = 1;

    /**
     * How long will responses be accepted after the tone finishes playing?
     */
    protected int GRACE_PERIOD_MS = 0;  // no grace period by default

    /**
     * Minimum time between end of one trial and start of another. MIN_WAIT_TIME_MS >= GRACE_PERIOD_MS
     */
    protected int MIN_WAIT_TIME_MS = 1000;

    /**
     * Maximum time between end of one trial and setStartTime of another
     */
    protected int MAX_WAIT_TIME_MS = 3000;

    /**
     * The results of this confidence test
     */
    protected ConfidenceTestResults results;

    /**
     * All tones to be tested, with duplicates for each individual time it is to be played
     */
    protected ArrayList<T> testTones;

    /**
     * An iterator for testTones to keep track of the current trial
     */
    protected ListIterator<T> position;

    /**
     * The default frequencies to be used in confidence tests
     */
    protected static final float[] DEFAULT_FREQUENCIES = DEFAULT_CONFIDENCE_FREQUENCIES;

    protected static final String DEFAULT_TEST_INFO =
            "In this test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone. ";

    /**
     * Fill testTones with appropriate tones for this test
     */
    protected abstract void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies);

    /**
     * @return A runnable which plays a sample of all testable tones in this confidence test via
     *         the Model
     */
    public abstract Runnable sampleTones();

    /**
     * Play a tone of the appropriate type for this confidence test
     */
    protected abstract void playTone(T tone);

    /**
     * @return True if the user answered correctly in the most recent trial, else false
     */
    protected abstract boolean wasCorrect();

    /**
     * @param noiseType The background noise played during this confidence test
     */
    public ConfidenceTest(BackgroundNoiseType noiseType) {
        // TODO make number of tones configurable
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;
        this.testTones = new ArrayList<>();
        this.position = testTones.listIterator();
        this.results = new ConfidenceTestResults(noiseType, this.getTestTypeName());
    }

    /**
     * Prepare for this ConfidenceTest to be started
     *
     * @param trialsPerTone The number of times to test each tone in this test
     * @param volsPerFreq The number of volumes at which to test each frequency
     * @param frequencies The frequencies of the tones to be tested
     */
    public void initialize(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        this.configureTestTones(trialsPerTone, volsPerFreq, frequencies);
        this.position = this.testTones.listIterator(0);
    }

    /**
     * Initialize with default values
     */
    public void initialize() {
        this.initialize(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, DEFAULT_FREQUENCIES);
    }

    @Override
    public boolean isComplete() {
        return ! this.position.hasNext();
    }

    @Override
    protected void run() {
        if (this.testTones.isEmpty()) throw new IllegalStateException("Test pairs not yet configured");
        else if (MIN_WAIT_TIME_MS < GRACE_PERIOD_MS) throw new RuntimeException("Grace period = " + GRACE_PERIOD_MS +
                " is greater than the minimum wait time = " + MIN_WAIT_TIME_MS);

        this.setStartTime();  // set the start time of this test (or do nothing if this has already been done)

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);
                    sleepThread(3000, 5000); // wait 3-5 seconds before playing tone

                    while (!isComplete()) {
                        if (iModel.testPaused() || ! iModel.testing()) return; // quit if user pauses or exits

                        iModel.resetAnswer(); 
                        T currentTone = position.next();
                        saveLine(); // save previous trial immediately before starting next one to register all clicks
                        newCurrentTrial(currentTone);
                        currentTrial.setStartTime();
                        playTone(currentTone);
                        if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                            currentTrial = null;    // remove current trial so it isn't added to list
                            return;
                        }
                        sleepThread(GRACE_PERIOD_MS, GRACE_PERIOD_MS);  // give user grace period after tone finishes
                        currentTrial.setCorrect(wasCorrect());
                        results.addResult(currentTone, currentTrial.wasCorrect());
                        // finish sleeping
                        sleepThread(MIN_WAIT_TIME_MS - GRACE_PERIOD_MS, MAX_WAIT_TIME_MS - GRACE_PERIOD_MS); 
                    }

                    // Test complete - perform any remaining steps
                    controller.confidenceTestComplete();

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        return String.format("%s, %s, %d clicks: %s",
                trial.tone().toString(),
                trial.wasCorrect() ? "Correct" : "Incorrect",
                trial.nClicks(),
                trial.getClicksAsString());
    }

    /**
     * @return An array containing each tone that has been tested at least once 
     */
    protected Tone[] allTones() {
        ArrayList<Tone> allTones = new ArrayList<>();
        for (SingleTrialResult t : this.completedTrials) if (! allTones.contains(t.tone())) allTones.add(t.tone());
        Tone[] toneArr = new Tone[allTones.size()];
        allTones.toArray(toneArr);
        return toneArr;
    }

    public ConfidenceTestResults getConfResults() {
        return this.results;
    }
}
