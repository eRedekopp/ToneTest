package ca.usask.cs.tonesetandroid;

public class Interval implements Cloneable {

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
    public Interval clone() {
        try {
            return (Interval) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Interval(this.freq1, this.freq2, this.vol);
        }
    }
}
