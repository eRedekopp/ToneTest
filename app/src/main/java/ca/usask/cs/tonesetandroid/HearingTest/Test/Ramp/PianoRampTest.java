package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public class PianoRampTest extends WavRampTest {

    public PianoRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);

        this.testInfo = DEFAULT_TEST_INFO;
        this.testTypeName = "piano-ramp";
        this.tones = new ArrayList<>();
        for (float freq : DEFAULT_CALIBRATION_FREQUENCIES) tones.add(new WavTone(freq, 0));
        this.position = tones.listIterator(0);
    }

    @Override
    protected float getRampRate1() {
        return 1.3f;  // todo tweak this
    }

    @Override
    protected float getRampRate2() {
        return 1.1f;  // todo tweak this
    }

    @Override
    protected double rampUp(double rateOfRamp, WavTone tone, double startingVol) {
        try {
            for (double volume = startingVol; volume < Short.MAX_VALUE; volume *= rateOfRamp) {
                if (!iModel.testing() || iModel.testPaused()) return -1;
                if (iModel.answered()) return volume;
                this.playWav(tone.newVol(volume));
            }
            return Short.MAX_VALUE;
        } finally {
            model.pauseAudio();
        }
    }
}
