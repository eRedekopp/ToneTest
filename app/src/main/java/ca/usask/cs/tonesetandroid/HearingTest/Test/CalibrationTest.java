package ca.usask.cs.tonesetandroid.HearingTest.Test;

import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;

public abstract class CalibrationTest extends HearingTest {

    private RampTest.RampTestResults rampResults;




    @Override
    protected void run() {

    }

    @Override
    boolean isComplete() {
        return false;
    }

    @Override
    protected String getLineEnd(SingleTrialResult result) {
        return null;
    }
}
