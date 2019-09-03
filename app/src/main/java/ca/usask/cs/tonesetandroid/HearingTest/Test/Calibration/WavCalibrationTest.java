package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * Parent class for all CalibrationTests that use WavTones
 */
public abstract class WavCalibrationTest extends CalibrationTest<WavTone> {

    public WavCalibrationTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    protected void playTone(WavTone tone) {
        this.playWav(tone);
    }
}
