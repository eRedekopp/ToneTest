package ca.usask.cs.tonesetandroid.HearingTest.Container;

import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;


public class ConfidenceTestResults {

    // Map starting frequency of interval to all single test results of that frequency, separated by direction
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResultsUpward;
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResultsFlat;
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResultsDownward;

    private BackgroundNoiseType noiseType;  // the background noise type used in the test from which these results come

    public ConfidenceTestResults() {
        this.allResultsUpward = new HashMap<>();
        this.allResultsDownward = new HashMap<>();
        this.allResultsFlat = new HashMap<>();
    }

    public BackgroundNoiseType getNoiseType() {
        return noiseType;
    }

    public void setNoiseType(BackgroundNoiseType noiseType) {
        this.noiseType = noiseType;
    }

    /**
     * Add a new confidence test trial result to this container
     *
     * @param earcon An Earcon object storing the exact earcon that this trial was testing
     * @param correct Did the user answer correctly?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(Earcon earcon, boolean correct) {
        ConfidenceSingleTestResult cstr = this.getResultForEarcon(earcon);
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap = this.getMapForDirection(earcon.direction);

        // create new cstr and/or allResults entry if required
        if (cstr == null) {
            cstr = new ConfidenceSingleTestResult(earcon);
            try {
                resultMap.get(earcon.frequency).add(cstr);
            } catch (NullPointerException e) {
                List<ConfidenceSingleTestResult> newList = new ArrayList<>();
                newList.add(cstr);
                resultMap.put(earcon.frequency, newList);
            }
        }
        cstr.addTrial(correct);
    }

    /**
     * Return a mapping of each tested volume to the number of times the volume was heard for the given frequency
     *
     * @param freq The frequency whose test results are to be queried
     * @param direction An integer representing the direction of the earcon being queried (Earcon.DIRECTION_*)
     * @return A mapping of each volume tested at the given frequency to the number of times it was heard
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesHeardPerVolForInterval(float freq, int direction) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap = this.getMapForDirection(direction);

        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = resultMap.get(freq);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.earcon.volume, cstr.getTimesCorrect());
            }
        } catch (NullPointerException e) { if (! outMap.isEmpty()) throw e; }
        return outMap;
    }

    /**
     * Return a mapping of each tested volume to the number of times the volume was not heard for the given frequency
     *
     * @param freq The frequency whose test results are to be queried
     * @param direction An integer representing the direction of the earcon being queried (Earcon.DIRECTION_*)
     * @return A mapping of each volume tested at the given frequency to the number of times it was not heard
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesNotHeardPerVolForInterval(float freq, int direction) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap = this.getMapForDirection(direction);
        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = resultMap.get(freq);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.earcon.volume, cstr.getTimesIncorrect());
            }
        } catch (NullPointerException e) { if (! outMap.isEmpty()) throw e; }
        return outMap;
    }

    /**
     * Note: assumes that all directions tested the exact same frequencies
     *
     * @return An array of all frequencies present in these confidence test results
     */
    public float[] getTestedFrequencies() {
        float[] outArr = new float[this.allResultsUpward.size()];
        Set<Float> freqSet = this.allResultsUpward.keySet();
        int i = 0;
        for (Float f : freqSet) outArr[i++] = f;
        return outArr;
    }

    /**
     * @return True if there are no results stored in this container
     */
    public boolean isEmpty() {
        return this.allResultsUpward.isEmpty() && this.allResultsDownward.isEmpty() && this.allResultsFlat.isEmpty();
    }

    /**
     * Returns an array containing all the volumes tested at the given frequency and direction
     *
     * @param freq The frequency whose tested volumes are to be found
     * @param direction An integer representing the direction of the earcon being queried (Earcon.DIRECTION_*)
     * @return All volumes at which the given frequency was tested
     */
    @SuppressWarnings("ConstantConditions")
    public double[] getTestedVolsForInterval(float freq, int direction) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap = this.getMapForDirection(direction);

