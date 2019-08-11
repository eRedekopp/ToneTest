package ca.usask.cs.tonesetandroid.HearingTest.Test;


import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SineCalibratonTest extends CalibrationTest<FreqVolPair> {

    private static final int TONE_DURATION_MS = 1500;

    public SineCalibratonTest(RampTest.RampTestResults rampResults, ReduceTest.ReduceTestResults reduceResults,
                              BackgroundNoiseType noiseType) {
        super(rampResults, reduceResults, noiseType);
    }

    @Override
    protected boolean isComplete() {
        return false;
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, TONE_DURATION_MS);
    }

    @Override
    protected void configureTestTones(int nVolsPerFreq, int nTrialsPerVol) {
        // todo boost volumes
        ArrayList<FreqVolPair> allTones = new ArrayList<>();
        for (float freq : STANDARD_FREQUENCIES) {
            double bottomVolEst = Tone.getVolForFreq(this.rampResults.getResults(), freq);
            double topVolEst = Tone.getVolForFreq(this.reduceResults.getResults(), freq);
            for (double vol = bottomVolEst;
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / nVolsPerFreq) {
                allTones.add(new FreqVolPair(freq, vol));
            }
        }
        // fill CurrentVolumes with one freqvolpair for each individual tone that will be played in the test
        for (int i = 0; i < nTrialsPerVol; i++) this.testTones.addAll(allTones);
        Collections.shuffle(this.testTones);
    }
}
