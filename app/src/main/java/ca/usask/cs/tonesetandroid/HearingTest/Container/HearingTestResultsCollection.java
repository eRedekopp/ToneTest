package ca.usask.cs.tonesetandroid.HearingTest.Container;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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

    public boolean isEmpty() {
        return this.resultsList.isEmpty();
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
        return getPreferredResults(noiseType).getVolFloorEstimate(freq);
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
        return getPreferredResults(noiseType).getVolCeilingEstimate(freq);
    }

    /**
     * Return the HearingTestResults from resultsList whose background noise is closest to
     * noiseTypeID. CalibrationTestResults are preferred over RampTestResultsWithFloorInfo, which
     * are preferred over RampTestResults
     *
     * @param noiseType The background noise type to which the selected HearingTestResults' noise
     *                  type should be closest
     * @return The HearingTestResults with background noise type closest to noiseTypeID
     */
    private HearingTestResults getPreferredResults(BackgroundNoiseType noiseType) {

        if (this.resultsList.isEmpty()) throw new IllegalStateException("No results stored"); // error if none stored
        if (this.resultsList.size() == 1) return this.resultsList.get(0); // if only one result, return it

        ArrayList<HearingTestResults> resultsWithSameNoiseID = new ArrayList<>();

        // find all results with same noise type
        for (HearingTestResults results : this.resultsList)
            if (results.getNoiseType().noiseTypeID == noiseType.noiseTypeID)
                resultsWithSameNoiseID.add(results);

        if (resultsWithSameNoiseID.isEmpty()) { // no results with same noise type
            // return results with closest volume, regardless of type
            return sortByProximityToVol(this.resultsList, noiseType.volume).get(0);
        } else {
            // if only one found, return it
            if (resultsWithSameNoiseID.size() == 1) return resultsWithSameNoiseID.get(0);
            // else return closest with highest priority
            else return sortByProximityToVol(this.resultsList, noiseType.volume).get(0);
        }
    }

    /**
     * Return a list of HearingTestResults sorted by their BackgroundNoiseType's volume's proximity to the given
     * volume (closest to farthest), with CalibrationTestResults preferred over RampTestResultsWithFloorInfo, which
     * are preferred over RampTestResults (if volumes are equal).
     */
    @NonNull
    private static List<HearingTestResults> sortByProximityToVol(List<HearingTestResults> resultsList,
                                                                 double vol) {

        if (resultsList.isEmpty()) return new ArrayList<>();
        else if (resultsList.size() == 1) return resultsList;

        ArrayList<HearingTestResults> outputList = new ArrayList<>();

        // add all subsequent items in appropriate spot
        for (HearingTestResults results : resultsList) insert(outputList, results, vol);

        return outputList;
    }

    /**
     * Insert the the HearingTestResults into resultsList at the appropriate place given the type of the test and its
     * proximity to vol (for use with sortByProximityToVol)
     *
     * @param resultsList A list of HearingTestResults
     * @param toInsert The HearingTestResults to be inserted into the list
     * @param vol The volume to which toInsert's volume is to be compared
     */
    private static void insert(List<HearingTestResults> resultsList, HearingTestResults toInsert, double vol) {
        double inputVolDistance = Math.abs(toInsert.getNoiseType().volume - vol);
        ListIterator<HearingTestResults> resultsListIter = resultsList.listIterator();

        // move forward until resultsListIter.next() is farther than toInsert, or end of list reached
        while (resultsListIter.hasNext()) {
            HearingTestResults nextResult = resultsListIter.next();
            double curVolDistance = Math.abs(nextResult.getNoiseType().volume - vol);
            if (inputVolDistance < curVolDistance) {
                resultsListIter.add(toInsert);
                return;
            } else if (inputVolDistance == curVolDistance) {
                // input at appropriate spot for priority level
                while (getPriority(nextResult) > getPriority(toInsert)) nextResult = resultsListIter.next();
                resultsListIter.add(toInsert);
                return;
            }
        }
        resultsList.add(toInsert); // toInsert is farthest in list if not returned yet
    }

    /**
     * Ranks the HearingTestResults by its priority in sortByProximityToVol, based on its type
     */
    private static int getPriority(HearingTestResults results) {
        if (results instanceof CalibrationTestResults) return 2;
        if (results instanceof RampTestResultsWithFloorInfo) return 1;
        if (results instanceof RampTestResults) return 0;
        throw new RuntimeException("Unknown HearingTestResults type");
    }
}
