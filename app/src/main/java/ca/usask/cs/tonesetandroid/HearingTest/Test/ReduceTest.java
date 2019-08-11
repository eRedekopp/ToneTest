package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

public abstract class ReduceTest extends HearingTest {

    private static final float HEARING_TEST_REDUCE_RATE = 0.2f; // reduce by this percentage each time

    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;       // number of times listener must fail to hear a tone in the
                                                            // reduction phase of the hearing test before the volume is
                                                            // considered "inaudible"

    ReduceTestResults results;
    ArrayList<FreqVolPair> currentVolumes;
    HashMap<Float, Integer> timesNotHeardPerFreq;

    public ReduceTest(RampTest.RampTestResults rampResults) {
        this.currentVolumes = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();

        for (FreqVolPair fvp : rampResults.getResults()) {
            this.currentVolumes.add(fvp);
            this.timesNotHeardPerFreq.put(fvp.freq, 0);
        }
    }

    protected abstract void playTone(FreqVolPair fvp);

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);

                    while (! isComplete() && ! iModel.testPaused()) {
                        for (FreqVolPair trial : currentVolumes) {
                            if (iModel.testPaused()) return;

                            newCurrentTrial(trial);
                            iModel.resetAnswer();
                            Log.i(testTypeName, "Testing " + trial.toString());
                            playTone(trial);
                            if (! iModel.answered())
                                mapReplace(timesNotHeardPerFreq, trial.freq, timesNotHeardPerFreq.get(trial.freq) + 1);
                            Log.i(testTypeName, iModel.answered() ? "Tone Heard" : "Tone not heard");
                            saveLine();
                            sleepThread(1000, 3000);
                        }
                        reduceCurrentVolumes();
                    }
                } finally { iModel.setTestThreadActive(false); }
            }
        }).start();
    }

    @Override
    boolean isComplete() {
        return this.currentVolumes.isEmpty();
    }

    @Override
    protected final String getLineEnd(SingleTrialResult result) {
        return String.format("%s, %s, %d clicks: %s",
                this.currentTrial.tone.toString(),
                this.currentTrial.wasCorrect() ? "Heard" : "NotHeard", this.currentTrial.nClicks(),
                Arrays.toString(this.currentTrial.clickTimes()));
    }

    /**
     * Reduce all elements of currentVolumes by [element * HEARING_TEST_REDUCE_RATE]
     */
    @SuppressWarnings("ConstantConditions")
    public void reduceCurrentVolumes() {
        ArrayList<FreqVolPair> newVols = new ArrayList<>();
        for (FreqVolPair fvp : currentVolumes) {
            // only reduce volumes of frequencies still being tested
            if (timesNotHeardPerFreq.get(fvp.freq) >= TIMES_NOT_HEARD_BEFORE_STOP) results.addResult(fvp);
            else newVols.add(new FreqVolPair(fvp.freq, fvp.vol * (1 - HEARING_TEST_REDUCE_RATE)));
        }
        this.currentVolumes = newVols;
    }

    public FreqVolPair[] getResults() {
        return this.results.getResults();
    }

    /**
     * Replace the value associated with the given key in the map with the given new value, or just associate the key
     * with the value if not already present.
     *
     * @param map A HashMap
     * @param key A valid key for that hashmap (not necessarily present in map)
     * @param newValue The new value with which to associate the key
     */
    public void mapReplace(HashMap<Float, Integer> map, Float key, Integer newValue) {
        map.remove(key);
        map.put(key, newValue);
    }

    public class ReduceTestResults {
        ArrayList<FreqVolPair> results;

        public ReduceTestResults() {
            this.results = new ArrayList<>();
        }

        public void addResult(FreqVolPair fvp) {
            results.add(fvp);
        }

        public FreqVolPair[] getResults() {
            return (FreqVolPair[]) results.toArray();
        }
    }
}
