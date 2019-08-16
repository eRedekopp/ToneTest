package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public abstract class ConfidenceTest<T extends Tone> extends HearingTest<T> {

    /**
     * The default number of times to test each tone
     */
    protected static final int DEFAULT_TRIALS_PER_TONE = 3;

    /**
     * The default number of volumes at which to test each frequency
     */
    protected static final int DEFAULT_VOLS_PER_FREQ = 1;

    /**
     * The default frequencies to be used in confidence tests
     */
    protected static final float[] DEFAULT_FREQUENCIES = {220, 440, 880, 1760, 3520};

    /**
     * The default test info the be displayed for confidence tests
     */
    protected static final String DEFAULT_TEST_INFO =
            "In this test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone. " +
            "To hear a sample of the tones that will be played in this test, press the \"Play Samples\" button. Once " +
            "you are familiar with the tones, press the \"Done\" button";

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
    protected abstract void configureTestPairs(int trialsPerTone, int volsPerFreq, float[] frequencies);

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
        this.configureTestPairs(trialsPerTone, volsPerFreq, frequencies);
        this.position = this.testTones.listIterator(0);
    }

    /**
     * Initialize with default values
     */
    public void initialize() {
        this.initialize(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, DEFAULT_FREQUENCIES);
    }

    @Override
    public String getLineEnd(SingleTrialResult result) {
        return result.toString();  // default for conf test
    }

    @Override
    public boolean isComplete() {
        return ! this.position.hasNext();
    }

    @Override
    protected void run() {
        if (this.testTones.isEmpty()) throw new IllegalStateException("Test pairs not yet configured");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);
                    while (!isComplete()) {
                        if (iModel.testPaused() || ! iModel.testing()) return;

                        iModel.resetAnswer();
                        T current = position.next();
                        saveLine();
                        newCurrentTrial(current);
                        Log.i("ConfidenceTest", "Testing tone: " + current.toString());
                        currentTrial.start();
                        playTone(current);
                        if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                            currentTrial = null;    // remove current trial so it isn't added to list
                            return;
                        }
                        currentTrial.setCorrect(wasCorrect());
                        sleepThread(1000, 3000);
                    }

                    controller.confidenceTestComplete();

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    /**
     * @return model.getCalibProbability(tone, n). Override this method to cast T as its particular type and call the
     * appropriate overloaded method
     */
    protected double getCalibProbFromModel(T tone, int n) {
        return model.getCalibProbability(tone, n);
    }

    /**
     * @return model.getRampProbability(tone, n). Override this method to cast T as its particular type and call the
     * appropriate overloaded method
     */
    protected double getRampProbFromModel(T tone, int n) {
        return model.getRampProbability(tone, n);
    }

    /**
     * @return The basic statistics of this confidence test as a string - including values
     * estimated by calibration and ramp models
     */
    @SuppressWarnings("ConstantConditions")
    public String summaryStatsAsString() {
        StringBuilder builder = new StringBuilder();
        HashMap<T, Integer> correctMap = new HashMap<>(), incorrectMap = new HashMap<>();
        ArrayList<T> allTones = new ArrayList<>();

        for (SingleTrialResult<T> t : this.completedTrials) {  // count the number of in/correct responses for each tone
            if (t.wasCorrect()) {
                if (!correctMap.containsKey(t.tone())) correctMap.put(t.tone(), 1);
                else {
                    int newVal = correctMap.get(t.tone()) + 1;
                    correctMap.remove(t.tone());
                    correctMap.put(t.tone(), newVal);
                }
            } else {
                if (!incorrectMap.containsKey(t.tone())) incorrectMap.put(t.tone(), 1);
                else {
                    int newVal = incorrectMap.get(t.tone()) + 1;
                    incorrectMap.remove(t.tone());
                    incorrectMap.put(t.tone(), newVal);
                }
            }
            if (!allTones.contains(t.tone())) allTones.add(t.tone());
        }

        for (int n = 1; n <= model.getNumCalibrationTrials(); n++)         {
            builder.append("########## n = ");
            builder.append(n);
            builder.append(" ##########\n");
            builder.append("Tone confidenceProbability toneProbability rampProbability\n");
            for (T t : allTones) {
                int correct, incorrect;
                try {
                    correct = correctMap.get(t);
                } catch (NullPointerException e) {
                    correct = 0;
                }
                try {
                    incorrect = incorrectMap.get(t);
                } catch (NullPointerException e) {
                    incorrect = 0;
                }
                double  confProb = (double) correct / (double) (correct + incorrect),
                        calibProb,
                        rampProb;
                calibProb = this.getCalibProbFromModel(t, n);
                rampProb = this.getRampProbFromModel(t, n);

                builder.append(String.format("%s %.4f %.4f %.4f%n",
                        t.toString(), confProb, calibProb, rampProb));
            }
        }

        return builder.toString();
    }
}
