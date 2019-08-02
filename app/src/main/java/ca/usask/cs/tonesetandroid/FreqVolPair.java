package ca.usask.cs.tonesetandroid;

/**
 * A data structure for storing a frequency, volume pair
 *
 * @author alexscott
 */
public class FreqVolPair implements Cloneable {

    public final float freq;
    public final double vol;

    public FreqVolPair(float f, double v) {
        freq = f;
        vol = v;
    }

    @Override
    public String toString() {
        return String.format("Frequency: %f | Volume: %f\n", freq, vol);
    }

    @Override
    public FreqVolPair clone() {
        try {
            return (FreqVolPair) super.clone();
        } catch (CloneNotSupportedException e) {
            return new FreqVolPair(this.freq, this.vol);
        }
    }

    /**
     * @param arr An array of freqvolpairs
     * @return The freqvolpair with the highest volume
     */
    public static FreqVolPair maxVol(FreqVolPair[] arr) {
        FreqVolPair curMax = arr[0];
        for (FreqVolPair fvp : arr) if (fvp.vol > curMax.vol) curMax = fvp;
        return curMax;
    }
}
