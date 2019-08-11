package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.ReducibleTone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public abstract class ReduceTest<T extends ReducibleTone> extends HearingTest<T> {

    private static final float HEARING_TEST_REDUCE_RATE = 0.2f; // reduce by this percentage each time

    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;       // number of times listener must fail to hear a tone in the
                                                            // reduction phase of the hearing test before the volume is
                                                            // considered "inaudible"

    protected RampTest.RampTestResults rampResults;
    protected ReduceTestResults results;
    protected ArrayList<T> currentVolumes;
    protected HashMap<Float, Integer> timesNotHeardPerFreq;

    public ReduceTest(RampTest.RampTestResults rampResults, BackgroundNoiseType noiseType) {
        super(noiseType);
        this.rampResults = rampResults;
        this.currentVolumes = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        this.configureCurrentVolumes();
    }

    protected abstract void playTone(Tone tone);

    protected abstract void configureCurrentVolumes();

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);

                    while (! isComplete() && ! iModel.testPaused()) {
                        for (T trial : currentVolumes) {
                            if (iModel.testPaused()) return;

                            newCurrentTrial(trial);
                            iModel.resetAnswer();
                            Log.i(testTypeName, "Testing " + trial.toString());
                            currentTrial.start();
                            playTone(trial);
                            if (! iModel.answered())
                                mapReplace(timesNotHeardPerFreq, trial.freq(),
                                           timesNotHeardPerFreq.get(trial.freq()) + 1);
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
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public void reduceCurrentVolumes() {
        ArrayList<T> newVols = new ArrayList<>();
        for (T tone : currentVolumes) {
            // only reduce volumes of frequencies still being tested
            if (timesNotHeardPerFreq.get(tone.freq()) >= TIMES_NOT_HEARD_BEFORE_STOP) results.addResult(tone);
            else newVols.add((T) tone.newVol(tone.vol() * (1 - HEARING_TEST_REDUCE_RATE)));
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

        public void addResult(Tone tone) {
            results.add(new FreqVolPair(tone.freq(), tone.vol()));
        }

        public FreqVolPair[] getResults() {
            return (FreqVolPair[]) results.toArray();
        }
    }
}
