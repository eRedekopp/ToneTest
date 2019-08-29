package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class WavRampTest extends RampTest<WavTone> {

    public WavRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

}
