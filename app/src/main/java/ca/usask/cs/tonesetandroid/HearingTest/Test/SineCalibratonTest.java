package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SineCalibratonTest extends CalibrationTest<FreqVolPair> {

    private static final int TONE_DURATION_MS = 1500;

    public SineCalibratonTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testTypeName = "sine-calibration";
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, TONE_DURATION_MS);
    }

    @Override
    protected void configureTestTones(RampTest.RampTestResults rampResults,
                                   ReduceTest.ReduceTestResults reduceResults,
                                   int nVolsPerFreq,
                                   int nTrialsPerVol) {

        // todo boost volumes
        // todo selects too many pairs?
        ArrayList<FreqVolPair> allTones = new ArrayList<>();
        for (float freq : STANDARD_FREQUENCIES) {
            double topVolEst = Tone.getVolForFreq(rampResults.getResults(), freq);
            double bottomVolEst = Tone.getVolForFreq(reduceResults.getResults(), freq);
            for (double vol = bottomVolEst;
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / nVolsPerFreq) {
                allTones.add(new FreqVolPair(freq, vol));
            }
        }
        // fill CurrentVolumes with one freqvolpair for each individual tone that will be played in the test
        this.testTones = new ArrayList<>();
        for (int i = 0; i < nTrialsPerVol; i++) this.testTones.addAll(allTones);
    }
}
