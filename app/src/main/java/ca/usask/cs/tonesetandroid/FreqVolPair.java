package ca.usask.cs.tonesetandroid;

/**
 * A data structure for storing a frequency, volume pair
 *
 * @author alexscott
 */
public class FreqVolPair {

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

}
