package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for storing the pitches and their associated durations in a melody. "Melody" in this case is defined as
 * any group of 2 or more pitches with associated durations
 */
public class Melody extends Tone implements Cloneable {

    /**
     * How long should each melody last in total?
     */
    protected static final int MELODY_DURATION_MS = 1500;

    protected List<FreqVolDurTrio> notes;

    protected double vol;

    protected int direction;

    public Melody(List<FreqVolDurTrio> notes, double vol) {
        this.notes = notes;
        this.vol = vol;
        if (notes.get(0).freq() > this.notes.get(this.notes.size()-1).freq()) this.direction = Earcon.DIRECTION_DOWN;
        else if (notes.get(0).freq() < this.notes.get(this.notes.size()-1).freq()) this.direction = Earcon.DIRECTION_UP;
        else this.direction = Earcon.DIRECTION_FLAT;
    }

    /**
     * Create a new Melody with a pre-set list of notes identified by the string, starting on freq1 and played at the
     * given volume
     *
     * @param identifier The String identifier for the desired melody (see source code for options)
     * @param freq1 The frequency at which to start this melody
     * @param vol The volume at which to play this melody
     */
    public Melody(String identifier, float freq1, double vol) {
        ArrayList<FreqVolDurTrio> noteList = new ArrayList<>();
        switch (identifier) {
            case "maj-triad-up":
                // Major triad first inversion upward, 2 quarter notes + half note
                noteList.add(new FreqVolDurTrio(freq1, vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(6f*freq1/5f, vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(8f*freq1/5f, vol, MELODY_DURATION_MS/2));
                this.direction = Earcon.DIRECTION_UP;
                break;
            case "maj-triad-down":
                // Major triad first inversion downward, 2 quarter notes + half note
                noteList.add(new FreqVolDurTrio(freq1, vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(3f*freq1/4f, vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(5f*freq1/8f, vol, MELODY_DURATION_MS/2));
                this.direction = Earcon.DIRECTION_DOWN;
                break;
            case "single-freq-rhythm":
                // Single-tone syncopated rhythm: | = 8th note, . = 8th rest -> |.||.|.|
                FreqVolDurTrio eighthNote = new FreqVolDurTrio(freq1, vol, MELODY_DURATION_MS/8);
                FreqVolDurTrio eighthRest = new FreqVolDurTrio(0, 0, MELODY_DURATION_MS/8);
                noteList.add(eighthNote);
                noteList.add(eighthRest);
                noteList.add(eighthNote);
                noteList.add(eighthNote);
                noteList.add(eighthRest);
                noteList.add(eighthNote);
                noteList.add(eighthRest);
                noteList.add(eighthNote);
                this.direction = Earcon.DIRECTION_FLAT;
                break;
            default:
                throw new RuntimeException("Unknown string identifier: " + identifier);
        }
        this.notes = noteList;
        this.vol = vol;
    }

    /**
     * Return all the notes that would be contained in a preset melody called with the String constructor for this class
     *
     * @param identifier The string identifier for the desired melody (same as constructor options)
     * @param freq1 The first frequency of the melody
     * @return A list of frequencies that would be in the melody if the constructor was called
     */
    public static float[] getFrequenciesForPreset(String identifier, float freq1) {
        switch (identifier) {
            case "maj-triad-up":
                // Major triad first inversion upward, 2 quarter notes + half note
                return new float[]{freq1, 6f*freq1/5f, 8f*freq1/5f};
            case "maj-triad-down":
                // Major triad first inversion downward, 2 quarter notes + half note
                return new float[]{freq1, 3f*freq1/4f, 5f*freq1/8f};
            case "single-freq-rhythm":
                // Single-tone syncopated rhythm: | = 8th note, . = 8th rest -> |.||.|.|
                return new float[]{freq1};
            default:
                throw new RuntimeException("Unknown string identifier: " + identifier);
        }
    }

    public FreqVolDurTrio[] getNotes() {
        return this.notes.toArray(new FreqVolDurTrio[this.notes.size()]);
    }

    /**
     * NOTE: a melody is categorized as "up" "down" or "flat" based only on the first and last notes of the melody
     *
     * @return An integer representing the direction of this melody
     */
    public int direction() {
        return this.direction;
    }

    public String directionAsString() {
        switch (this.direction) {
            case Earcon.DIRECTION_DOWN: return "down";
            case Earcon.DIRECTION_FLAT: return "flat";
            case Earcon.DIRECTION_UP:   return "up";
            default: throw new RuntimeException("Unknown direction identifier: " + this.direction);
        }
    }

    /**
     * @return The frequency of the first note in this melody
     */
    @Override
    public float freq() {
        return notes.get(0).freq();
    }

    @Override
    public double vol() {
        return this.vol;
    }

    @Override
    public Melody clone() {
        try {
            return (Melody) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Melody(this.notes, this.vol);
        }
    }
}
