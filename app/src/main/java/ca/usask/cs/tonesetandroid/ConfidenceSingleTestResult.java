package ca.usask.cs.tonesetandroid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A class to store the result of a single freq-vol pair test from a confidence test
 *
 * @author redekopp
 */
public class ConfidenceSingleTestResult {

    public final float expected;
    public final float freq;
    public final double vol;

    private int nHeard;
    private int nNotHeard;

    public ConfidenceSingleTestResult(float expected, FreqVolPair fvp) {
        this.expected = expected;
        this.nHeard = 0;
        this.nNotHeard = 0;
        this.freq = fvp.getFreq();
        this.vol = fvp.getVol();
    }

    public void addTrial(boolean heard) {
        if (heard) this.nHeard++;
        else this.nNotHeard++;
    }

    public float getActual() {
        return (float) this.nHeard / (float) (this.nNotHeard + this.nHeard);
    }

}