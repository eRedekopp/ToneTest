package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public abstract class ConfidenceTest<T extends Tone> extends HearingTest<T> {

    /**
     * The default number of times to test each tone
     */
    protected static final int DEFAULT_TRIALS_PER_TONE = 20;

    /**
     * The default number of volumes at which to test each frequency
     */
    protected static final int DEFAULT_VOLS_PER_FREQ = 1;

    /**
     * How long will responses be accepted after the tone finishes playing?
     */
    protected int GRACE_PERIOD_MS = 0;  // no grace period by default

    /**
     * Minimum time between end of one trial and setStartTime of another. MIN_WAIT_TIME_MS >= GRACE_PERIOD_MS
     */
    protected int MIN_WAIT_TIME_MS = 1000;

    /**
     * Maximum time between end of one trial and setStartTime of another
     */
    protected int MAX_WAIT_TIME_MS = 3000;

    /**
     * The default frequencies to be used in confidence tests
     */
    protected static final float[] DEFAULT_FREQUENCIES = {220, 440, 880, 1760, 3520};

    /**
     * The default test info the be displayed for confidence tests
     */
    protected static final String DEFAULT_TEST_INFO =
            "In this test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone. ";

    /**
     * The results of the calibration test associated with this confidence test
     */
    protected CalibrationTestResults calibResults;

    /**
     * All tones to be tested, with duplicates for each time a tone is to be tested and shuffled
     * if desired
     */
    protected ArrayList<T> testTones;

    /**
     * An iterator for testTones to keep track of the current trial
     */
    protected ListIterator<T> position;

    /**
     * Fill testTones with appropriate tones for this test
     */
    protected abstract void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies);

    /**
     * @return A runnable which plays a sample of all testable tones in this confidence test via
     * the Model
     */
    public abstract Runnable sampleTones();

    /**
     * Play a tone for this confidence test
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
     * Prepare for this test to be performed
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
                    sleepThread(3000, 5000); // wait 3-5 seconds before playing first tone
                    while (!isComplete()) {
                        if (iModel.testPaused() || ! iModel.testing()) return;

                        iModel.resetAnswer();
                        T current = position.next();
                        saveLine();
                        newCurrentTrial(current);
                        currentTrial.setStartTime();
                        playTone(current);
                        if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                            currentTrial = null;    // remove current trial so it isn't added to list
                            return;
                        }
                        sleepThread(GRACE_PERIOD_MS, GRACE_PERIOD_MS);
                        currentTrial.setCorrect(wasCorrect());
                        sleepThread(MIN_WAIT_TIME_MS - GRACE_PERIOD_MS, MAX_WAIT_TIME_MS - GRACE_PERIOD_MS);
                    }

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
     * Call model.getCalibProbabliity for the given tone. Overload this method to get the appropriate overloaded
     * method from the model
     */
    protected double getModelCalibProbability(T t, int n) {
        return model.getCalibProbability(t, n);
    }

    protected Tone[] allTones() {
        ArrayList<Tone> allTones = new ArrayList<>();
        for (SingleTrialResult t : this.completedTrials) if (! allTones.contains(t.tone())) allTones.add(t.tone());
        Tone[] toneArr = new Tone[allTones.size()];
        allTones.toArray(toneArr);
        return toneArr;
    }

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
     * @return The basic statistics of this confidence test as a string - including values
     * estimated by calibration and ramp models
     */
    @SuppressWarnings({"unchecked"})
    public String summaryStatsAsString() {
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

    protected static class SingleToneResult {
        private int correct;
        private int incorrect;
        public final Tone tone;

        public SingleToneResult(Tone tone) {
            this.correct = 0;
            this.incorrect = 0;
            this.tone = tone;
        }

        public void addCorrect() {
            this.correct++;
        }

        public void addIncorrect() {
            this.incorrect++;
        }

        public int getCorrect() {
            return this.correct;
        }

        public int getIncorrect() {
            return this.incorrect;
        }
    }

}
