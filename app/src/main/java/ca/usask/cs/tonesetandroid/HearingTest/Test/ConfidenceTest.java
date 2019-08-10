package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Container.ConfidenceTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;

public abstract class ConfidenceTest extends HearingTest {

    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_FLAT = 3;
    public static final float[] STANDARD_FREQUENCIES = {220, 440, 880, 1760, 3520};

    protected CalibrationTestResults calibResults;

    /**
     * Play a sample of all testable tones of the given direction
     * @param direction An integer indicating the direction of samples to be played (ConfidenceTest.DIRECTION_*)
     */
    public abstract void playSamples(int direction);

}
