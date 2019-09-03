package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

public class SineReduceTest extends ReduceTest<FreqVolPair> {

    private static final int SINE_DURATION_MS = 1500;

    public SineReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testTypeName = "sine-reduce";
    }

    @Override
    public void initialize(RampTestResults rampResults) {
        this.currentVolumes = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        this.results = new ReduceTestResults();

        for (FreqVolPair fvp : rampResults.getResultsArray()) {
            this.currentVolumes.add(fvp);
            this.timesNotHeardPerFreq.put(fvp.freq(), 0);
        }
        Collections.shuffle(this.currentVolumes);
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        playSine(tone, SINE_DURATION_MS);
    }
}
