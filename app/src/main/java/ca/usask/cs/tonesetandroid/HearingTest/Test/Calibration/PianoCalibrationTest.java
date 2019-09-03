package ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.WavCalibrationTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.ReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * A CalibrationTest that tests the user's ability to hear individual piano tones
 */
public class PianoCalibrationTest extends WavCalibrationTest {

                                          //        F4       C5       B5        G6        A7
    protected static final float[] FREQUENCIES = {349.23f, 523.25f, 987.77f, 1567.98f, 3520.0f};    // todo make closer
                                                                                                    // to defaults

    public PianoCalibrationTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testTypeName = "single-piano-calibration";
    }

    @Override
    protected void configureTestTones(RampTestResults rampResults,
                                      FreqVolPair[] reduceResults,
                                      int nVolsPerFreq,
                                      int nTrialsPerVol) {

        ArrayList<WavTone> allTones = new ArrayList<>();

        for (float freq : FREQUENCIES) {
            double topVolEst = Tone.getVolForFreq(rampResults.getResultsArray(), freq) * 1.2; // boost volumes
            double bottomVolEst = Tone.getVolForFreq(reduceResults, freq) * 1.2;
            for (double vol = bottomVolEst;
                 vol < topVolEst;
                 vol += (topVolEst - bottomVolEst) / nVolsPerFreq) {
                allTones.add(new WavTone(freq, vol));
            }
        }

        this.testTones = new ArrayList<>();
        for (int i = 0; i < nTrialsPerVol; i++) testTones.addAll(allTones);
    }
}
