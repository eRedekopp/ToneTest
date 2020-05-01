package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;

/**
 * A class for storing the pitches and their associated durations in a melody. "Melody", in this case, is defined as
 * any group of 2 or more pitches with associated durations
 */
public class Melody extends Tone implements Cloneable {

    /**
     * How long should the melody last in total?
     */
    protected static final int MELODY_DURATION_MS = 1500;

    /**
     * The frequencies, volumes, and durations of each note in this melody
     */
    protected List<FreqVolDurTrio> notes;

    /**
     * The volume of this melody
     */
    protected double vol;

    @Override
    public int direction() {
        float firstFreq = notes.get(0).freq();
        float lastFreq  = notes.get(notes.size() - 1).freq();
        return firstFreq < lastFreq ? DIRECTION_UP : (firstFreq == lastFreq ? DIRECTION_FLAT : DIRECTION_DOWN);
    }

    public Melody(List<FreqVolDurTrio> notes, double vol) {
        this.notes = notes;
        this.vol = vol;
    }

    /**
     * Create a new Melody with a pre-set list of notes identified by the string, starting on freq1 and played at the
     * given volume
     *
     * @param identifier The String identifier for the desired melody (see source code for options)
     * @param freq1 The frequency (pitch) of the first note of the new melody
     * @param vol The volume at which to play this melody
     */
    public Melody(String identifier, float freq1, double vol) {
        ArrayList<FreqVolDurTrio> noteList = new ArrayList<>();
        float[] freqs = getFrequenciesForPreset(identifier, freq1);
        switch (identifier) {
            case "maj-triad-up":
                // Major triad first inversion upward, 2 quarter notes + half note
                // same as maj-triad-down
            case "maj-triad-down":
                // Major triad first inversion downward, 2 quarter notes + half note
                noteList.add(new FreqVolDurTrio(freqs[0], vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(freqs[1], vol, MELODY_DURATION_MS/4));
                noteList.add(new FreqVolDurTrio(freqs[2], vol, MELODY_DURATION_MS/2));
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
                break;
            default:
                throw new RuntimeException("Unknown string identifier: " + identifier);
        }
        this.notes = noteList;
        this.vol = vol;
    }

    /**
     * Return all the notes that would be contained in a preset melody called with the String constructor for this class
     * All melody intervals are calculated in 12-Tone Equal Temperament (12-TET)
     *
     * @param identifier The string identifier for the desired melody (same as constructor options)
     * @param freq1 The first frequency of the melody
     * @return A list of frequencies that would be in the melody if the constructor was called
     */
    public static float[] getFrequenciesForPreset(String identifier, float freq1) {
        // min3 -> 1.189207 in 12-TET
        // P4   -> 1.334840 in 12-TET
        // min6 -> 1.587401 in 12-TET
        switch (identifier) {
            case "maj-triad-up":
                // Major triad first inversion upward, 2 quarter notes + half note
                // starting note -> min 3rd up from setStartTime -> min6 up from setStartTime
                float freq2 = 1.189207f * freq1;
                float freq3 = 1.587401f * freq1;
                return new float[]{freq1, freq2, freq3};
            case "maj-triad-down":
                // Major triad first inversion downward, 2 quarter notes + half note
                // starting note -> P4 down from setStartTime -> min6 down from setStartTime
                freq2 = freq1 / 1.334840f;
                freq3 = freq1 / 1.587401f;
                return new float[]{freq1, freq2, freq3};
            case "single-freq-rhythm":
                // Single-tone syncopated rhythm: | = 8th note, . = 8th rest -> |.||.|.|
                return new float[]{freq1};
            default:
                throw new RuntimeException("Unknown string identifier: " + identifier);
        }
    }

    /**
     * @return All FreqVolDurTrios in this melody
     */
    public FreqVolDurTrio[] getTones() {
        return this.notes.toArray(new FreqVolDurTrio[0]);
    }

    /**
     * @return All FreqVolDurTrios in this melody with volume != 0
     */
    public FreqVolDurTrio[] getAudibleTones() {
        ArrayList<FreqVolDurTrio> nonRestNotes = new ArrayList<>();
        for (FreqVolDurTrio note : this.notes) if (note.vol() != 0) nonRestNotes.add(note);
        return nonRestNotes.toArray(new FreqVolDurTrio[0]);
    }

    @Override
    public Melody newVol(double newVol) {
        if (newVol == this.vol()) return this.clone();
        List<FreqVolDurTrio> newNotes = new ArrayList<>();
        for (FreqVolDurTrio note : this.notes) newNotes.add(note.newVol(newVol));
        return new Melody(newNotes, vol);
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

    @Override
    @NonNull
    public String toString() {
        return String.format("freq: %.2f, vol: %.2f, direction: %s", this.freq(), this.vol(), this.directionAsString());
    }
}
