package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;

public class SingleSineConfidenceTest extends ConfidenceTest<FreqVolPair> {

    private static final int TONE_DURATION_MS = 1500;

    public SingleSineConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(calibResults, noiseType);
        this.testTypeName = "sine-single-tone-conf";
    }

    @Override
    public Runnable sampleTones() {
        return new Runnable() {
            @Override
            public void run() {
                if (! iModel.sampleThreadActive()) {
                    try {
                        iModel.setSampleThreadActive(true);
                        for (float freq : DEFAULT_FREQUENCIES) {
                            if (!iModel.testPaused()) return;  // stop if user un-pauses during tones
                            playSine(freq, 70, TONE_DURATION_MS);
                            sleepThread(500, 500);
                        }
                    } finally {
                        iModel.setSampleThreadActive(false);
                    }
                }
            }
        };
    }

    @Override
    protected boolean wasCorrect() {
        return iModel.answered();
    }

    @Override
    protected void configureTestPairs(int trialsPerTone, int volsPerFreq, float[] frequencies) {

        if (frequencies.length == 0) return;

        // create a list containing one copy of freq for each volume at which freq should be tested
        this.testTones = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (int i = 0; i < volsPerFreq; i++) for (float freq : frequencies) confFreqs.add(freq);
        Collections.shuffle(confFreqs);

        testTones.add(new FreqVolPair(      // add a test that will likely be heard every time
                confFreqs.get(0),
                this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(0))));

        if (frequencies.length > 1)         // add a test that will likely not be heard at all
            testTones.add(new FreqVolPair(
                    confFreqs.get(1),
                    this.calibResults.getVolFloorEstimateForFreq(confFreqs.get(1))));

        if (frequencies.length > 2)         // add a test that very likely will be heard every time
            testTones.add(new FreqVolPair(
                    confFreqs.get(2),
                    this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(2)) * 1.25));

        if (frequencies.length > 3)         // add a test that extremely likely will be heard every time
            testTones.add(new FreqVolPair(
                    confFreqs.get(3),
                    this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(3)) * 1.5));

        int hardCodedCases = 4; // how many test cases are hard-coded like the ones above?

        // For every other frequency in the list, add a test case where it is somewhere between 40% and 100% of the
        // way between estimates for "completely inaudible" and "completely audible" volumes
        float pct = 0.4f;
        float jumpSize = (1 - pct) / (frequencies.length - hardCodedCases);
        for (int i = hardCodedCases; i < frequencies.length; i++, pct += jumpSize) {
            float freq = confFreqs.get(i);
            double volFloor = this.calibResults.getVolFloorEstimateForFreq(freq);
            double volCeiling = this.calibResults.getVolCeilingEstimateForFreq(freq);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.testTones.add(new FreqVolPair(freq, testVol));
        }

        // prepare list of all trials
        ArrayList<FreqVolPair> allTrials = new ArrayList<>();
        for (int i = 0; i < trialsPerTone; i++) {
            allTrials.addAll(this.testTones);
        }
        Collections.shuffle(allTrials);
        this.testTones = allTrials;

        if (this.testTones.size() != trialsPerTone * frequencies.length * volsPerFreq)
            Log.e("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length +
                                        " test pairs but generated " + this.testTones.size());
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        return String.format("%s, %s, %d clicks: %s",
                trial.tone().toString(), trial.wasCorrect() ? "Heard" : "NotHeard", trial.nClicks(),
                Arrays.toString(trial.clickTimes()));
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, TONE_DURATION_MS);
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
