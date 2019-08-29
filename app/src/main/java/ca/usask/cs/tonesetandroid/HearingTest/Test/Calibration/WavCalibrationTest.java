package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * A calibration test that uses WavTones
 */
public abstract class WavCalibrationTest extends CalibrationTest<WavTone> {

    public WavCalibrationTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    protected void playTone(WavTone tone) {
        this.playWav(tone);
    }

}
