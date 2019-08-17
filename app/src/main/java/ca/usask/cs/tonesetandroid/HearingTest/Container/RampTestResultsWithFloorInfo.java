package ca.usask.cs.tonesetandroid.HearingTest.Container;

import ca.usask.cs.tonesetandroid.HearingTest.Test.ReduceTest;

/**
 * A class for ramp test results that stores results from a reduce test and uses them to
 * calculate volume floors differently
 */
public class RampTestResultsWithFloorInfo extends RampTestResults {

    private ReduceTest.ReduceTestResults reduceResults;

    public RampTestResultsWithFloorInfo() {
        super();
    }

    public void setReduceResults(ReduceTest.ReduceTestResults reduceResults) {
        this.reduceResults = reduceResults;
    }

    @Override
    protected double getVolFloorEstimate(float freq) {
        return 0.0; // todo
    }
}
