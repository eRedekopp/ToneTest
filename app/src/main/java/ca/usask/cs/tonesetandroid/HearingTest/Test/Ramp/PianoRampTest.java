package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public class PianoRampTest extends WavRampTest {

    // todo make these different than confidence freqs
                                                      //        F4       C5       B5        G6        A7
    protected static final float[] DEFAULT_WAV_FREQUENCIES = {349.23f, 523.25f, 987.77f, 1567.98f, 3520.0f};

    public PianoRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);

        this.testInfo = DEFAULT_TEST_INFO;
        this.testTypeName = "piano-ramp";
        this.tones = new ArrayList<>();
        for (float freq : DEFAULT_WAV_FREQUENCIES) tones.add(new WavTone(freq, 0));
        this.position = tones.listIterator(0);
        this.startingVol = 3.0;
    }

    @Override
    protected float getRampRate1() {
        return 2.0f;
    }

    @Override
    protected float getRampRate2() {
        return 1.5f;
    }

    @Override
    protected double rampUp(double rateOfRamp, WavTone tone, double startingVol) {
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
