package ca.usask.cs.tonesetandroid.HearingTest.Test;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;

/**
 *  A SingleSineConfidenceTest that tests CalibrationTest.DEFAULT_CALIBRATION_FREQUENCIES instead of ConfidenceTest.DEFAULT_FREQUENCIES
 */
public class SingleSineCalibFreqConfidenceTest extends SingleSineConfidenceTest {

    public SingleSineCalibFreqConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(calibResults, noiseType);
        this.testTypeName = "sine-single-tone-conf-calib-freqs";
    }

    @Override
    public void initialize() {
        this.initialize(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, CalibrationTest.DEFAULT_CALIBRATION_FREQUENCIES);
    }
}
