package ca.usask.cs.tonesetandroid.HearingTest.Test;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class WavRampTest extends RampTest<WavTone> {

    public WavRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

}
