package ca.usask.cs.tonesetandroid;

import android.util.Log;

import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class ConfidenceTestResultsContainer {

    // Map starting frequency of interval to all single test results of that frequency, separated by direction
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResultsUpward;
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResultsDownward;

    public ConfidenceTestResultsContainer() {
        this.allResultsUpward = new HashMap<>();
        this.allResultsDownward = new HashMap<>();
    }

    /**
     * Add a new confidence test trial result to this container
     *
     * @param freq1 The first frequency of the interval in the trial
     * @param upward Is the interval upward?
     * @param vol The volume of the trial
     * @param correct Did the user answer correctly?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(Interval interval, boolean correct) {
        ConfidenceSingleTestResult cstr = this.getResultForInterval(interval);
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap =
                interval.isUpward ? this.allResultsUpward : this.allResultsDownward;

        // create new cstr and/or allResults entry if required
        if (cstr == null) {
            cstr = new ConfidenceSingleTestResult(interval);
            try {
                resultMap.get(interval.freq1).add(cstr);
            } catch (NullPointerException e) {
                List<ConfidenceSingleTestResult> newList = new ArrayList<>();
                newList.add(cstr);
                resultMap.put(interval.freq1, newList);
            }
        }
        cstr.addTrial(correct);
    }

    /**
     * Return a mapping of each tested volume to the number of times the volume was heard for the given frequency
     *
     * @param freq The frequency whose test results are to be queried
     * @return A mapping of each volume tested at the given frequency to the number of times it was heard
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesHeardPerVolForInterval(float freq1, boolean upward) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;

        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = resultMap.get(freq1);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.vol, cstr.getTimesCorrect());
            }
        } catch (NullPointerException e) { if (! outMap.isEmpty()) throw e; }
        return outMap;
    }

    /**
     * Return a mapping of each tested volume to the number of times the volume was not heard for the given frequency
     *
     * @param freq The frequency whose test results are to be queried
     * @return A mapping of each volume tested at the given frequency to the number of times it was not heard
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesNotHeardPerVolForInterval(float freq1, boolean upward) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;
        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = resultMap.get(freq1);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.vol, cstr.getTimesIncorrect());
            }
        } catch (NullPointerException e) { if (! outMap.isEmpty()) throw e; }
        return outMap;
    }

    /**
     * Note: assumes that frequencies tested upward were also tested downward, and vice versa
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
        return this.allResultsUpward.isEmpty() && this.allResultsDownward.isEmpty();
    }

    /**
     * Returns an array containing all the volumes tested at the given frequency
     *
     * @param freq The frequency whose tested volumes are to be found
     * @return All volumes at which the given frequency was tested
     */
    @SuppressWarnings("ConstantConditions")
    public double[] getTestedVolsForInterval(float freq1, boolean upward) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;

        try {
            List<ConfidenceSingleTestResult> resultList = resultMap.get(freq1);
            double[] outArr = new double[resultList.size()];
            int i = 0;
            for (ConfidenceSingleTestResult cstr : resultList) outArr[i++] = cstr.vol;
            return outArr;
        } catch (NullPointerException e) {
            return new double[0];
        }
    }

    /**
     * @return A list containing all the frequency-volume pairs present in these confidence results
     */
    public List<Interval> getTestedIntervals() {
        ArrayList<Interval> returnList = new ArrayList<>();
        for (List<ConfidenceSingleTestResult> lst : this.allResultsUpward.values()) {
            for (ConfidenceSingleTestResult cstr : lst) {
                returnList.add(new Interval(cstr.freq1, cstr.freq2, cstr.vol));
            }
        }
        for (List<ConfidenceSingleTestResult> lst : this.allResultsDownward.values()) {
            for (ConfidenceSingleTestResult cstr : lst) {
                returnList.add(new Interval(cstr.freq1, cstr.freq2, cstr.vol));
            }
        }
        return returnList;
    }

    /**
     * Return the actual fraction of times that the tone was heard for the given frequency at the given volume in
     * these confidence test results
     *
     * @throws IllegalArgumentException if the given frequency and volume were not tested
     * @param freq The frequency whose results are to be queried
     * @param vol The volume whose results are to be queried
     * @return The fraction (0 <= result <= 1) of times that the tone was heard for the given freq-vol pair
     */
    public float getActualResultForInterval(float freq1, boolean upward, double vol) throws IllegalArgumentException {
        ConfidenceSingleTestResult result = this.getResultForInterval(freq1, upward, vol);
        if (result == null) throw new IllegalArgumentException("frequency-volume pair not present in results");
        return result.getActual();
    }

    public float getActualResultForInterval(Interval interval) {
        return getActualResultForInterval(interval.freq1, interval.isUpward, interval.vol);
    }

    /**
     * Get and return the ConfidenceSingleTestResult (CSTR) object for the given freq and vol, or null if does not exist
     *
     * @param freq The frequency of the desired CSTR
     * @param vol The volume of the desired CSTR
     * @return The CSTR with the given frequency and volume, or null if none found
     */
    private ConfidenceSingleTestResult getResultForInterval(float freq1, boolean upward, double vol) {
        HashMap<Float, List<ConfidenceSingleTestResult>> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;

        List<ConfidenceSingleTestResult> resultList = resultMap.get(freq1);
        if (resultList == null) return null;
        for (ConfidenceSingleTestResult cstr : resultList)
            if (cstr.vol == vol && cstr.freq1 == freq1) return cstr;
            else if (cstr.freq1 != freq1)
                throw new AssertionError("Result with frequency "+cstr.freq1+" found in list for frequency "+freq1);
        return null;
    }

    private ConfidenceSingleTestResult getResultForInterval(Interval interval) {
        return getResultForInterval(interval.freq1, interval.isUpward, interval.vol);
    }

    /**
     * Given a frequency, a volume, and an estimate for the probability that the listener will hear the tone, perform
     * analysis on the accuracy of the estimate given these confidence test results
     *
     * @param freq The frequency of the tone whose probability is being estimated
     * @param vol The volume of the tone whose probability is being estimated
     * @param estimate The estimate for the likelihood that the listener will hear a tone at the given freq and vol
     * @return A StatsAnalysisResultsContainer containing the results of the analysis
     * @throws IllegalArgumentException If the given frequency and volume were not tested in the confidence test
     */
    public StatsAnalysisResultsContainer performAnalysis(Interval interval, float estimate)
                                                         throws IllegalArgumentException {
        ConfidenceSingleTestResult result = this.getResultForInterval(interval);
        if (result == null) throw new IllegalArgumentException("Interval not present in results");
        return new StatsAnalysisResultsContainer(result, estimate);
    }

    /**
     * A class for performing and storing the results of a statistical analysis on an estimate of the actual value
     */
    public class StatsAnalysisResultsContainer {

        public final float freq1; // the first frequency of the interval being analyzed

        public final float freq2; // the second frequency of the interval being analyzed

        public final boolean upward; // is the interval upward?

        public final double vol;  // the volume of the interval being analyzed

        public final float confProbEstimate;  // The probability estimate as determined by the confidence test

        public final float probEstimate;  // the probability estimate as determined by the model

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
        private StatsAnalysisResultsContainer(ConfidenceSingleTestResult confResult, float probEstimate) {
            // set constants
            this.freq1 = confResult.freq1;
            this.freq2 = confResult.freq2;
            this.vol = confResult.vol;
            this.confProbEstimate = confResult.getActual();
            this.probEstimate = probEstimate;
            this.alpha = 0.10f;
            this.upward = confResult.upward;

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
            int x = Math.round(probEstimate * confResult.getTotalTrials());
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

        final float freq1;
        final float freq2;
        final boolean upward;
        final double vol;

        private int nCorrect;
        private int nIncorrect;

        public ConfidenceSingleTestResult(Interval interval) {
            this.nCorrect = 0;
            this.nIncorrect = 0;
            this.freq1 = interval.freq1;
            this.freq2 = interval.freq2;
            this.vol = interval.vol;
            this.upward = interval.isUpward;
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
