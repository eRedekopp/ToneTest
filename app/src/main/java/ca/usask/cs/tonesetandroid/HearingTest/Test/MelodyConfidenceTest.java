package ca.usask.cs.tonesetandroid.HearingTest.Test;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;

public class MelodyConfidenceTest extends ConfidenceTest<Melody> {

    public MelodyConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(calibResults, noiseType);
    }

    @Override
    protected void configureTestPairs(int trialsPerTone, int volsPerFreq, float[] frequencies) {

    }

    @Override
    public Runnable sampleTones() {
        return null;
    }

    @Override
    protected void playTone(Melody tone) {

    }

    @Override
    protected boolean wasCorrect() {
        return false;
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[0];
    }
}
