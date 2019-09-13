package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * A class to store the results of a ConfidenceTest
 */
public class ConfidenceTestResults extends HearingTestResults {

    /**
     * A mapping between all the frequencies tested in this ConfidenceTest and the corresponding SingleToneResult
     * containing the results of all trials of the tone of that frequency
     *
     * NOTE this only allows for one volume per frequency
     */
    private HashMap<Float, SingleToneResult> allResults;

    public ConfidenceTestResults(BackgroundNoiseType noiseType, String testTypeName) {
        super(noiseType, testTypeName);
        this.allResults = new HashMap<>();
    }

    /**
     * Add the result of a single trial to these results
     * @param tone The tone tested in this trial
     * @param wasCorrect Did the user answer correctly?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(Tone tone, boolean wasCorrect) {
        SingleToneResult result;
        if (allResults.containsKey(tone.freq())) result = allResults.get(tone.freq());
        else result = new SingleToneResult(tone);
        result.addResult(wasCorrect);
    }

    /**
     * @return A Collection containing all the tones tested in this ConfidenceTest
     */
    @SuppressWarnings("ConstantConditions")
    public Collection<Tone> getTestedTones() {
        ArrayList<Tone> allTones = new ArrayList<>();
        for (Float freq : this.allResults.keySet()) allTones.add(this.allResults.get(freq).tone);
        return allTones;
    }

    /**
     * @return A Collection containing all frequencies tested in this ConfidenceTest
     */
    public Collection<Float> getTestedFreqs() {
        return this.allResults.keySet();
    }

    /**
     * Get the probability found for the tone with the given frequency
     *
     * @param freq The frequency of the tone whose probability is being queried
     * @return The probability found for the tone of the given frequency
     * @throws IllegalArgumentException If the given frequency was not tested
     */
    public double getProbability(Float freq) throws IllegalArgumentException {
        SingleToneResult result = this.allResults.get(freq);
        if (result == null) throw new IllegalArgumentException("Frequency not found in results: " + freq);
        else return ((double) result.getCorrect()) / (result.getCorrect() + result.getIncorrect());
    }

    @Override
    public boolean isEmpty() {
        return this.allResults.isEmpty();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * An object containing the results from a single tone tested in this ConfidenceTest
     */
    protected static class SingleToneResult {

        /**
         * The number of times that the user answered this tone correctly
         */
        private int correct;

        /**
         * The number of times that the user answered this tone incorrectly
         */
        private int incorrect;

        /**
         * The tone whose results are being stored
         */
        public final Tone tone;

        public SingleToneResult(Tone tone) {
            this.correct = 0;
            this.incorrect = 0;
            this.tone = tone;
        }

        /**
         * Add the result of a single trial to these results
         */
        public void addResult(boolean wasCorrect) {
            if (wasCorrect) correct++;
            else incorrect++;
        }

        /**
         * @return The number of times that the user answered this tone correctly
         */
        public int getCorrect() {
            return this.correct;
        }

        /**
         * @return The number of times that the user answered this tone incorrectly
         */
        public int getIncorrect() {
            return this.incorrect;
        }
    }
}
