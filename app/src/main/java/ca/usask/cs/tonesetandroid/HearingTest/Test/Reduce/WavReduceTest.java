package ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class WavReduceTest extends ReduceTest<WavTone> {

    public WavReduceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    public void playTone(WavTone tone) {
        this.playWav(tone);
    }

}
