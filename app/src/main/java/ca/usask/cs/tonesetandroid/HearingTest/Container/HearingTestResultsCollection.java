package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;

/**
 * A class for storing the results of multiple hearing tests
 */
public class HearingTestResultsCollection {

    ArrayList<HearingTestResults> resultsList;


    public HearingTestResultsCollection() {
        this.resultsList = new ArrayList<>();
    }

    public void addResults(HearingTestResults results) {
        resultsList.add(results);
    }

    /**
     * Get an estimate for the highest volume with P(heard) = 0 for the given frequency in the
     * given background noise
     *
     * @param freq The frequency whose volume floor is to be estimated
     * @param noiseType The background noise in which the frequency is played
     * @return An estimate for the highest volume with P(heard) = 0
     */
    public double getVolFloorEstimate(float freq, BackgroundNoiseType noiseType) {
        return 0.0; // todo
    }

    /**
     * Get an estimate for the lowest volume with P(heard) = 1 for the given frequency in the
     * given background noise
     *
     * @param freq The frequency whose volume ceiling is to be estimated
     * @param noiseType The background noise in which the frequency is payed
     * @return An estimate for the lowest volume with P(heard) = 1
     */
    public double getVolCeilingEstimate(float freq, BackgroundNoiseType noiseType) {
        return Double.MAX_VALUE; // todo
    }

}
