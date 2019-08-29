package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;

public class EarconDifferentiationConfidenceTest extends WavConfidenceTest {

    public EarconDifferentiationConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
        super(results, noiseType);
    }

    @Override
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        // todo
    }
}
