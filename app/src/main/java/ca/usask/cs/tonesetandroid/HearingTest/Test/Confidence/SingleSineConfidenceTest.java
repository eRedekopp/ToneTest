package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResultsCollection;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;

/**
 * A ConfidenceTest that tests the user's ability to hear a single sine wave
 */
public class SingleSineConfidenceTest extends ConfidenceTest<FreqVolPair> {

    public  SingleSineConfidenceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    public String getTestTypeName() {
        return "sine-single-tone-conf";
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
                            playSine(freq, 70, DEFAULT_TONE_DURATION_MS);
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
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {

        if (frequencies.length == 0) return;

        HearingTestResultsCollection results = model.getCurrentParticipant().getResults();

        // create a list containing one copy of freq for each volume at which freq should be tested
        this.testTones = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (int i = 0; i < volsPerFreq; i++) for (float freq : frequencies) confFreqs.add(freq);
        Collections.shuffle(confFreqs);

        testTones.add(new FreqVolPair(      // add a test that will likely be heard every time
                confFreqs.get(0),
                results.getVolCeilingEstimate(confFreqs.get(0), this.getBackgroundNoiseType())));

        if (frequencies.length > 1)         // add a test that will likely not be heard at all
            testTones.add(new FreqVolPair(
                    confFreqs.get(1),
                    results.getVolFloorEstimate(confFreqs.get(1), this.getBackgroundNoiseType())));

        if (frequencies.length > 2)         // add a test that very likely will be heard every time
            testTones.add(new FreqVolPair(
                    confFreqs.get(2),
                    results.getVolCeilingEstimate(confFreqs.get(2), this.getBackgroundNoiseType()) * 1.25));

        if (frequencies.length > 3)         // add a test that extremely likely will be heard every time
            testTones.add(new FreqVolPair(
                    confFreqs.get(3),
                    results.getVolCeilingEstimate(confFreqs.get(3), this.getBackgroundNoiseType()) * 1.5));

        final int hardCodedCases = 4; // how many test cases are hard-coded like the ones above?

        // For every other frequency in the list, add a test case where it is somewhere between 40% and 100% of the
        // way between estimates for "completely inaudible" and "completely audible" volumes
        float pct = 0.4f;
        float jumpSize = (1 - pct) / (frequencies.length - hardCodedCases);
        for (int i = hardCodedCases; i < frequencies.length; i++, pct += jumpSize) {
            float freq = confFreqs.get(i);
            double volFloor = results.getVolFloorEstimate(freq, this.getBackgroundNoiseType());
            double volCeiling = results.getVolCeilingEstimate(freq, this.getBackgroundNoiseType());
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
            Log.w("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length * volsPerFreq +
                                        " test pairs but generated " + this.testTones.size());
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, DEFAULT_TONE_DURATION_MS);
    }

    @Override
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_HEARD};
    }
}
