package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

/**
 * A class representing an "earcon" at a certain volume. It is up to the programmer to ensure that frequency and
 * direction match up with the .wav file with the given audioResourceID
 */
public class Earcon extends Tone implements Cloneable {

    // to indicate the direction of the notes in the earcon
    public static final int DIRECTION_UP = 2;

    public static final int DIRECTION_DOWN = 3;

    public static final int DIRECTION_FLAT = 4;

    /**
     * An int indicating the direction of this earcon (use values from DIRECTION_*)
     */
    public final int direction;

    /**
     * The R.id of the .wav file associated with this earcon
     */
    public final int audioResourceID;

    /**
     * The volume at which this earcon is to be played
     */
    public final double volume;

    /**
     * The frequency of the first note of this earcon, or the lowest note if first is a chord
     */
    public final float frequency;

    /**
     * Create a new earcon
     *
     * @param freq The frequency of the first note of the new earcon, or the lowest note if first is a chord
     * @param audioResourceID The R.id of the .wav file associated with this earcon
     * @param volume The volume at which this earcon is to be played
     * @param direction The direction of the notes (use values from DIRECTION_*)
     * @throws IllegalArgumentException If value of direction is not recognized
     */
    public Earcon(float freq, int audioResourceID, double volume, int direction) throws IllegalArgumentException {
        if (direction != DIRECTION_DOWN && direction != DIRECTION_UP && direction != DIRECTION_FLAT)
            throw new IllegalArgumentException("Invalid direction value: " + direction);
        else this.direction = direction;
        this.frequency = freq;
        this.audioResourceID = audioResourceID;
        this.volume = volume;
    }

    public int getDirection() {
        return direction;
    }

    public int getAudioResourceID() {
        return audioResourceID;
    }

    public double getVolume() {
        return volume;
    }

    public float getFrequency() {
        return frequency;
    }

    @Override
    public Earcon clone() {
        try {
            return (Earcon) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Earcon(this.frequency, this.audioResourceID, this.volume, this.direction);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("Freq: %.1f, Volume: %.1f, Direction: %s",
                this.frequency, this.volume, this.getDirectionAsString());
    }

    public boolean equals(Earcon other) {
        return      this.direction == other.direction
                &&  this.frequency == other.frequency
                &&  this.volume == other.volume
                &&  this.audioResourceID == other.audioResourceID;
    }

    @Override
    public double vol() {
        return this.volume;
    }

    @Override
    public float freq() {
        return this.frequency;
    }

    public String getDirectionAsString() {
        switch (direction) {
            case DIRECTION_DOWN: return "down";
            case DIRECTION_UP: return "up";
            case DIRECTION_FLAT: return "flat";
            default: throw new IllegalStateException("Earcon has unexpected direction : " + direction);
        }
    }
}
