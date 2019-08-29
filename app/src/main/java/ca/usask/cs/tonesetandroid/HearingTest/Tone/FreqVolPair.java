package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

/**
 * A data structure for storing a frequency, volume pair
 *
 * @author alexscott, redekopp
 */
public class FreqVolPair extends Tone implements Cloneable {

    protected final float freq;
    protected final double vol;

    public FreqVolPair(float f, double v) {
        this.freq = f;
        this.vol = v;
    }

    @Override
    public FreqVolPair newVol(double vol) {
        return new FreqVolPair(this.freq, vol);
    }

    @Override
    public double vol() {
        return this.vol;
    }

    @Override
    public float freq() {
        return this.freq;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("Frequency: %f, Volume: %f", freq, vol);
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
     * Given an array of FreqVolPairs, return the n FreqVolPairs with the highest volumes
     * @param arr An array of FreqVolPairs
     * @param n The number of loudest pairs to return
     * @return An array containing the n loudest freqvolpairs in the array
     * @throws IllegalArgumentException if n > arr.length
     */
    public static FreqVolPair[] maxNVols(FreqVolPair[] arr, int n) throws IllegalArgumentException {
        if (n > arr.length) throw new IllegalArgumentException("n = " + n + " greater than arr.length = " + arr.length);
        if (n == arr.length) return arr;

        FreqVolPair[] loudest = new FreqVolPair[n];  // a sorted list of current top pairs

        for (FreqVolPair fvp : arr) {
            if (loudest[0] == null || fvp.vol > loudest[0].vol) {   // if louder than quietest in array, insert at
                int i = 1;                                          // appropriate index
                while (i < n) {
                    if (loudest[i] == null || loudest[i].vol < fvp.vol) loudest[i - 1] = loudest[i];
                    else break;
                    i++;
                }
                loudest[i - 1] = fvp;
            }
        }
        return loudest;
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

    /**
     * @param arr An array of freqvolpairs
     * @return An array of each pair's frequency in the same order
     */
    public static Float[] getFreqsFromPairs(FreqVolPair[] arr) {
        Float[] outArr = new Float[arr.length];
        for (int i = 0; i < arr.length; i++) outArr[i] = arr[i].freq();
        return outArr;
    }
}
