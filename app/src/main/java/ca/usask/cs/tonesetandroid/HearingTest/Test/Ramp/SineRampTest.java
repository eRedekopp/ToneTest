package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.Model;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SineRampTest extends RampTest<FreqVolPair> {

    protected static final int TIME_PER_VOL_MS = 50;

    public SineRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);

        this.testInfo = DEFAULT_TEST_INFO;
        this.testTypeName = "sine-ramp";
        this.tones = new ArrayList<>();
        for (float freq : DEFAULT_CALIBRATION_FREQUENCIES) tones.add(new FreqVolPair(freq, 0));
        this.position = tones.listIterator(0);
    }

    @Override
    protected float getRampRate1() {
        return 1.05f;
    }

    @Override
    protected float getRampRate2() {
        return 1.025f;
    }

    @Override
    protected double rampUp(double rateOfRamp, FreqVolPair tone, double startingVol) {

        byte[] buf = Model.buf;

        model.enforceMaxVolume();
        model.startAudio();

        try {
            for (double volume = startingVol; volume < Short.MAX_VALUE; volume *= rateOfRamp) {  // Short.MAX_VALUE =
                if (!iModel.testing() || iModel.testPaused()) return -1;                         // max vol for pcm16

                if (iModel.answered()) {
                    return volume;
                }

                for (int i = 0; i < TIME_PER_VOL_MS * (float) 44100 / 1000; i++) { //1000 ms in 1 second
                    float period = (float) Model.OUTPUT_SAMPLE_RATE / tone.freq();
                    double angle = 2 * i / (period) * Math.PI;
                    short a = (short) (Math.sin(angle) * volume);
                    buf[0] = (byte) (a & 0xFF); //write 8bits ________WWWWWWWW out of 16
                    buf[1] = (byte) (a >> 8); //write 8bits WWWWWWWW________ out of 16
                    model.lineOut.write(buf, 0, 2);
                }
            }
            return Short.MAX_VALUE;
        } finally {
            model.pauseAudio();
        }
    }
}