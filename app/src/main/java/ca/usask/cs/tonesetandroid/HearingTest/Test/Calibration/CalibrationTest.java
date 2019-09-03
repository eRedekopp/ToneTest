package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * A hearing test that performs the "main" portion of a calibration test
 * @param <T> The type of tones being played in this test
 */
public abstract class CalibrationTest<T extends Tone> extends SingleToneTest<T> {

    /**
     * The default number of volumes at which to test each frequency in a calibration test
     */
    protected static final int DEFAULT_N_VOL_PER_FREQ  = 5;

    /**
     * The default number of trials to perform for each tone in this test
     */
    protected static final int DEFAULT_N_TRIAL_PER_VOL = 5;

    /**
     * The tones that will be played in this calibration test - including duplicates for each
     * time the tone will be played
     */
    protected ArrayList<T> testTones;

    /**
     * An iterator for testTones to keep track of the current position
     */
    protected ListIterator<T> position;

    /**
     * Play a tone of the appropriate type for this calibration test
     */
    protected abstract void playTone(T tone);

    /**
     * Populate testTones with a Tone for each individual trial that will be performed in this test
     */
    protected abstract void configureTestTones(RampTestResults rampResults,
                                            FreqVolPair[] reduceResults,
                                            int nVolsPerFreq,
                                            int nTrialsPerVol);

    /**
     * @param noiseType The type of background noise in this calibration test
     */
    public CalibrationTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.results = new CalibrationTestResults();
    }

    /**
     * Prepare for this test to be run
     *
     * @param rampResults The results of the ramp test run previous to this calibration test
     * @param reduceResults The results of the reduce test run previous to this calibration test
     * @param nVolsPerFreq The number of volumes at which to test each frequency
     * @param nTrialsPerVol The number of times to test each tone
     */
    public void initialize(RampTestResults rampResults,
                           FreqVolPair[] reduceResults,
                           int nVolsPerFreq,
                           int nTrialsPerVol) {
        this.configureTestTones(rampResults, reduceResults, nVolsPerFreq, nTrialsPerVol);
        Collections.shuffle(this.testTones);
        this.position = testTones.listIterator(0);
    }

    /**
     * Initialize with default values
     */
    public void initialize(RampTestResults rampResults, FreqVolPair[] reduceResults) {
        initialize(rampResults, reduceResults, DEFAULT_N_VOL_PER_FREQ, DEFAULT_N_TRIAL_PER_VOL);
    }

    @Override
    protected void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);

                    while (! isComplete()) {
                        if (iModel.testPaused() || ! iModel.testing()) return;  // exit if paused or returned to login

                        sleepThread(1800, 3000); // wait before playing next tone
                        iModel.resetAnswer();
                        T current = position.next();
                        saveLine();  // save previous trial immediately before next to register all clicks
                        newCurrentTrial(current);
                        currentTrial.setStartTime();
                        playTone(current);
                        if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                            currentTrial = null;    // remove current trial so it isn't added to list
                            return;
                        }
                        currentTrial.setCorrect(iModel.answered());
                        ((CalibrationTestResults) results).addResult(current, currentTrial.wasCorrect());
                    }

                    // test complete: finalize results
                    controller.calibrationTestComplete();

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    public CalibrationTestResults getResults() {
        return (CalibrationTestResults) this.results;
    }

    @Override
    public boolean isComplete() {
        return ! this.position.hasNext();
    }

    @Override
    protected String getLineEnd(SingleTrialResult result) {

        return String.format("freq(Hz) %.1f, vol %.4f, %s, %d clicks: %s",
                this.currentTrial.tone().freq(),
                this.currentTrial.tone().vol(),
                this.currentTrial.wasCorrect() ? "Heard" : "NotHeard",
                this.currentTrial.nClicks(),
                this.currentTrial.getClicksAsString());
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
