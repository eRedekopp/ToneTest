package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
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
        this.results = new RampTestResults();
    }

    protected double rampUp(double rateOfRamp, float freq, double startingVol) {
        // ramp up, increasing volume slowly. Return when answered else return Short.MAX_VALUE (ie. the highest
        // amplitude possible in 16-bit PCM)
        for (double vol = startingVol; vol < Short.MAX_VALUE; vol *= rateOfRamp) {
            if (iModel.testPaused()) return -1;
            else if (iModel.answered()) return vol;
            else this.playSine(freq, vol, TIME_PER_VOL);
        }
        return Short.MAX_VALUE;
    }
}
