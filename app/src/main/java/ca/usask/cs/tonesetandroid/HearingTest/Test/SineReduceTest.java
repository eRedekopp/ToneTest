package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SineReduceTest extends ReduceTest<FreqVolPair> {

    private static final int SINE_DURATION_MS = 1500;

    public SineReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = "In this phase of the test, tones of various pitches and volumes will play at random times. " +
                        "Please press the \"Heard Tone\" button each time that you hear a tone";
        this.testTypeName = "sine-reduce";
    }

    @Override
    public void initialize(RampTest.RampTestResults rampResults) {
        for (FreqVolPair fvp : rampResults.getResults()) {
            this.currentVolumes.add(fvp);
            this.timesNotHeardPerFreq.put(fvp.freq(), 0);
        }
        Collections.shuffle(this.currentVolumes);
    }

    @Override
    protected void playTone(Tone tone) {
        playSine((FreqVolPair) tone, SINE_DURATION_MS);
    }
}
