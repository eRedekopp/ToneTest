package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

/**
 * A ReduceTest that tests single sine waves
 */
public class SineReduceTest extends ReduceTest<FreqVolPair> {

    /**
     * The duration in milliseconds of the tones in this test 
     */
    private static final int SINE_DURATION_MS = DEFAULT_TONE_DURATION_MS;

    public SineReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    public String getTestTypeName() {
        return "sine-reduce";
    }

    /**
     * @throws IllegalStateException If no ramp test results have been set
     */
    @Override
    public void initialize() throws IllegalStateException {
        if (this.rampResults == null) throw new IllegalStateException("rampResults not yet configured");

        this.currentVolumes = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        this.results = new ReduceTestResults();

        for (FreqVolPair fvp : this.rampResults.getResultsArray()) {
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
