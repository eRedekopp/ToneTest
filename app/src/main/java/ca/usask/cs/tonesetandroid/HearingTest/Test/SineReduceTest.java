package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.Arrays;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SineReduceTest extends ReduceTest<FreqVolPair> {

    private static final int SINE_DURATION_MS = 1500;

    public SineReduceTest(RampTest.RampTestResults rampTestResults, BackgroundNoiseType noiseType) {
        super(rampTestResults, noiseType);
        this.testTypeName = "sine-reduce";
    }

    @Override
    protected void configureCurrentVolumes() {
        this.currentVolumes.addAll(Arrays.asList(this.rampResults.getResults()));
    }

    @Override
    protected void playTone(Tone tone) {
        playSine((FreqVolPair) tone, SINE_DURATION_MS);
    }
}
