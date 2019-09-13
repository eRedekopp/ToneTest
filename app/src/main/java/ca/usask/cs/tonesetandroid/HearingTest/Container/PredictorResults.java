package ca.usask.cs.tonesetandroid.HearingTest.Container;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * A class for any HearingTestResults which can calculate probabilities of hearing a given Tone not necessarily
 * tested in the associated HearingTest
 */
public abstract class PredictorResults extends HearingTestResults {

    public PredictorResults(BackgroundNoiseType noiseType, String testTypeName) {
        super(noiseType, testTypeName);
    }

    /**
     * Get the probability of hearing the given tone, given the results of these HearingTestResults
     *
     * @param tone The tone whose probability of being heard is to be estimated
     * @return The estimated probability of hearing the tone, given these results
     * @throws IllegalStateException If there is no data stored in these results
     */
    public abstract double getProbability(Tone tone) throws IllegalStateException;

    /**
     * Return a string containing all estimates of P(heard) for the given tone that can be calculated by these
     * PredictorResults. If more than one probability, include a label for each
     *
     * @param tone The tone for which P(heard) is to be estimated
     * @return A string containing all estimates of P(heard) from these PredictorResults
     */
    public abstract String getPredictionString(Tone tone);

    /**
     * Get an estimate for the highest volume with P(heard) = 0 for the given frequency
     *
     * @param freq A frequency
     * @return An estimate for the highest volume with P(heard) = 0 for freq
     */
    public abstract double getVolFloorEstimate(float freq);

    /**
     * Get an estimate for the lowest volume with P(heard) = 1 for the given frequency
     *
     * @param freq A frequency
     * @return An estimate for the lowest volume with P(heard) = 1 for freq
     */
    public abstract double getVolCeilingEstimate(float freq);
}
