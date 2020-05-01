package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;

/**
 * A ConfidenceTest that tests the user's ability to differentiate between "upward" and "downward" WavTone "earcons"
 */
public class EarconDifferentiationConfidenceTest extends WavConfidenceTest {

    public EarconDifferentiationConfidenceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.testInfo = MelodySineConfidenceTest.TEST_INFO; // same test info as melody-sine
    }

    @Override
    public String getTestTypeName() {
        return "earcon-differentiation-confidence";
    }

    @Override
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        // Not implemented
    }

    @Override
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_UP, ANSWER_DOWN, ANSWER_FLAT};
    }
}
