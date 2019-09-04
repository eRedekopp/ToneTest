package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
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
     * The results of the CalibrationTest to be used to generate volumes for trials in this test
     */
    protected CalibrationTestResults calibResults;

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
     * @param calibResults The results of the calibration test associated with this confidence test
     * @param noiseType The background noise played during this confidence test
     */
    public ConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;
        this.calibResults = calibResults;
        this.testTones = new ArrayList<>();
        this.position = testTones.listIterator();
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
     * Call model.getRampProbability for the given tone. Overload this method to get the appropriate overloaded
     * method from the Model. Gets the results from the ramp test that stores volume floor results if
     * withFloorResults == true, else gets them from the regular RampTestResults
     */
    protected double getModelRampProbability(T t, boolean withFloorResults) {
        return model.getRampProbability(t, withFloorResults);
    }

    /**
     * Call model.getCalibProbabliity for the given tone. Overload this method to get the appropriate 
     * method from the model
     */
    protected double getModelCalibProbability(T t, int n) {
        return model.getCalibProbability(t, n);
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

    /**
     * @return An array of SingleToneResults, with one SingleToneResult for each tone tested so far
     */
    @SuppressWarnings("ConstantConditions")
    protected SingleToneResult[] getConfResults() {
        HashMap<Float, SingleToneResult> allResults = new HashMap<>();

        for (Tone t : this.allTones()) allResults.put(t.freq(), new SingleToneResult(t));

        for (SingleTrialResult t : this.completedTrials)  // count the number of in/correct responses for each tone
            if (t.wasCorrect()) allResults.get(t.tone().freq()).addCorrect();
            else allResults.get(t.tone().freq()).addIncorrect();

        SingleToneResult[] toneArr = new SingleToneResult[allResults.size()];
        allResults.values().toArray(toneArr);
        return toneArr;
    }

    /**
     * Return a String containing the results of this ConfidenceTest compared to the estimates from the 
     * CalibrationTestResults and RampTestResults stored in the model. 
     * 
     * Prints out a batch for each 1 < n < calibrationTest.nTrialsPerTone, with each batch containing the confidence
     * results and all estimates for each Tone tested in this ConfidenceTest. CalibrationTest estimates are made 
     * based on the first n trial results in the stored CalibrationTest
     *
     * @throws IllegalStateException if this test is not yet complete
     */
    @SuppressWarnings({"unchecked"})
    public String summaryStatsAsString() throws IllegalStateException {
        if (! this.isComplete()) throw new IllegalStateException("Test not yet complete");

        StringBuilder builder = new StringBuilder();
        RampTestResults regularResults = model.getRampResults().getRegularRampResults();
        SingleToneResult[] confResults = this.getConfResults();

        for (int n = 1; n <= model.getNumCalibrationTrials(); n++)         {
            builder.append("########## n = ");
            builder.append(n);
            builder.append(" ##########\n");
            builder.append( "Tone confidenceProbability toneProbability rampProbabilityLinearWithReduceData " +
                            "rampProbabilityLinearWithoutReduceData rampProbabilityLogWithReduceData " +
                            "rampProbabilityLogWithoutReduceData\n");
            for (SingleToneResult result : confResults) {
                double  confProb = (double) result.getCorrect() / (result.getCorrect() + result.getIncorrect()),
                        calibProb,
                        rampProbLinFloor,
                        rampProbLinReg,
                        rampProbLogFloor,
                        rampProbLogReg;

                // get all 4 ramp estimates
                Tone t = result.tone;
                regularResults.setModelEquation(0);
                model.getRampResults().setModelEquation(0);
                rampProbLinFloor = this.getModelRampProbability((T) t, true);
                rampProbLinReg = this.getModelRampProbability((T) t, false);
                model.getRampResults().setModelEquation(1);
                rampProbLogFloor = this.getModelRampProbability((T) t, true);
                rampProbLogReg = this.getModelRampProbability((T) t, false);

                // get calib estimate
                calibProb = this.getModelCalibProbability((T) t, n);

                builder.append(String.format("%s %.4f %.4f %.4f %.4f %.4f %.4f%n",
                        t.toString(), confProb, calibProb, rampProbLinFloor, rampProbLinReg, rampProbLogFloor,
                        rampProbLogReg));
            }
        }

        return builder.toString();
    }

    /**
     * An object containing the results from a single tone tested in this ConfidenceTest
     */
    protected static class SingleToneResult {

        /**
         * The number of times that the user answered this tone correctly
         */
        private int correct;

        /**
         * The number of times that the user answered this tone incorrectly
         */
        private int incorrect;

        /**
         * The tone whose results are being stored
         */
        public final Tone tone;

        public SingleToneResult(Tone tone) {
            this.correct = 0;
            this.incorrect = 0;
            this.tone = tone;
        }

        /**
         * Indicate that the user answered this tone correctly 
         */
        public void addCorrect() {
            this.correct++;
        }

        /**
         * Indicate that the user answered this tone incorrectly 
         */
        public void addIncorrect() {
            this.incorrect++;
        }

        /**
         * @return The number of times that the user answered this tone correctly
         */
        public int getCorrect() {
            return this.correct;
        }

        /**
         * @return The number of times that the user answered this tone incorrectly
         */
        public int getIncorrect() {
            return this.incorrect;
        }
    }
}
