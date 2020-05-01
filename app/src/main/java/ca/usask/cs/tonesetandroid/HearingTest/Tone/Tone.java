package ca.usask.cs.tonesetandroid.HearingTest.Tone;


/**
 * Parent class for all types that can be played in a hearing test
 */
public abstract class Tone {

    // the direction of a tone. Single-pitch tones are all considered to be FLAT
    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_FLAT = 2;

    // the timbre of a tone
    public static final int TIMBRE_PIANO = 0;
    public static final int TIMBRE_SINE = 1;
    public static final int TIMBRE_WAV = 2;

    // the type of a tone
    public static final int TYPE_SINGLE = 0;
    public static final int TYPE_INTERVAL = 1;
    public static final int TYPE_MELODY = 2;

    /**
     * @return The volume of this Tone, where 0 is not audible at all and Double.MAX_VALUE is the maximum
     */
    public abstract double vol();

    /**
     * @return The frequency in Hz of the first pitch of this Tone
     */
    public abstract float freq();

    /**
     * @return One of DIRECTION_UP, DIRECTION_DOWN, and DIRECTION_FLAT, depending on the 'direction' of this tone.
     * Single-pitch tones are all considered to be FLAT
     */
    public abstract int direction();

    /**
     * @return A new Tone identical to this one except with the given volume
     */
    public abstract Tone newVol(double vol);

    public boolean equals(Tone other) {
        return this.vol() == other.vol() && this.freq() == other.freq();
    }

    /**
     * Return a string corresponding to the direction of this Tone
     */
    public String directionAsString() {
        switch (this.direction()) {
            case DIRECTION_DOWN: return "down";
            case DIRECTION_FLAT: return "flat";
            case DIRECTION_UP:   return "up";
            default: throw new RuntimeException("Unknown direction identifier: " + this.direction());
        }
    }

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
}
