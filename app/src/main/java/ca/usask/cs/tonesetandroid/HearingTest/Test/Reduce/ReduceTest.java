package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public abstract class ReduceTest<T extends Tone> extends HearingTest<T> {

    private static final float HEARING_TEST_REDUCE_RATE = 0.2f; // reduce by this percentage each time

    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2;       // number of times listener must fail to hear a tone in the
                                                            // reduction phase of the hearing test before the volume is
                                                            // considered "inaudible"

    protected ReduceTestResults results;
    protected ArrayList<T> currentVolumes;
    protected HashMap<Float, Integer> timesNotHeardPerFreq;

    public ReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    protected abstract void playTone(T tone);

    public abstract void initialize(RampTestResults rampResults);

    @Override
    protected void run() {
        if (this.currentVolumes.isEmpty()) throw new IllegalStateException("Test not initialized");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    iModel.setTestThreadActive(true);
                    model.setUpLineOut();

                    while (! isComplete()) {
                        for (T trial : currentVolumes) {
                            if (iModel.testPaused() || ! iModel.testing()) return;

                            saveLine();
                            newCurrentTrial(trial);
                            iModel.resetAnswer();
                            currentTrial.start();
                            playTone(trial);
                            if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                                currentTrial = null;    // remove current trial so it isn't added to list
                                return;
                            }
                            if (! iModel.answered())
                                mapIncrement(timesNotHeardPerFreq, trial.freq());
                            currentTrial.setCorrect(iModel.answered());
                            sleepThread(1000, 3000);
                        }
                        reduceCurrentVolumes();
                    }

                    controller.reduceTestComplete();

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    @Override
    public boolean isComplete() {
        return this.currentVolumes.isEmpty();
    }

    @Override
    protected final String getLineEnd(SingleTrialResult result) {
        return String.format("freq: %.2f, vol: %.2f, %s, %d clicks: %s",
                this.currentTrial.tone().freq(), this.currentTrial.tone().vol(),
                this.currentTrial.wasCorrect() ? "Heard" : "NotHeard", this.currentTrial.nClicks(),
                Arrays.toString(this.currentTrial.clickTimes()));
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
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
        Collections.shuffle(this.currentVolumes);
    }

    public FreqVolPair[] getLowestVolumes() {
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

    /**
     * Increase the value of the int associated with the given key in the given map by 1, or associate the key with
     * the integer 1 if not present
     */
    @SuppressWarnings("ConstantConditions")
    public static void mapIncrement(HashMap<Float, Integer> map, Float key) {
        if (! map.containsKey(key)) map.put(key, 1);
        else {
            int oldVal = map.get(key);
            map.remove(key);
            map.put(key, ++oldVal);
        }
    }

    public static class ReduceTestResults  {
        ArrayList<FreqVolPair> results;

        public ReduceTestResults() {
            this.results = new ArrayList<>();
        }

        public void addResult(Tone tone) {
            results.add(new FreqVolPair(tone.freq(), tone.vol()));
        }

        public FreqVolPair[] getResults() {
            return results.toArray(new FreqVolPair[]{});
        }

    }

    /**
     * A class for building a ReduceTestResults from file data - add all data with addResult(), then use build()
     * to get a ReduceTestResults containing each frequency and its lowest volume seen
     */
    public static class ResultsBuilder {
        private HashMap<Float, Double> curLowest;

        public ResultsBuilder() {
            curLowest = new HashMap<>();
        }

        /**
         * If vol is the lowest or first volume seen for the given frequency, update curLowest, else do nothing
         */
        @SuppressWarnings("ConstantConditions")
        public void addResult(float freq, double vol) {
            if (! curLowest.containsKey(freq) || curLowest.get(freq) > vol) curLowest.put(freq, vol);
        }

        /**
         * @return A ReduceTestResults containing each frequency and its lowest volume seen
         */
        @SuppressWarnings("ConstantConditions")
        public ReduceTestResults build() {
            ReduceTestResults newResults = new ReduceTestResults();
            for (float f : this.curLowest.keySet()) newResults.results.add(new FreqVolPair(f, this.curLowest.get(f)));
            return newResults;
        }
    }
}
