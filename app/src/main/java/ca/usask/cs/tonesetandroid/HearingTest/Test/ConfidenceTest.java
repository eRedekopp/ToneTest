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

    protected static final int DEFAULT_TRIALS_PER_TONE = 20;
    protected static final int DEFAULT_VOLS_PER_FREQ = 1;
    protected static final float[] DEFAULT_FREQUENCIES = {220, 440, 880, 1760, 3520};
    protected static final String DEFAULT_TEST_INFO =
            "In this test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone. " +
            "To hear a sample of the tones that will be played in this test, press the \"Play Samples\" button. Once " +
            "you are familiar with the tones, press the \"Done\" button";

    protected CalibrationTestResults calibResults;

    // for keeping track of trials
    protected ArrayList<T> testPairs;
    protected ListIterator<T> position;

    /**
     * Configure testPairs to contain all the tones to be tested in this test
     */
    protected abstract void configureTestPairs(int trialsPerTone, int volsPerFreq, float[] frequencies);

    /**
     * Play a sample of all testable tones of the given direction
     *
     * @param direction An integer indicating the direction of samples to be played (ConfidenceTest.DIRECTION_*)
     * @return A runnable which plays a sample of all tones in this confidence test via the Model
     */
    public abstract Runnable sampleTones();

    /**
     * Play a tone of the appropriate type for the subclass
     */
    protected abstract void playTone(T tone);

    /**
     * @return True if the user answered the most recent test correctly, else false
     */
    protected abstract boolean wasCorrect();

    public ConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;

        this.calibResults = calibResults;
        this.configureTestPairs(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, DEFAULT_FREQUENCIES);
        this.position = this.testPairs.listIterator(0);
    }

    @Override
    public String getLineEnd(SingleTrialResult result) {
        return result.toString();  // default for conf test
    }

    @Override
    public boolean isComplete() {
        return !this.position.hasNext();
    }

    @Override
    protected void run() {
        if (this.testPairs.isEmpty()) throw new IllegalStateException("Test pairs not yet configured");

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

    @SuppressWarnings("ConstantConditions")
    public String summaryStatsAsString() {
        StringBuilder builder = new StringBuilder();
        HashMap<Tone, Integer> correctMap = new HashMap<>(), incorrectMap = new HashMap<>();
        ArrayList<Tone> allTones = new ArrayList<>();

        for (SingleTrialResult t : this.completedTrials) {  // count the number of in/correct responses for each tone
            if (t.wasCorrect()) {
                if (!correctMap.containsKey(t.tone)) correctMap.put(t.tone, 1);
                else {
                    int newVal = correctMap.get(t.tone) + 1;
                    correctMap.remove(t.tone);
                    correctMap.put(t.tone, newVal);
                }
            } else {
                if (!incorrectMap.containsKey(t.tone)) incorrectMap.put(t.tone, 1);
                else {
                    int newVal = incorrectMap.get(t.tone) + 1;
                    incorrectMap.remove(t.tone);
                    incorrectMap.put(t.tone, newVal);
                }
            }
            if (!allTones.contains(t.tone)) allTones.add(t.tone);
        }

        for (int n = 1; n <= model.getNumCalibrationTrials(); n++)         {
            builder.append("########## n = ");
            builder.append(n);
            builder.append(" ##########\n");
            builder.append("Tone confidenceProbability toneProbability rampProbability\n");
            for (Tone t : allTones) {
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
                calibProb = model.getCalibProbability(t, n);
                rampProb = model.getRampProbability(t);

                builder.append(String.format("%s %.4f %.4f %.4f%n",
                        t.toString(), confProb, calibProb, rampProb));
            }
        }

        return builder.toString();
    }
}
