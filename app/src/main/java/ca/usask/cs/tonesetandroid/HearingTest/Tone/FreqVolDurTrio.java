package ca.usask.cs.tonesetandroid.HearingTest.Tone;

/**
 * FreqVolPair that also stores information about the duration of the tone
 */
public class FreqVolDurTrio extends FreqVolPair {

    /**
     * How many milliseconds does the tone last?
     */
    private final int durationMs;

    public FreqVolDurTrio(float freq, double vol, int durationMs) {
        super(freq, vol);
        this.durationMs = durationMs;
    }

    public int durationMs() {
        return this.durationMs;
    }

    @Override
    public ReducibleTone newVol(double vol) {
        return new FreqVolDurTrio(this.freq, vol, this.durationMs);
    }
}
