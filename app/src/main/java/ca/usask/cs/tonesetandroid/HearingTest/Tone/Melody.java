package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import java.util.ArrayList;

/**
 * A class for storing the pitches and their associated durations in a melody. "Melody" in this case is defined as
 * any group of 2 or more pitches with associated durations
 */
public class Melody extends Tone {

    protected ArrayList<FreqVolDurTrio> tones;

    protected double vol;

    public Melody() {

    }

    public float freq() {
        return 0.0f;
    }

    public double vol() {
        return 0.0;
    }

}
