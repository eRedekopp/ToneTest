package ca.usask.cs.tonesetandroid.HearingTest.Tone;


/**
 * An interface for anything that can be used as a single trial for a tone test
 */
public abstract class Tone {

    public abstract double vol();

    public abstract float freq();

    /**
     * Given a list of FreqVolPairs, return the volume associated with the given frequency in a pair
     *
     * @param list A list of freqvolpairs
     * @param freq The frequency whose corresponding volume is to be returned
     * @return The volume of the first freqvolpair with the given frequency in the list
     * @throws IllegalArgumentException if there is no pair with the given frequency
     */
    public static double getVolForFreq(Tone[] list, Float freq) throws IllegalArgumentException {
        for (Tone tone : list) if (tone.freq() == freq) return tone.vol();
        throw new IllegalArgumentException("Requested frequency not present in list");
    }
}
