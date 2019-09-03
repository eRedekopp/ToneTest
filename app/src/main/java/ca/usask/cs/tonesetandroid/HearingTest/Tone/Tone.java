package ca.usask.cs.tonesetandroid.HearingTest.Tone;


/**
 * Parent class for all types that can be played in a hearing test
 */
public abstract class Tone {

    /**
     * @return The volume of this Tone, where 0 is not audible at all and Double.MAX_VALUE is the maximum
     */
    public abstract double vol();

    /**
     * @return The frequency in Hz of the first pitch of this Tone
     */
    public abstract float freq();

    /**
     * @return A new Tone identical to this one except with the given volume
     */
    public abstract Tone newVol(double vol);

    /**
     * Given a list of FreqVolPairs, return the volume associated with the given frequency in a pair
     *
     * @param list An array of Tones
     * @param freq The frequency whose corresponding volume is to be returned
     * @return The volume of the first Tone with the given frequency in the list
     * @throws IllegalArgumentException if there is no pair with the given frequency
     */
    public static double getVolForFreq(Tone[] list, Float freq) throws IllegalArgumentException {
        for (Tone tone : list) if (tone.freq() == freq) return tone.vol();
        throw new IllegalArgumentException("Requested frequency not present in list");
    }

    public boolean equals(Tone other) {
        return this.vol() == other.vol() && this.freq() == other.freq();
    }
}
