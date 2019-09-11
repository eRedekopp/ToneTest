package ca.usask.cs.tonesetandroid.HearingTest.Container;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * Interface for all HearingTest results container classes whose results can be used
 * to generate probabilities.
 */
public abstract class HearingTestResults {

    /**
     * The background noise type played during the test
     */
    private BackgroundNoiseType noiseType;

    public HearingTestResults(BackgroundNoiseType noiseType) {
        this.noiseType = noiseType;
    }

    /**
     * Get the probability of hearing the given tone, given the results of these HearingTestResults
     *
     * @param tone The tone whose probability of being heard is to be estimated
     * @return The estimated probability of hearing the tone, given these results
     * @throws IllegalStateException If there is no data stored in these results
     */
    public abstract double getProbability(Tone tone) throws IllegalStateException;

    public abstract double getProbability(Interval tone) throws IllegalStateException;

    public abstract double getProbability(Melody tone) throws IllegalStateException;

    public abstract double getProbability(WavTone tone) throws IllegalStateException;

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

    /**
     * Are there any results stored in this container?
     */
    public abstract boolean isEmpty();

    public BackgroundNoiseType getNoiseType() {
        return noiseType;
    }
}
