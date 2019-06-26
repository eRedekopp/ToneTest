package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class HearingTestResultsContainer {

    // Keyed by Interval.freq1
    public HashMap<Float, HearingTestSingleIntervalResult> allResultsUpward;   // for upward intervals

    public HashMap<Float, HearingTestSingleIntervalResult> allResultsDownward; // for downward intervals


    public HearingTestResultsContainer() {
        allResultsUpward = new HashMap<>();
        allResultsDownward = new HashMap<>();
    }

    /**
     * Add a new calibration test result to this container
     *
     * @param freq The frequency of the trial
     * @param vol  The volume of the trial
     * @param heard Was the trial heard?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(Interval interval, boolean heard) {
        HashMap<Float, HearingTestSingleIntervalResult> mapToUpdate =
                interval.isUpward ? allResultsUpward : allResultsDownward;
        try {
            mapToUpdate.get(interval.freq1).addResult(interval.vol, heard);
        } catch (NullPointerException e) {
            HearingTestSingleIntervalResult r = new HearingTestSingleIntervalResult(interval.freq1, interval.freq2);
            r.addResult(interval.vol, heard);
            mapToUpdate.put(interval.freq1, r);
        }
    }

    /**
     * @return true if there are no results stored in this container, else false
     */
    public boolean isEmpty() {
        return allResultsUpward.isEmpty() && allResultsDownward.isEmpty();
    }

    /**
     * Given the first frequency direction, and volume of an interval, determine the probability that the user will
     * correctly hear the direction of the interval
     *
     * @param freq1 the starting frequency of the interval
     * @param upward Is the interval upward?
     * @param vol The volume of the tones in the interval
     * @return The probability that the user will correctly hear the direction of the interval
     */
    @SuppressWarnings("ConstantConditions")
    public float getProbOfCorrectAnswer(float freq1, boolean upward, double vol) {

        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;

        // find frequencies tested just above and below freq
        float freqAbove = findNearestAbove(freq1, this.getTestedFreqs());
        float freqBelow = findNearestBelow(freq1, this.getTestedFreqs());

        // if freq is higher than the highest or lower than the lowest, just return the probability of the nearest
        if (freqAbove == -1) return resultMap.get(freqBelow).getProbOfHearing(vol);
        if (freqBelow == -1) return resultMap.get(freqAbove).getProbOfHearing(vol);

        // find the probabilities of each of these frequencies
        float probAbove = resultMap.get(freqAbove).getProbOfHearing(vol);
        float probBelow = resultMap.get(freqAbove).getProbOfHearing(vol);

        // how far of the way between freqBelow and freqAbove is fvp.freq?
        float pctBetween = (freq1 - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
    }

    public float getProbOfCorrectAnswer(Interval interval) {
        return getProbOfCorrectAnswer(interval.freq1, interval.isUpward, interval.vol);
    }

    /**
     * Given the starting note of the interval, its direction, its volume, and a subset of the tested frequencies,
     * determine the probability that the user will correctly hear the direction of the interval based only on the
     * given subset of frequencies
     *
     * @param freq1 The starting frequency of the interval
     * @param upward Is the interval upward?
     * @param vol The volume of the tones in the interval
     * @param subset A subset of the tested volumes, to be used to generate the estimate
     * @return An estimate of the probability that the user will correctly hear the direction of the interval
     * @throws IllegalArgumentException If the given subset is not a subset of the tested frequencies
     */
    @SuppressWarnings("ConstantConditions")
    public float getProbOfCorrectAnswer(float freq1, boolean upward, double vol, float[] subset)
            throws IllegalArgumentException {

        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;

        Float[] subsetAsObj = new Float[subset.length];
        for (int i = 0; i < subset.length; i++)
            if (! this.freqTested(subset[i], upward))
                throw new IllegalArgumentException("Subset argument must be a subset of tested frequencies");
            else subsetAsObj[i] = subset[i];

        // find subset frequencies just above and below tested frequencies
        float freqAbove = findNearestAbove(freq1, subsetAsObj);
        float freqBelow = findNearestBelow(freq1, subsetAsObj);

        // find the probabilities of each of these frequencies
        float probAbove = resultMap.get(freqAbove).getProbOfHearing(vol);
        float probBelow = resultMap.get(freqAbove).getProbOfHearing(vol);

        // how far of the way between freqBelow and freqAbove is fvp.freq?
        float pctBetween = (freq1 - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
    }

    /**
     * Given a starting frequency and a direction, return a mapping of volumes at which that interval was tested to the
     * number of times the user answered correctly at that volume
     *
     * Note: getTimesCorrPerVolForInterval().keySet() doesn't necessarily contain all tested volumes. If never answered
     * correctly, a volume will not be present
     *
     * @param freq1 The starting frequency of the interval
     * @param upward Is the interval upward?
     * @return A mapping of volumes at which the interval was tested to the number of correct answers the user gave at
     * that volume
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesCorrPerVolForInterval(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;
        try {
            return resultMap.get(freq1).getTimesCorrPerVol();
        } catch (NullPointerException e) {
            return new HashMap<>();  // empty map if freq not tested
        }
    }

    /**
     * Given a starting frequency and a direction, return a mapping of volumes at which that interval was tested to the
     * number of times the user answered incorrectly at that volume
     *
     * Note: getTimesIncorrPerVolForInterval().keySet() doesn't necessarily contain all tested volumes. If never
     * answered incorrectly, a volume will not be present
     *
     * @param freq1 The starting frequency of the interval
     * @param upward Is the interval upward?
     * @return A mapping of volumes at which the interval was tested to the number of incorrect answers the user gave at
     * that volume
     */
    @SuppressWarnings("ConstantConditions")
    public HashMap<Double, Integer> getTimesIncorrPerVolForFreq(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;
        try {
            return resultMap.get(freq1).getTimesIncorrPerVol();
        } catch (NullPointerException e) {
            return new HashMap<>();  // empty map if freq not tested
        }
    }

    /**
     * Returns a list of all volumes tested for the given starting note and interval direction
     *
     * @param freq1 The starting frequency of the interval whose tested volumes are to be returned
     * @param upward Is the interval upward?
     * @return A list of all volumes tested for the given frequency and direction
     */
    @SuppressWarnings("ConstantConditions")
    public Collection<Double> getTestedVolumesForFreq(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;
        try {
            return resultMap.get(freq1).getVolumes();
        } catch (NullPointerException e) {
            return new ArrayList<>();  // empty list if frequency not tested
        }
    }

    /**
     * Note: assumes that all starting frequencies were tested both upward and downward
     * @return An array of all the first frequencies tested in these results (ie. freq1 of the Interval)
     */
    public Float[] getTestedFreqs() {
        Float[] outArr = new Float[allResultsUpward.size()];
        allResultsUpward.keySet().toArray(outArr);
        return outArr;
    }

    /**
     * Return true if the frequency and direction was tested, else false
     *
     * @param freq1 The frequency to be queried
     * @param upward Is the interval upward?
     * @return True if the frequency was tested, else false
     */
    public boolean freqTested(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : this.allResultsDownward;
        for (Float f : resultMap.keySet()) if (f == freq1) return true;
        return false;
    }

    /**
     * Returns an estimate of the highest volume which has a 0% chance of being correctly differentiated for the given
     * starting frequency and direction
     *
     * @param freq1 The frequency whose volume floor is to be estimated
     * @param upward Is the interval upward?
     * @return An estimate of the volume floor for the given frequency
     */
    @SuppressWarnings("ConstantConditions")
    public double getVolFloorEstimateForFreq(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : allResultsDownward;

        if (this.freqTested(freq1, upward)) return resultMap.get(freq1).getVolFloor();

        float nearestBelow = findNearestBelow(freq1, this.getTestedFreqs());
        float nearestAbove = findNearestAbove(freq1, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return resultMap.get(this.getNearestTestedFreq(freq1)).getVolFloor();

        float pctBetween = (freq1 - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = resultMap.get(nearestBelow).getVolFloor();
        double floorAbove = resultMap.get(nearestAbove).getVolFloor();

        return floorBelow + (floorAbove - floorBelow) * pctBetween;
    }

    /**
     * Returns an estimate of the lowest volume which has a 100% percent chance of being correctly differentiated for
     * the given frequency
     *
     * @param freq1 The starting frequency of the interval whose volume ceiling is to be estimated
     * @param upward Is the interval upward?
     * @return An estimate of the volume ceiling for the given frequency
     */
    @SuppressWarnings("ConstantConditions")
    public double getVolCeilingEstimateForFreq(float freq1, boolean upward) {
        HashMap<Float, HearingTestSingleIntervalResult> resultMap =
                upward ? this.allResultsUpward : allResultsDownward;

        if (this.freqTested(freq1, upward)) return resultMap.get(freq1).getVolCeiling();

        float nearestBelow = findNearestBelow(freq1, this.getTestedFreqs());
        float nearestAbove = findNearestAbove(freq1, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return resultMap.get(this.getNearestTestedFreq(freq1)).getVolCeiling();

        float pctBetween = (freq1 - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = resultMap.get(nearestBelow).getVolCeiling();
        double floorAbove = resultMap.get(nearestAbove).getVolCeiling();

        return floorBelow + (floorAbove - floorBelow) * pctBetween;
    }

    /**
     * Return the frequency tested in the hearing test which is closest to the given frequency
     *
     * @param nearestTo The number that the returned value is to be nearest to
     * @return The tested frequency nearest to nearestTo
     */
    public float getNearestTestedFreq(float nearestTo) {
        float curClosest = -1;
        float minDiff = Float.MAX_VALUE;
        for (float f : this.getTestedFreqs())
            if (Math.abs(f - nearestTo) < minDiff) {
                curClosest = f;
                minDiff = Math.abs(f - nearestTo);
            }
        if (curClosest == -1) throw new RuntimeException("Found unexpected value -1");
        return curClosest;
    }

    /**
     * Return a new HearingTestResultsContainer with the same results as this one, but only containing the first n
     * results for each interval (ie. as though Model.NUMBER_OF_TESTS_PER_VOL == n)
     *
     * @param n The number of trials per freq-vol pair in the new container
     * @return A new container containing a subset of this one's results
     * @throws IllegalArgumentException If n is greater than the number of trials performed in this test
     */
    public HearingTestResultsContainer getSubsetResults(int n) throws IllegalArgumentException {
        if (n > this.getNumOfTrials())
            throw new IllegalArgumentException(
                    "n = " + n + " is larger than the actual sample size = " + this.getNumOfTrials());
        HearingTestResultsContainer newContainer = new HearingTestResultsContainer();
        for (HearingTestSingleIntervalResult htsr : this.allResultsUpward.values())
            newContainer.allResultsUpward.put(htsr.freq1, htsr.getSubsetResult(n));
        for (HearingTestSingleIntervalResult htsr : this.allResultsDownward.values())
            newContainer.allResultsDownward.put(htsr.freq1, htsr.getSubsetResult(n));
        return newContainer;
    }

    /**
     * @return The number of trials in this hearing test (assumes all intervals tested equal number of times)
     */
    @SuppressWarnings("ConstantConditions")
    public int getNumOfTrials() {
        HearingTestSingleIntervalResult aResult = this.allResultsUpward.get(this.getTestedFreqs()[0]);
        double aVol = aResult.getVolumes().iterator().next();
        return aResult.getNumSamples(aVol);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (HearingTestSingleIntervalResult result : allResultsUpward.values()) builder.append(result.toString());
        for (HearingTestSingleIntervalResult result : allResultsDownward.values()) builder.append(result.toString());
        return builder.toString();
    }

    /**
     * Given a list of floats, return the number closest to f while being greater than f, or -1 if none found
     */
    public static float findNearestAbove(float f, Float[] lst) {
        float closest = -1;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < freq - f && freq - f < distance) {
                closest = freq;
                distance = freq - f;
            }
        }
        return closest;
    }

    /**
     * Given a list of floats, return the number closest to f while being less than f, or -1 if none found
     */
    public static float findNearestBelow(float f, Float[] lst) {
        float closest = -1f;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < f - freq && f - freq < distance) {
                closest = freq;
                distance = f - freq;
            }
        }
        return closest;
    }

    /**
     * @return true if the array contains the item, else false
     */
    public static boolean arrContains(Comparable[] arr, Comparable item) {
        for (Comparable c : arr) if (c.equals(item)) return true;
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A class for storing the hearing test results for a single frequency at multiple volumes
     */
    protected class HearingTestSingleIntervalResult {

        private final float freq1; // the first frequency of the interval

        private final float freq2; // the second frequency of the interval

        public final boolean isUpward; // true -> upward interval, false -> downward interval

        /* (timesCorr or timesIncorr).keySet() doesn't necessarily contain all tested volumes - if a volume is never
         * answered correctly, timesCorr will not contain a key for that volume (sim. for timesIncorr). All tested
         * intervals exist in testResultsPerVol.keySet() but preferably use this.getVolumes()
         * */
        // do not mutate these maps except in addResult()
        private HashMap<Double, Integer> timesCorrPerVol; // how many times has the user answered correctly at each vol

        private HashMap<Double, Integer> timesIncorrPerVol;  // ^^ except incorrectly

        private HashMap<Double, List<Boolean>> testResultsPerVol; // The sequence of responses for each volume

        private HearingTestSingleIntervalResult(float freq1, float freq2) {
            this.freq1 = freq1;
            this.freq2 = freq2;
            this.timesCorrPerVol = new HashMap<>();
            this.timesIncorrPerVol = new HashMap<>();
            this.testResultsPerVol = new HashMap<>();
            this.isUpward = freq1 < freq2;
        }

        /**
         * Add a new test result to this object
         *
         * @param vol The volume of the trial
         * @param correct Did the user correctly guess the direction of the interval?
         */
        @SuppressWarnings("ConstantConditions")
        public void addResult(double vol, boolean correct) {
            // update testResultsPerVol
            if (!testResultsPerVol.containsKey(vol)) testResultsPerVol.put(vol, new ArrayList<Boolean>());
            testResultsPerVol.get(vol).add(correct);
            // update to timesCorr/timesIncorr
            if (correct)
                if (timesCorrPerVol.containsKey(vol))
                    mapReplace(timesCorrPerVol, vol, timesCorrPerVol.get(vol) + 1);

                else timesCorrPerVol.put(vol, 1);
            else
                if (timesIncorrPerVol.containsKey(vol))
                    mapReplace(timesIncorrPerVol, vol, timesIncorrPerVol.get(vol) + 1);
                else timesIncorrPerVol.put(vol, 1);
        }

        /**
         * Get the probability of hearing a tone of this.freq Hz at the given volume
         *
         * @param vol The volume whose probability is to be found
         * @return The probability of hearing a tone of this.freq Hz at the given volume
         */
        public float getProbOfHearing(double vol) {
            // sanity check
            if (testResultsPerVol.isEmpty()) throw new IllegalStateException("testResultsPerVol unexpectedly empty");

            // find volumes just above and below, or if they are smaller than the smallest or larger than the
            // largest, then return the probability of the nearest volume
            double volBelow, volAbove;
            try {
                volBelow = findNearestBelow(vol, this.getVolumes());
            } catch (IllegalArgumentException e) {
                return this.getActualProb(findNearestAbove(vol, this.getVolumes()));
            }
            try {
                volAbove = findNearestAbove(vol, this.getVolumes());
            } catch (IllegalArgumentException e) {
                return this.getActualProb(findNearestBelow(vol, this.getVolumes()));
            }

            // what percentage of the way between volBelow and volAbove is vol?
            float pctBetween = (float) (vol - volBelow) / (float) (volAbove - volBelow);

            // return value on the line between the probabilities of the volumes just above and below the given volume
            float probOfVolBelow = this.getActualProb(volBelow);
            float probOfVolAbove = this.getActualProb(volAbove);
            return probOfVolBelow + pctBetween * (probOfVolAbove - probOfVolBelow);
        }

        /**
         * Gets the actual probability found of being able to differentiate this interval at a given volume
         *
         * @param vol The volume whose probability is to be found
         * @return The probability of differentiating the interval at the given volume
         * @throws IllegalArgumentException If the given volume was not tested
         */
        @SuppressWarnings("ConstantConditions")
        public float getActualProb(double vol) throws IllegalArgumentException {
            if (! this.getVolumes().contains(vol)) throw new IllegalArgumentException("Volume not present in results");
            int timesCorr, timesIncorr;
            try {
                timesCorr = timesCorrPerVol.get(vol);
            } catch (NullPointerException e) {
                timesCorr = 0;
            }
            try {
                timesIncorr = timesIncorrPerVol.get(vol);
            } catch (NullPointerException e) {
                timesIncorr = 0;
            }
            return (float) timesCorr / (float) (timesCorr + timesIncorr);
        }

        /**
         * @return The largest volume which was never answered correctly, or the lowest tested volume if all were
         * answered correctly at least once
         */
        public double getVolFloor() {
            ArrayList<Double> unheardVols = new ArrayList<>();
            for (double vol : this.getVolumes()) if (! timesCorrPerVol.containsKey(vol)) unheardVols.add(vol);
            if (unheardVols.isEmpty()) return Collections.min(this.getVolumes());
            else return Collections.max(unheardVols);
        }

        /**
         * @return The smallest volume which was answered correctly in every trial, or the largest tested volume if none
         * were answered correctly in every trial
         */
        public double getVolCeiling() {
            ArrayList<Double> alwaysHeardVols = new ArrayList<>();
            for (double vol : this.getVolumes()) if (! timesIncorrPerVol.containsKey(vol)) alwaysHeardVols.add(vol);
            if (alwaysHeardVols.isEmpty()) return Collections.max(this.getVolumes());
            else return Collections.min(alwaysHeardVols);
        }

        public HashMap<Double, Integer> getTimesCorrPerVol() {
            return timesCorrPerVol;
        }

        public HashMap<Double, Integer> getTimesIncorrPerVol() {
            return timesIncorrPerVol;
        }

        /**
         * Return the number of times that the given volume was sampled
         *
         * @param vol The volume whose number of samples is to be returned
         * @return The number of times that the given volume was sampled
         */
        @SuppressWarnings("ConstantConditions")
        public int getNumSamples(double vol) {
            int corr, incorr;
            try {
                corr = this.timesCorrPerVol.get(vol);
            } catch (NullPointerException e) {
                corr = 0;
            }
            try {
                incorr = this.timesIncorrPerVol.get(vol);
            } catch (NullPointerException e) {
                incorr = 0;
            }
            return corr + incorr;
        }

        /**
         * Returns a new HearingTestSingleIntervalResult containing the first n results for each volume stored within
         * this object
         *
         * @param n The number of results for each frequency
         * @return A new HearingTestSingleIntervalResult containing a subset of the results in this one
         */
        @SuppressWarnings("ConstantConditions")
        public HearingTestSingleIntervalResult getSubsetResult(int n) {
            HearingTestSingleIntervalResult newResult = new HearingTestSingleIntervalResult(this.freq1, this.freq2);
            for (Double vol : this.getVolumes()) {
                // addResult for the first n responses in the hearing test
                ListIterator<Boolean> iter = this.testResultsPerVol.get(vol).listIterator();
                for (int i = 0; i < n; i++) {
                    newResult.addResult(vol, iter.next());
                }
            }
            return newResult;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            ArrayList<Double> testedVolumes = new ArrayList<>(this.getVolumes());
            Collections.sort(testedVolumes);

            builder.append(String.format("Interval: %.1f, %.1f\n", this.freq1, this.freq2));

            for (Double d : testedVolumes) {
                builder.append(String.format("Volume %.2f: P(Heard) = %.4f\n", d, this.getActualProb(d)));
            }
            return builder.toString();
        }

        /**
         * Delete any mapping from the given key if it exists, then add a mapping between the given key and the new
         * value in the given map
         *
         * @param map The map to be affected
         * @param key The key whose value is to be replaced
         * @param newValue The new value to associate with the key
         */
        public void mapReplace(HashMap<Double, Integer> map, Double key, Integer newValue) {
            map.remove(key);
            map.put(key, newValue);
        }

        public Collection<Double> getVolumes() {
            return this.testResultsPerVol.keySet();
        }

        /**
         * Given a collection of doubles, find the double nearest to d which is also less than d
         * Only intended for use with collections of positive numbers
         * @throws IllegalArgumentException if d is smaller than the smallest element of the collection
         */
        public double findNearestBelow(double d, Collection<Double> dbls) {
            double closest = -1.0;
            double distance = Double.MAX_VALUE;
            for (double dbl : dbls) {
                if (0 < d - dbl && d - dbl < distance) {
                    closest = dbl;
                    distance = d - dbl;
                }
            }
            if (closest == -1) throw new IllegalArgumentException("No elements less than d found");
            else return closest;
        }

        /**
         * Given a collection of doubles, find the double nearest to d which is also greater than d
         * Only intended for use with collections of positive numbers
         * @throws IllegalArgumentException if d is larger than the largest element of the collection
         */
        public double findNearestAbove(double d, Collection<Double> dbls) throws IllegalArgumentException {
            double closest = -1.0;
            double distance = Double.MAX_VALUE;
            for (double dbl : dbls) {
                if (0 < dbl - d && dbl - d < distance) {
                    closest = dbl;
                    distance = dbl - d;
                }
            }
            if (closest == -1) throw new IllegalArgumentException("No elements greater than d found");
            else return closest;
        }
    }
}
