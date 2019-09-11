package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * Parent class for all RampTests that test WavTones
 */
public abstract class WavRampTest extends RampTest<WavTone> {

    public WavRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    protected double rampUp(double rateOfRamp, WavTone tone, double startingVol) {
        // play a piano note over and over again, slowly getting louder each time
        model.startAudio();
        try {
            for (double volume = startingVol; volume < Short.MAX_VALUE; volume *= rateOfRamp) {
                if (!iModel.testing() || iModel.testPaused()) return -1;
                this.playWav(tone.newVol(volume));
                if (iModel.answered()) return volume;
            }
            return Short.MAX_VALUE;
        } finally {
            model.pauseAudio();
        }
    }
}
