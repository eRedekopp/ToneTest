package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;


public abstract class CalibrationTest<T extends Tone> extends HearingTest<T> {

    protected static final float[] STANDARD_FREQUENCIES = {200, 500, 1000, 2000, 4000};

    protected static final int DEFAULT_N_VOL_PER_FREQ = 5;
    protected static final int DEFAULT_N_TRIAL_PER_VOL = 5;

    // The tones that will be tested in this calibration test
    protected ArrayList<T> testTones;

    // To keep track of which tone will be played next
    private ListIterator<T> position;

    // Container for the results of this calibration test
    protected CalibrationTestResults results;

    /**
     * Play a tone of the appropriate type for the test
     */
    protected abstract void playTone(T tone);

    /**
     * Configure the tones that will be tested in this calibration test
     */
    public abstract void initialize(RampTest.RampTestResults rampResults,
                                    ReduceTest.ReduceTestResults reduceResults,
                                    int nVolsPerFreq,
                                    int nTrialsPerVol);

    /**
     * Configure the tones that will be used in this calibration test with the default values
     */
    public void initialize(RampTest.RampTestResults rampResults, ReduceTest.ReduceTestResults reduceResults) {
        initialize(rampResults, reduceResults, DEFAULT_N_VOL_PER_FREQ, DEFAULT_N_TRIAL_PER_VOL);
    }

    public CalibrationTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        position = this.testTones.listIterator(0);
    }

    @Override
    protected void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);

                    while (! isComplete()) {
                        if (iModel.testPaused()) return;
                        iModel.resetAnswer();
                        T current = position.next();
                        newCurrentTrial(current);
                        Log.i("CalibrationTest", "Testing tone: " + current.toString());
                        currentTrial.start();
                        playTone(current);
                        if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                            currentTrial = null;    // remove current trial so it isn't added to list
                            return;
                        }
                        currentTrial.setCorrect(iModel.answered());
                        Log.i("CalibrationTest", currentTrial.wasCorrect() ? "Tone Heard" : "Tone Not Heard");
                        saveLine();
//                        results.addResult(); // todo add to calibrationTestResults
                        sleepThread(1000, 3000);
                    }

                    // test complete: finalize results


                } finally {
                    iModel.setTestThreadActive(false);
                    model.audioTrackCleanup();
                }
            }
        }).start();
    }

    @Override
    protected boolean isComplete() {
        return this.position.hasNext();
    }

    @Override
    protected String getLineEnd(SingleTrialResult result) {
        return String.format("%s, %s, %d clicks: %s",
                this.currentTrial.tone.toString(),
                this.currentTrial.wasCorrect() ? "Heard" : "NotHeard",
                this.currentTrial.nClicks(),
                Arrays.toString(this.currentTrial.clickTimes()));
    }
}
