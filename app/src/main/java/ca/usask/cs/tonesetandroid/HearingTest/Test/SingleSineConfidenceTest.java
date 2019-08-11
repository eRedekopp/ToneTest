package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
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
    public void playSamples(int direction) {
        if (direction != ANSWER_FLAT)
            throw new IllegalArgumentException("Found unexpected direction identifier: " + direction);

        for (float freq : DEFAULT_FREQUENCIES) playSine(freq, 30, TONE_DURATION_MS);
    }

    @Override
    protected boolean wasCorrect() {
        return iModel.answered();
    }

    @Override
    protected void configureTestPairs(int trialsPerTone) {

        // todo add louder trials

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
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, TONE_DURATION_MS);
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        FreqVolPair fvp = (FreqVolPair) trial.tone;
        return String.format("%s, heard? %b, %d clicks, click times: %s",
                fvp.toString(), trial.wasCorrect(), trial.nClicks(), Arrays.toString(trial.clickTimes()));
    }

}
