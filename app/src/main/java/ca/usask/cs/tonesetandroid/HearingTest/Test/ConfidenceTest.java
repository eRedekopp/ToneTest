package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public abstract class ConfidenceTest<T extends Tone> extends HearingTest<T> {

    private static final int DEFAULT_TRIALS_PER_TONE = 20;
    protected static final float[] DEFAULT_FREQUENCIES = {220, 440, 880, 1760, 3520};
    private static final String DEFAULT_TEST_INFO =
            "In this test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone";

    protected CalibrationTestResults calibResults;

    // for keeping track of trials
    protected ArrayList<T> testPairs;
    protected ListIterator<T> position;

    /**
     * Configure testPairs to contain all the tones to be tested in this test
     */
    protected abstract void configureTestPairs(int trialsPerTone);

    /**
     * Play a sample of all testable tones of the given direction
     *
     * @param direction An integer indicating the direction of samples to be played (ConfidenceTest.DIRECTION_*)
     */
    public abstract void playSamples(int direction);

    /**
     * Play a tone of the appropriate type for the subclass
     */
    protected abstract void playTone(T tone);

    /**
     * @return True if the user answered the most recent test correctly, else false
     */
    protected abstract boolean wasCorrect();

    @Override
    public String getLineEnd(SingleTrialResult result) {
        return result.toString();  // default for conf test
    }

    @Override
    protected boolean isComplete() {
        return this.position.hasNext();
    }

    @Override
    protected void run() {
        if (this.testPairs.isEmpty()) throw new IllegalStateException("Test pairs not yet configured");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);
                    while (! isComplete() && ! iModel.testPaused()) {
                        iModel.resetAnswer();
                        T current = position.next();
                        newCurrentTrial(current);
                        Log.i("ConfidenceTest", "Testing tone: " + current.toString());
                        currentTrial.start();
                        playTone(current);
                        currentTrial.setCorrect(wasCorrect());
                        Log.i("ConfidenceTest", currentTrial.wasCorrect() ? "Tone Heard" : "Tone Not Heard");
                        saveLine();
                        sleepThread(1000, 3000);
                    }
                } finally { model.setTestThreadActive(false); }
            }
        }).start();
    }

    public ConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;

        this.calibResults = calibResults;
        this.configureTestPairs(DEFAULT_TRIALS_PER_TONE);
        this.position = this.testPairs.listIterator(0);
    }
}
