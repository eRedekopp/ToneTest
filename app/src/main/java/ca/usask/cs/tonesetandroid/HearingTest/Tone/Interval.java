package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

/**
 * A tone composed of two pitches at the same volume
 */
public class Interval extends Tone implements Cloneable {

    /**
     * The first frequency of the interval
     */
    private final float freq1;

    /**
     * The second frequency of the interval
     */
    private final float freq2;

    /**
     * The volume of the tones
     */
    private final double vol;

    public Interval(float freq1, float freq2, double vol) {
        this.freq1 = freq1;
        this.freq2 = freq2;
        this.vol = vol;
    }

    @Override
    public int direction() {
        return freq1 < freq2 ? DIRECTION_UP : (freq1 == freq2 ? DIRECTION_FLAT : DIRECTION_DOWN);
    }

    @Override
    public double vol() {
        return this.vol;
    }

    @Override
    public float freq() {
        return this.freq1;
    }

    public float freq2() {
        return this.freq2;
    }

    @Override
    public Interval newVol(double vol) {
        return new Interval(this.freq1, this.freq2, vol);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("Freq1: %.1f, Freq2: %.1f, vol: %.1f ", this.freq1, this.freq2, this.vol);
    }

    @Override
    public Interval clone() {
        try {
            return (Interval) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Interval(this.freq1, this.freq2, this.vol);
        }
    }
}
