package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SingleToneTest;
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
     * The results of the RampTest that was performed before this CalibrationTest
     */
    protected RampTestResults rampResults;

    /**
     * The results of the ReduceTest that was performed before this CalibrationTest
     */
    protected FreqVolPair[] reduceResults;

    /**
     * Populate testTones with a Tone for each individual trial that will be performed in this test
     *
     * @param rampResults The results of the ramp test run previous to this calibration test
     * @param reduceResults The results of the reduce test run previous to this calibration test
     * @param nVolsPerFreq The number of volumes at which to test each frequency
     * @param nTrialsPerVol The number of times to test each tone
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
        this.results = new CalibrationTestResults(this.getBackgroundNoiseType(), this.getTestTypeName());
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

        if (this.reduceResults == null || this.rampResults == null)
            throw new IllegalStateException("reduce or ramp test results not yet configured");
        this.configureTestTones(rampResults, reduceResults, nVolsPerFreq, nTrialsPerVol);
        Collections.shuffle(this.testTones);
        this.position = testTones.listIterator(0);
    }

    /**
     * Initialize with default values
     */
    public void initialize() {
        initialize(this.rampResults, this.reduceResults, DEFAULT_N_VOL_PER_FREQ, DEFAULT_N_TRIAL_PER_VOL);
    }

    @Override
    protected void run() {
        this.setStartTime();  // set the start time of this test (or do nothing if this has already been done)
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
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_HEARD};
    }

    public void setRampResults(RampTestResults rampResults) {
        this.rampResults = rampResults;
    }

    public void setReduceResults(FreqVolPair[] reduceResults) {
        this.reduceResults = reduceResults;
    }
}
