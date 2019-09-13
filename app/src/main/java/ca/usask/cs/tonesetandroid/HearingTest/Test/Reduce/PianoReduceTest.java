package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public class PianoReduceTest extends WavReduceTest {

    public PianoReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    public String getTestTypeName() {
        return "piano-reduce";
    }

    /**
     * @throws IllegalStateException if rampResults.getResultsArray() contains a frequency without a default
     * wav resource (see WavTone constructor)
     */
    @Override
    public void initialize() throws IllegalStateException {
        // todo get rid of default constructors and build WavTones here
        this.currentVolumes = new ArrayList<>();
        this.timesNotHeardPerFreq = new HashMap<>();
        this.results = new ReduceTestResults();

        for (FreqVolPair fvp : rampResults.getResultsArray()) {
            this.currentVolumes.add(new WavTone(fvp.freq(), fvp.vol()));
            this.timesNotHeardPerFreq.put(fvp.freq(), 0);
        }
        Collections.shuffle(this.currentVolumes);
    }
}
