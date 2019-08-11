package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

public class Interval extends Tone implements Cloneable {

    public final float freq1;

    public final float freq2;

    public final double vol;

    public final boolean isUpward;

    public Interval(float freq1, float freq2, double vol) {
        this.freq1 = freq1;
        this.freq2 = freq2;
        this.vol = vol;
        this.isUpward = freq1 < freq2;
    }

    @Override
    public double vol() {
        return this.vol;
    }

    @Override
    public float freq() {
        return this.freq1;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("| Freq1: %.1f, Freq2: %.1f, vol: %.1f | ", this.freq1, this.freq2, this.vol);
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
