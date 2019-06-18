package ca.usask.cs.tonesetandroid;


/**
 * A class to store the result of a single freq-vol pair test from a confidence test
 *
 * @author redekopp
 */
public class ConfidenceSingleTestResult {

    // todo make this internal to ConfidenceTestResultsContainer

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

    public int getTimesHeard() {
        return this.nHeard;
    }

    public int getTimesNotHeard() {
        return  this.nNotHeard;
    }
}