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
                            sleepThread(800, 800);
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
    protected void configureTestPairs(int trialsPerTone) {

        // todo add louder trials

        this.testPairs = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (float freq : DEFAULT_FREQUENCIES) confFreqs.add(freq);

        // randomize the order of test frequencies
        Collections.shuffle(confFreqs);

        // for each frequency, add a new fvp to confidenceTestPairs with the frequency and a volume some percentage
        // of the way between completely inaudible and perfectly audible
        float pct = 0;  // the percentage of the way between the lowest and highest tested vol that this test will be
        float jumpSize = 1.0f / DEFAULT_FREQUENCIES.length;
        for (Float freq : confFreqs) {
            double volFloor = this.calibResults.getVolFloorEstimateForFreq(freq);
            double volCeiling = this.calibResults.getVolCeilingEstimateForFreq(freq);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.testPairs.add(new FreqVolPair(freq, testVol));
            pct += jumpSize;
        }

        // prepare list of all trials
        ArrayList<FreqVolPair> allTrials = new ArrayList<>();
        for (int i = 0; i < trialsPerTone; i++) {
            allTrials.addAll(this.testPairs);
        }
        Collections.shuffle(allTrials);
        this.testPairs = allTrials;

        if (this.testPairs.size() != trialsPerTone * DEFAULT_FREQUENCIES.length)
            Log.e("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * DEFAULT_FREQUENCIES.length +
                                        " test pairs but generated " + this.testPairs.size());
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, TONE_DURATION_MS);
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        FreqVolPair fvp = (FreqVolPair) trial.tone;
        return String.format("%s, %s, %d clicks: %s",
                fvp.toString(), trial.wasCorrect() ? "Heard" : "notHeard", trial.nClicks(),
                Arrays.toString(trial.clickTimes()));
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
