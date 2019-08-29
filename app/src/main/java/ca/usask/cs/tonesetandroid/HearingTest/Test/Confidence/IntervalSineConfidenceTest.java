package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.UtilFunctions;

public class IntervalSineConfidenceTest extends ConfidenceTest<Interval> {

    private static final float INTERVAL_FREQ_RATIO = 1.25f;  // freq ratio 5:4 = major third

    private static final int INTERVAL_DURATION_MS = 1500;

    public static final String INTERVAL_TEST_INFO =
            "In this test, pairs of tones of various frequencies and volumes will be played one after the other at " +
            "random times. Please press the \"Up\" button if the interval moved upward (ie. the second tone was " +
            "higher than the first), press the \"Down\" button if the interval moved downward (ie. the second tone " +
            "was lower than the first), or do nothing if you couldn't tell.";

    public IntervalSineConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(calibResults, noiseType);
        this.testInfo = INTERVAL_TEST_INFO;
        this.GRACE_PERIOD_MS = 1200;  // user gets 1.2 seconds after tone ends to register clicks
        this.MIN_WAIT_TIME_MS = 1500;
        this.testTypeName = "sine-interval-conf";
    }

    @Override
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        if (frequencies.length == 0) return;

        this.testTones = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (int i = 0; i < volsPerFreq; i++) for (float freq : frequencies) confFreqs.add(freq);

        for (boolean upward : new boolean[]{true, false}) {  // once upward, once downward: all frequencies tested twice

            Collections.shuffle(confFreqs);

            testTones.add(createNewInterval(        // add an interval that will likely be heard every time
                        confFreqs.get(0),
                        this.getCeilingEstimateAvg(confFreqs.get(0), upward),
                        upward
            ));

            if (frequencies.length > 1)             // add and interval that likely won't be heard at all
                testTones.add(createNewInterval(
                        confFreqs.get(1),
                        this.getFloorEstimateAvg(confFreqs.get(0), upward),
                        upward
                ));

            if (frequencies.length > 2)             // add an interval that will very likely be heard every time
                testTones.add(createNewInterval(
                        confFreqs.get(2),
                        this.getCeilingEstimateAvg(confFreqs.get(2), upward) * 1.25,
                        upward
                ));

            if (frequencies.length > 3)             // add an interval that will extremely likely be heard every time
                testTones.add(createNewInterval(
                        confFreqs.get(3),
                        this.getCeilingEstimateAvg(confFreqs.get(2), upward) * 1.5,
                        upward
                ));

            int hardCodedCases = 4;  // how many test cases are hard-coded like the ones above?

            // For every other frequency in the list, add a test case where it is somewhere between 40% and 100% of the
            // way between estimates for "completely inaudible" and "completely audible" volumes
            float pct = 0.4f;
            float jumpSize = (1 - pct) / (frequencies.length - hardCodedCases);
            for (int i = hardCodedCases; i < frequencies.length; i++, pct += jumpSize) {
                float freq = confFreqs.get(i);
                double volFloor = this.getFloorEstimateAvg(freq, upward);
                double volCeiling = this.getCeilingEstimateAvg(freq, upward);
                double testVol = volFloor + pct * (volCeiling - volFloor);
                this.testTones.add(createNewInterval(freq, testVol, upward));
            }
        }

        // prepare list of all trials
        ArrayList<Interval> allTrials = new ArrayList<>();
        for (int i = 0; i < trialsPerTone; i++) allTrials.addAll(this.testTones);
        Collections.shuffle(allTrials);
        this.testTones = allTrials;

        if (this.testTones.size() != trialsPerTone * frequencies.length * volsPerFreq * 2)  // sanity check
            Log.w("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length * volsPerFreq * 2 +
                    " test pairs but generated " + this.testTones.size());
    }

    @Override
    public Runnable sampleTones() {
        return new Runnable() {
            @Override
            public void run() {
                if (! iModel.sampleThreadActive())
                    try {
                        iModel.setSampleThreadActive(true);
                        for (float freq : DEFAULT_FREQUENCIES) {
                            if (!iModel.testPaused()) return; // stop if user un-pauses
                            playTone(new Interval(freq, freq * INTERVAL_FREQ_RATIO, 70));
                            sleepThread(500, 500);  // sleep 1/2 second
                            if (!iModel.testPaused()) return; // stop if user un-pauses
                            playTone(new Interval(freq, freq / INTERVAL_FREQ_RATIO, 70));
                            sleepThread(500, 500);  // sleep 1/2 second
                        }
                    } finally {
                        iModel.setSampleThreadActive(false);
                    }
            }
        };
    }

    @Override
    protected void playTone(Interval tone) {
        // Play 2 sines in succession, total time = INTERVAL_DURATION_MS
        playSine(tone.freq(), tone.vol(), INTERVAL_DURATION_MS / 2);
        playSine(tone.freq2(), tone.vol(), INTERVAL_DURATION_MS / 2);
    }

    @Override
    protected boolean wasCorrect() {
        int expected = ((Interval) this.currentTrial.tone()).isUpward() ? ANSWER_UP : ANSWER_DOWN;
        return iModel.getAnswer() == expected;
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_UP, ANSWER_DOWN};
    }

    @Override
    protected double getModelRampProbability(Interval t, boolean withFloorResults) {
        return model.getRampProbability(t, withFloorResults);
    }

    @Override
    protected double getModelCalibProbability(Interval t, int n) {
        return model.getCalibProbability(t, n);
    }

    /**
     * @return a new interval starting on the given frequency and going the requested direction
     */
    public Interval createNewInterval(float freq1, double vol, boolean isUpward) {
        if (isUpward) return new Interval(freq1, freq1 * INTERVAL_FREQ_RATIO, vol);
        else return new Interval(freq1, freq1 / INTERVAL_FREQ_RATIO, vol);
    }

    /**
     * @return The mean of CalibrationTestResults.getVolFloorEstimateForFreq for each frequency in freqs
     */
    protected double getFloorEstimateAvg(float freq1, boolean isUpward) {
        float[] freqs = new float[]{freq1, isUpward ? freq1 * INTERVAL_FREQ_RATIO : freq1 / INTERVAL_FREQ_RATIO};
        double[] floorEstimates = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) floorEstimates[i] = calibResults.getVolFloorEstimateForFreq(freqs[i]);
        return UtilFunctions.mean(floorEstimates);
    }

    /**
     * @return The mean of CalibrationTestResults.getVolCeilingestimateForFreq for each frequency in freqs
     */
    protected double getCeilingEstimateAvg(float freq1, boolean isUpward) {
        float[] freqs = new float[]{freq1, isUpward ? freq1 * INTERVAL_FREQ_RATIO : freq1 / INTERVAL_FREQ_RATIO};
        double[] ceilingEstimates = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++)
            ceilingEstimates[i] = calibResults.getVolCeilingEstimateForFreq(freqs[i]);
        return UtilFunctions.mean(ceilingEstimates);
    }
}
