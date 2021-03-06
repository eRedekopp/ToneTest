package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import android.util.Log;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.ReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * A CalibrationTest that tests the user's ability to hear individual sine waves
 */
public class SineCalibratonTest extends CalibrationTest<FreqVolPair> {

    public SineCalibratonTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = DEFAULT_TEST_INFO;
    }

    @Override
    public String getTestTypeName() {
        return "sine-calibration";
    }

    @Override
    protected void playTone(FreqVolPair tone) {
        this.playSine(tone, DEFAULT_TONE_DURATION_MS);
    }

    @Override
    protected void configureTestTones(RampTestResults rampResults,
                                      FreqVolPair[] reduceResults,
                                      int nVolsPerFreq,
                                      int nTrialsPerVol) {

        ArrayList<FreqVolPair> allTones = new ArrayList<>();
        for (float freq : DEFAULT_CALIBRATION_FREQUENCIES) {
            // we boost the top volume estimates so that
            double topVolEst = Tone.getVolForFreq(rampResults.getResultsArray(), freq) * 1.2; // boost volumes
            double bottomVolEst = Tone.getVolForFreq(reduceResults, freq) * 1.2;
            for (double vol = bottomVolEst;
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / nVolsPerFreq) {
                allTones.add(new FreqVolPair(freq, vol));
            }
        }
        // fill CurrentVolumes with one freqvolpair for each individual tone that will be played in the test
        this.testTones = new ArrayList<>();
        for (int i = 0; i < nTrialsPerVol; i++) this.testTones.addAll(allTones);

        // sanity check
        if (this.testTones.size() != DEFAULT_CALIBRATION_FREQUENCIES.length * nVolsPerFreq * nTrialsPerVol)
            Log.e("SineCalibration", "Error configuring test tones: should have generated "
                    + nVolsPerFreq * nTrialsPerVol * DEFAULT_CALIBRATION_FREQUENCIES.length + " trials but generated "
                    + this.testTones.size());
    }
}
