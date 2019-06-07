package ca.usask.cs.tonesetandroid;

import java.io.Serializable;

/**
 * A data structure for storing a frequency, volume pair
 *
 * @author alexscott
 */
public class FreqVolPair implements Cloneable {

    float freq;
    double vol;

    public FreqVolPair(float f, double v) {
        freq = f;
        vol = v;
    }

    /**
     * Get the frequency
     *
     * @return freq: the frequency component of the pair
     */
    public float getFreq() {
        return freq;
    }

    /**
     * Get the volume
     *
     * @return vol: the volume component of the pair
     */
    public double getVol() {
        return vol;
    }

    public String toString() {
        return String.format("Frequency: %f | Volume: %f", freq, vol);
    }

    public FreqVolPair clone() {
        return new FreqVolPair(this.freq, this.vol);
    }

}
