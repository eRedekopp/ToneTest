package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;

public class SingleSineConfidenceTest extends ConfidenceTest {

    private static final int TONE_DURATION_MS = 1500;
    private static final int TRIALS_PER_FVP = 20;

    // for keeping track of trials
    ArrayList<FreqVolPair> testPairs;
    ListIterator<FreqVolPair> position;

    public SingleSineConfidenceTest(CalibrationTestResults calibResults) {
        this.testInfo =
                "In this test, tones of various frequencies and volumes will be played at random times. " +
                "Please press the \"Heard Tone\" button each time that you hear a tone";

        this.testTypeName = "sine-single-tone-conf";

        this.calibResults = calibResults;
        this.completedTrials = new ArrayList<>();
        this.testPairs = new ArrayList<>();
        this.configure();
        this.position = testPairs.listIterator(0);
    }

    @Override
    public void playSamples(int direction) {
        if (direction != DIRECTION_FLAT)
            throw new IllegalArgumentException("Found unexpected direction identifier: " + direction);

        for (float freq : STANDARD_FREQUENCIES) playSine(freq, 30, TONE_DURATION_MS);
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
                        FreqVolPair current = position.next();
                        newCurrentTrial(current);
                        Log.i("ConfidenceTest", "Testing sine: " + current.toString());
                        playSine(current, TONE_DURATION_MS);
                        currentTrial.setCorrect(DIRECTION_FLAT);
                        Log.i("ConfidenceTest", currentTrial.wasCorrect() ? "Tone Heard" : "Tone Not Heard");
                        saveLine();
                        sleepThread(1000, 3000);
                    }
                } finally { model.setTestThreadActive(false); }
            }
        }).start();
    }

    @Override
    boolean isComplete() {
        return ! this.position.hasNext();
    }

    private void configure() {

        // todo add louder trials

        ArrayList<Float> confFreqs = new ArrayList<>();
        for (float freq : STANDARD_FREQUENCIES) confFreqs.add(freq);

        // randomize the order of test frequencies
        Collections.shuffle(confFreqs);

        // for each frequency, add a new fvp to confidenceTestPairs with the frequency and a volume some percentage
        // of the way between completely inaudible and perfectly audible
        float pct = 0;  // the percentage of the way between the lowest and highest tested vol that this test will be
        float jumpSize = 1.0f / STANDARD_FREQUENCIES.length;
        for (Float freq : confFreqs) {
            double volFloor = this.calibResults.getVolFloorEstimateForFreq(freq);
            double volCeiling = this.calibResults.getVolCeilingEstimateForFreq(freq);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.testPairs.add(new FreqVolPair(freq, testVol));
            pct += jumpSize;
        }

        // prepare list of all trials
        ArrayList<FreqVolPair> allTrials = new ArrayList<>();
        for (int i = 0; i < TRIALS_PER_FVP; i++) {
            allTrials.addAll(this.testPairs);
        }
        Collections.shuffle(allTrials);
        this.testPairs = allTrials;
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        FreqVolPair fvp = (FreqVolPair) trial.tone;
        return String.format("freq: %.2f vol: %.2f, heard? %b, %d clicks, click times: %s",
                fvp.freq, fvp.vol, trial.wasCorrect(), trial.nClicks(), Arrays.toString(trial.clickTimes()));
    }

}
