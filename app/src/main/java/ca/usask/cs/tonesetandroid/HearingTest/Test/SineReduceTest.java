package ca.usask.cs.tonesetandroid.HearingTest.Test;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

public class SineReduceTest extends ReduceTest {

    private static final int SINE_DURATION_MS = 1500;

    public SineReduceTest(RampTest.RampTestResults rampTestResults) {
        super(rampTestResults);
        this.testTypeName = "sine-reduce";
    }

    @Override
    protected void playTone(FreqVolPair fvp) {
        playSine(fvp, SINE_DURATION_MS);
    }
}
