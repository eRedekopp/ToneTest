package ca.usask.cs.tonesetandroid.HearingTest.Container;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.SinglePitchTone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public interface HearingTestResults {

    /**
     * Get the probability of hearing the given tone, given the results of these HearingTestResults
     *
     * @param tone The tone whose probability of being heard is to be estimated
     * @return The estimated probability of hearing the tone, given these results
     * @throws IllegalStateException If there is no data stored in these results
     */
    double getProbability(Tone tone) throws IllegalStateException;

    double getProbability(SinglePitchTone tone) throws IllegalStateException;

    double getProbability(Interval tone) throws IllegalStateException;

    boolean isEmpty();

}
