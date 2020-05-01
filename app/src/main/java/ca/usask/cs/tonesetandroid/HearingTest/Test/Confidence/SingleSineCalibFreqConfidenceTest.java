package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.CalibrationTest;

/**
 *  A SingleSineConfidenceTest that tests DEFAULT_CALIBRATION_FREQUENCIES
 */
public class SingleSineCalibFreqConfidenceTest extends SingleSineConfidenceTest {

    public SingleSineCalibFreqConfidenceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }

    @Override
    public String getTestTypeName() {
        return "sine-single-tone-conf-calib-freqs";
    }

    @Override
    public void initialize() {
        this.initialize(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, DEFAULT_CALIBRATION_FREQUENCIES);
    }
}
