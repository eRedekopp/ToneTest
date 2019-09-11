package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;

/**
 * A confidence test that tests the participant's ability to hear an earcon, without taking direction into account
 */
public class EarconAudibilityConfidenceTest extends WavConfidenceTest {

    public EarconAudibilityConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
        super(results, noiseType);
        this.testTypeName = "earcon-audibility-confidence";
        this.testInfo = DEFAULT_TEST_INFO;
    }

    @Override
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        // todo
    }

    @Override
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_HEARD};
    }
}
