package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.Model;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

public class SineRampTest extends RampTest<FreqVolPair> {

    public SineRampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo =
                "In this phase of the test, tones will play quietly and slowly get louder. Please press the \"Heard " +
                        "Tone\" button as soon as the tone becomes audible";

        this.testTypeName = "sine-ramp";
        this.freqs = new ArrayList<>();
        for (float freq : CALIB_FREQS) freqs.add(freq);
        this.position = freqs.listIterator(0);
    }

    protected double rampUp(double rateOfRamp, float freq, double startingVol) {

        // todo sometimes this method writes crackling audio to line out for no discernible reason, then stops. Does
        //  not happen with playSine()

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
                    float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
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