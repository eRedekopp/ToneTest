package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SingleToneTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Parent class for "ReduceTests" in which tones are played quieter and quieter until the user stops indicating that
 * they've heard the tone - used to get an estimate of "inaudible" volumes.
 *
 * @param <T> The type of tone being played in this ReduceTest
 */
public abstract class ReduceTest<T extends Tone> extends SingleToneTest<T> {

    protected static final String DEFAULT_TEST_INFO =
            "In this phase of the test, tones of various pitches and volumes will play at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone";

    /**
     * Reduce by this percentage each time 
     */
    private static final float HEARING_TEST_REDUCE_RATE = 0.2f;

    /**
     * The number of times that the user must fail to hear a tone before it is considered "inaudible"
     */
    static final int TIMES_NOT_HEARD_BEFORE_STOP = 2; 

    /**
     * The results of this ReduceTest
     */
    protected ReduceTestResults results;

    /**
     * The results from the RampTest that preceded this ReduceTest
     */
    protected RampTestResults rampResults;

    /**
     * All tones to be played in the current round of the test 
     */
    protected ArrayList<T> currentVolumes;

    /**
     * Each frequency in this test mapped to the number of times that the user has failed to hear a tone at 
     * that frequency
     */
    protected HashMap<Float, Integer> timesNotHeardPerFreq;

    public ReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;
    }

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

                            newCurrentTrial(trial);
                            iModel.resetAnswer();
                            currentTrial.setStartTime();
                            playTone(trial);
                            if (iModel.testPaused()) {  // return without doing anything if user paused during tone
                                currentTrial = null;    // remove current trial so it isn't added to list
                                return;
                            }
                            if (! iModel.answered())
                                mapIncrement(timesNotHeardPerFreq, trial.freq());
                            currentTrial.setCorrect(iModel.answered());
                            sleepThread(1000, 3000);
                            saveLine();     // save line after waiting, so we save all the clicks that happened while
                                            // waiting
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
    public int[] getPossibleResponses() {
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

    /**
     * Return an array of freqvolpairs representing each frequency and its associated "inaudible" volume found in
     * this test
     */
    public FreqVolPair[] getLowestVolumes() {
        return this.results.getResults();
    }

    public void setRampResults(RampTestResults results) {
        this.rampResults = results;
    }

    /**
     * Replace the value associated with the given key in the map with the given new value, or just associate the key
     * with the value if not already present.
     *
     * @param map A HashMap
     * @param key A valid key for that hashmap (not necessarily present in map)
     * @param newValue The new value with which to associate the key
     */
    public void mapReplace(HashMap<Float, Integer> map, Float key, Integer newValue) { // todo doesn't map.put() already do this?
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

    /*
     * This is extremely over-engineered but not worth fixing
     */
    /**
     * A class representing the results of a reduce test
     */
    public static class ReduceTestResults  {
        ArrayList<FreqVolPair> results;

        public ReduceTestResults() {
            this.results = new ArrayList<>();
        }

        /**
         * Add a result to this object
         * 
         * @param tone A tone with the appropriate frequency and volume representing the "inaudible" volume found for 
         *             tone.freq()
         */
        public void addResult(Tone tone) {
            results.add(new FreqVolPair(tone.freq(), tone.vol()));
        }

        /**
         * @return An array with one FreqVolPair for each tested frequency, with vol= the "inaudible" volume found for freq 
         */
        public FreqVolPair[] getResults() {
            return results.toArray(new FreqVolPair[]{});
        }

        public boolean isEmpty() {
            return this.results.isEmpty();
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
