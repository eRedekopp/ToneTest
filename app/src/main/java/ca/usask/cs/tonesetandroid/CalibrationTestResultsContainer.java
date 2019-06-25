package ca.usask.cs.tonesetandroid;

/**
 * A HearingTestResultsContainer which also contains data about the background noise at the time of the test
 */
public class CalibrationTestResultsContainer extends HearingTestResultsContainer {

    private FreqVolPair[] periodogram;

    public CalibrationTestResultsContainer() {
        super();
    }

    public void setPeriodogram(FreqVolPair[] periodogram) {
        this.periodogram = periodogram;
    }

    /**
     * Generates a set of estimated HearingTestResults for the frequency space represented by the given periodogram,
     * based on the results stored in this model
     *
     * @param periodogram The estimated Power Spectral Density of the space for which the estimates are to be generated
     * @return A HearingTestResultsContainer populated with estimated results for the new space
     */
    public HearingTestResultsContainer generateNewResults(FreqVolPair[] periodogram) {
        return null;
    }

//    protected class AutoTestSingleFreqResult extends HearingTestResultsContainer.HearingTestSingleFreqResult {
//
//        public AutoTestSingleFreqResult() {
//
//        }
//
//    }

}