        try {
            List<ConfidenceSingleTestResult> resultList = resultMap.get(freq);
            double[] outArr = new double[resultList.size()];
            int i = 0;
            for (ConfidenceSingleTestResult cstr : resultList) outArr[i++] = cstr.earcon.volume;
            return outArr;
        } catch (NullPointerException e) {
            return new double[0];
        }
    }

    /**
     * @return A list containing all the Earcons present in these confidence results
     */
    public List<Earcon> getTestedEarcons() {
        ArrayList<Earcon> returnList = new ArrayList<>();
        for (List<ConfidenceSingleTestResult> lst : this.allResultsUpward.values()) {
            for (ConfidenceSingleTestResult cstr : lst) {
                returnList.add(cstr.earcon.clone());
            }
        }
        for (List<ConfidenceSingleTestResult> lst : this.allResultsDownward.values()) {
            for (ConfidenceSingleTestResult cstr : lst) {
                returnList.add(cstr.earcon.clone());
            }
        }
        for (List<ConfidenceSingleTestResult> lst : this.allResultsFlat.values()) {
            for (ConfidenceSingleTestResult cstr : lst) {
                returnList.add(cstr.earcon.clone());
            }
        }
        return returnList;
    }

    /**
     * Return the actual fraction of times that the tone was heard for the given frequency and direction at the given
     * volume in these confidence test results
     *
     * @throws IllegalArgumentException if the given frequency and volume were not tested or direction not recognized
     * @param freq The frequency whose results are to be queried
     * @param direction An integer representing the direction of the earcon being queried (Earcon.DIRECTION_*)
     * @param vol The volume whose results are to be queried
     * @return The fraction (0 <= result <= 1) of times that the tone was heard for the given freq-vol pair
     */
    public float getActualResultForEarcon(Earcon earcon) throws IllegalArgumentException {
        ConfidenceSingleTestResult result = this.getResultForEarcon(earcon);
        if (result == null) throw new IllegalArgumentException("Earcon not present in confidence results");
        return result.getActual();
    }

    /**
     * Get and return the ConfidenceSingleTestResult (CSTR) object for the given freq, vol, and direction or null if
     * does not exist
     *
     * @param freq The frequency of the desired CSTR
     * @param direction An integer representing the direction of the earcon in the desired CSTR (Earcon.DIRECTION_*)
     * @param vol The volume of the desired CSTR
     * @return The CSTR with the given frequency and volume, or null if none found
     */
    private ConfidenceSingleTestResult getResultForEarcon(Earcon earcon) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap = this.getMapForDirection(earcon.direction);

        List<ConfidenceSingleTestResult> resultList = resultMap.get(earcon.frequency);
        if (resultList == null) return null;
        for (ConfidenceSingleTestResult cstr : resultList)
            if (cstr.earcon.equals(earcon)) return cstr;
            else if (cstr.earcon.frequency != earcon.frequency)
                throw new AssertionError(
                        "Result with frequency " + cstr.earcon.frequency +
                        " found in list for frequency " + earcon.frequency);
        return null;
    }

    /**
     * Given an Earcon object and a model estimate for the probability that the listener will hear the tone, perform
     * analysis on the accuracy of the estimate given these confidence test results
     *
     * @param earcon An Earcon object storing the exact earcon to be analyzed. Must have been tested in this conf test
     * @param estimate The estimate for the likelihood that the listener will hear the earcon (from the model)
     * @return A StatsAnalysisResultsContainer containing the results of the analysis
     * @throws IllegalArgumentException If the given frequency and volume were not tested in the confidence test
     */
    public StatsAnalysisResultsContainer performAnalysis(Earcon earcon, double estimate)
                                                         throws IllegalArgumentException {
        ConfidenceSingleTestResult result = this.getResultForEarcon(earcon);
        if (result == null) throw new IllegalArgumentException("Interval not present in results");
        return new StatsAnalysisResultsContainer(result, estimate);
    }

    /**
     * Given an integer indicating a direction (Earcon.DIRECTION_*), return the HashMap which stores results for
     * trials in that same direction.
     *
     * @param direction An integer indicating an earcon direction (Earcon.DIRECTION_*)
     * @return The HashMap which stores results for trials in the given direction
     * @throws IllegalArgumentException If direction does not correspond to any known direction indicator in Earcon
     */
    public HashMap<Float, List<ConfidenceSingleTestResult>> getMapForDirection(int direction)
            throws IllegalArgumentException {
        switch (direction) {
            case Earcon.DIRECTION_DOWN:
                return this.allResultsDownward;
            case Earcon.DIRECTION_UP:
                return this.allResultsUpward;
            case Earcon.DIRECTION_FLAT:
                return this.allResultsFlat;
            default:
                throw new IllegalArgumentException("Unrecognized value for direction: " + direction);
        }
    }

    /**
     * A class for performing and storing the results of a statistical analysis on an estimate of the actual value
     */
    public class StatsAnalysisResultsContainer {

        public final Earcon earcon;

        public final float confProbEstimate;  // The probability estimate as determined by the confidence test

        public final double probEstimate;  // the probability estimate as determined by the model

        public final boolean estimatesSigDifferent; // Are the results statistically significantly different?

        public final float alpha; // the value of alpha used for this analysis

        public final float beta;  // the probability of a type II error in this analysis

        public final int critLow;   // = min({x | P(X < x) > alpha/2})

        public final int critHigh;  // = max({x | P(X > x) > alpha/2})

        /**
         * Create a new results container and perform all statistical analysis on the given data
         *
         * @param confResult The results of the confidence test at the desired frequency and volume
         * @param probEstimate The probability estimate found by the model (ie. based on the hearing test
         *                          results) for the frequency and volume tested in confResult
         */
        private StatsAnalysisResultsContainer(ConfidenceSingleTestResult confResult, double probEstimate) {
            // set constants
            this.earcon = confResult.earcon;
            this.confProbEstimate = confResult.getActual();
            this.probEstimate = probEstimate;
            this.alpha = 0.10f;

            // check for statistical significance
            // Null Hypothesis : confProb == probEstimate
            // Alt. Hypothesis : confProb != probEstimate
            BinomialDistribution binDist =
                    new BinomialDistribution(confResult.getTotalTrials(), probEstimate);

            // find critical region
            int     critBelow = -1,
                    critAbove = -1,
                    i = 0;
            while (critAbove == -1) {
                if (critBelow == -1 && binDist.cumulativeProbability(i) > alpha / 2) critBelow = i;
                else if (critBelow != -1 && 1 - binDist.cumulativeProbability(i) < alpha / 2) critAbove = i - 1;
                i++;
            }
            this.critLow = critBelow;
            this.critHigh = critAbove;

            // Check if probEstimate within rejection region
            int x = (int) Math.round(probEstimate * confResult.getTotalTrials());
            this.estimatesSigDifferent = x < critBelow || x > critAbove;

            // calculate beta assuming confProbEstimate is correct
            // beta = P(x outside rejection region | confProbEstimate is true)
            binDist = new BinomialDistribution(confResult.getTotalTrials(), confProbEstimate);
            this.beta = (float) (binDist.cumulativeProbability(critAbove) - binDist.cumulativeProbability(critBelow));
        }
    }

    /**
     * A class to store the responses for a single interval + volume in a confidence test
     *
     * @author redekopp
     */
    private class ConfidenceSingleTestResult {

        final Earcon earcon;

        private int nCorrect;
        private int nIncorrect;

        public ConfidenceSingleTestResult(Earcon earcon) {
            this.nCorrect = 0;
            this.nIncorrect = 0;
            this.earcon = earcon;
        }

        /**
         * Add a new trial to this single test result
         * @param correct Did the user correctly answer in this trial?
         */
        public void addTrial(boolean correct) {
            if (correct) this.nCorrect++;
            else this.nIncorrect++;
        }

        /**
         * @return The fraction [0, 1] of correct answers
         */
        public float getActual() {
            return (float) this.nCorrect / (float) (this.nIncorrect + this.nCorrect);
        }

        /**
         * @return The number of correct answers for this interval and volume
         */
        public int getTimesCorrect() {
            return this.nCorrect;
        }

        /**
         * @return The number of incorrect answers for this interval and volume
         */
        public int getTimesIncorrect() {
            return  this.nIncorrect;
        }

        /**
         * @return The total number of trials at this interval and volume
         */
        public int getTotalTrials() {
            return this.nCorrect + this.nIncorrect;
        }
    }
}
