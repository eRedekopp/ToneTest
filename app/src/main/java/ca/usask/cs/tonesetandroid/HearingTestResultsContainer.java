package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class HearingTestResultsContainer {

    // each frequency tested mapped to its corresponding SingleFreqResult
    public HashMap<Float, HearingTestSingleFreqResult> allResults;

    public HearingTestResultsContainer() {
        allResults = new HashMap<>();
    }

    /**
     * Add a new calibration test result to this container
     *
     * @param freq The frequency of the trial
     * @param vol The volume of the trial
     * @param heard Was the trial heard?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(float freq, double vol, boolean heard) {
        try {
            allResults.get(freq).addResult(vol, heard);
        } catch (NullPointerException e) {
            HearingTestSingleFreqResult r = new HearingTestSingleFreqResult(freq);
            r.addResult(vol, heard);
            allResults.put(freq, r);
        }
    }

    /**
     * @return true if there are no results stored in this container, else false
     */
    public boolean isEmpty() {
        return allResults.isEmpty();
    }

    /**
     * @return  the probability of hearing a tone of the given frequency at the given volume
     */
    public float getProbOfHearingFVP(float freq, double vol) {

        // find frequencies tested just above and below freq
        float freqAbove = findNearestAbove(freq, this.getTestedFreqs());
        float freqBelow = findNearestBelow(freq, this.getTestedFreqs());

        // if freq is higher than the highest or lower than the lowest, just return the probability of the nearest
        if (freqAbove == -1) return this.allResults.get(freqBelow).getProbOfHearing(vol);
        if (freqBelow == -1) return this.allResults.get(freqAbove).getProbOfHearing(vol);

        // find the probabilities of each of these frequencies
        float probAbove = this.allResults.get(freqAbove).getProbOfHearing(vol);
        float probBelow = this.allResults.get(freqAbove).getProbOfHearing(vol);

        // how far of the way between freqBelow and freqAbove is fvp.freq?
        float pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
    }

    /**
     * Given a subset of the tested frequencies, return the probability of hearing a tone of the given frequency and
     * volume modeled only based on the subset frequencies
     *
     * @param freq The frequency of the tone whose probability is to be determined
     * @param vol The volume of the tone whose probability is to be determined
     * @param subset A subset of the calibration frequencies to be used to generate the model
     * @return The estimated probability of hearing the tone, based on the given subset
     * @throws IllegalArgumentException If the given subset is not a subset of the tested frequencies
     */
    public float getProbOfHearingFVP(float freq, double vol, float[] subset) throws IllegalArgumentException{
        Float[] subsetAsObj = new Float[subset.length];
        for (int i = 0; i < subset.length; i++)
            if (! this.allResults.containsKey(subset[i]))
                throw new IllegalArgumentException("Subset argument must be a subset of tested frequencies");
            else subsetAsObj[i] = subset[i];

        // find subset frequencies just above and below tested frequencies
        float freqAbove = findNearestAbove(freq, subsetAsObj);
        float freqBelow = findNearestBelow(freq, subsetAsObj);

        // if freq is higher than the highest or lower than the lowest, return the probability of the nearest
        if (freqAbove == -1) return this.allResults.get(freqBelow).getProbOfHearing(vol);
        if (freqBelow == -1) return this.allResults.get(freqAbove).getProbOfHearing(vol);

        // find probabilities of each of these frequencies
        float probAbove = this.allResults.get(freqAbove).getProbOfHearing(vol);
        float probBelow = this.allResults.get(freqBelow).getProbOfHearing(vol);

        // how far of the way between freqBelow and freqAbove is fvp.freq?
        float pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
    }

    public float getProbOfHearingFVP(FreqVolPair fvp) {
        return getProbOfHearingFVP(fvp.getFreq(), fvp.getVol());
    }

    /**
     * Returns a mapping of volumes to the number of times each volume was heard in the test for the given frequency
     *
     * @param freq The frequency whose volume results are to be returned
     * @return A mapping of volumes to the number of times they were heard
     */
    public HashMap<Double, Integer> getTimesHeardPerVolForFreq(float freq) {
        try {
            return this.allResults.get(freq).getTimesHeardPerVol();
        } catch (NullPointerException e) {
            return new HashMap<>();  // empty map if freq not tested
        }
    }

    /**
     * Returns a mapping of volumes to the number of times each volume was not heard in the test for the given frequency
     *
     * @param freq The frequency whose volume results are to be returned
     * @return A mapping of volumes to the number of times they weren't heard
     */
    public HashMap<Double, Integer> getTimesNotHeardPerVolForFreq(float freq) {
        try {
            return this.allResults.get(freq).getTimesNotHeardPerVol();
        } catch (NullPointerException e) {
            return new HashMap<>();  // empty map if freq not tested
        }
    }

    /**
     * Returns a list of all volumes tested for the given frequency
     *
     * @param freq The frequency whose tested volumes are to be returned
     * @return A list of all volumes tested for the given frequency
     */
    public Collection<Double> getTestedVolumesForFreq(float freq) {
        try {
            return this.allResults.get(freq).getVolumes();
        } catch (NullPointerException e) {
            return new ArrayList<>();  // empty list if frequency not tested
        }
    }

    /**
     * @return An array of all the frequencies present in these results
     */
    public Float[] getTestedFreqs() {
        Float[] outArr = new Float[allResults.size()];
        allResults.keySet().toArray(outArr);
        return outArr;
    }

    public boolean freqTested(float freq) {
        for (Float f : allResults.keySet()) if (f == freq) return true;
        return false;
    }

    /**
     * Returns an estimate of the highest volume which has a 0% chance of being heard for the given frequency
     *
     * @param freq The frequency whose volume floor is to be estimated
     * @return An estimate of the volume floor for the given frequency
     */
    public double getVolFloorEstimateForFreq(float freq) {
        if (this.freqTested(freq)) return this.allResults.get(freq).getVolFloor();

        float nearestBelow = findNearestBelow(freq, this.getTestedFreqs());
        float nearestAbove = findNearestAbove(freq, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return this.allResults.get(this.getNearestTestedFreq(freq)).getVolFloor();

        float pctBetween = (freq - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = this.allResults.get(nearestBelow).getVolFloor();
        double floorAbove = this.allResults.get(nearestAbove).getVolFloor();

        return floorBelow + (floorAbove - floorBelow) * pctBetween;
    }

    /**
     * Returns an estimate of the lowest volume which has a 100% percent chance of being heard for the given frequency
     *
     * @param freq The frequency whose volume ceiling is to be estimated
     * @return An estimate of the volume ceiling for the given frequency
     */
    @SuppressWarnings("ConstantConditions")
    public double getVolCeilingEstimateForFreq(float freq) {
        if (this.freqTested(freq)) return this.allResults.get(freq).getVolCeiling();

        float nearestBelow = findNearestBelow(freq, this.getTestedFreqs());
        float nearestAbove = findNearestAbove(freq, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return this.allResults.get(this.getNearestTestedFreq(freq)).getVolCeiling();

        float pctBetween = (freq - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = this.allResults.get(nearestBelow).getVolCeiling();
        double floorAbove = this.allResults.get(nearestAbove).getVolCeiling();

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
        for (float f : this.allResults.keySet())
            if (Math.abs(f - nearestTo) < minDiff) {
                curClosest = f;
                minDiff = Math.abs(f - nearestTo);
            }
        if (curClosest == -1) throw new RuntimeException("Found unexpected value -1");
        return curClosest;
    }

    /**
     * Return a new HearingTestResultsContainer with the same results as this one, but only containing the first n
     * results for each frequency-volume pair (ie. as though Model.NUMBER_OF_TESTS_PER_VOL == n)
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
        for (HearingTestSingleFreqResult htsr : this.allResults.values())
            newContainer.allResults.put(htsr.freq, htsr.getSubsetResult(n));
        return newContainer;
    }

    /**
     * @return The number of trials in this hearing test (assumes all FVPs tested equal number of times)
     */
    @SuppressWarnings("ConstantConditions")
    public int getNumOfTrials() {
        HearingTestSingleFreqResult aResult = this.allResults.get(this.getTestedFreqs()[0]);
        double aVol = aResult.getVolumes().iterator().next();
        return aResult.getNumSamples(aVol);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (HearingTestSingleFreqResult result : allResults.values()) builder.append(result.toString());
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A class for storing the hearing test results for a single frequency at multiple volumes
     */
    protected class HearingTestSingleFreqResult {

        private final float freq;

        // do not mutate these maps except in addResult()
        private HashMap<Double, Integer> timesHeardPerVol;

        private HashMap<Double, Integer> timesNotHeardPerVol;

        private HashMap<Double, List<Boolean>> testResultsPerVol;

        private HearingTestSingleFreqResult(float freq) {
            this.freq = freq;
            this.timesHeardPerVol = new HashMap<>();
            this.timesNotHeardPerVol = new HashMap<>();
            this.testResultsPerVol = new HashMap<>();
        }

        /**
         * Add a new test result to this singleFreqResult
         *
         * @param vol The volume of the trial
         * @param heard Was the tone heard in the trial?
         */
        @SuppressWarnings("ConstantConditions")
        public void addResult(double vol, boolean heard) {
            // update testResultsPerVol
            if (!testResultsPerVol.containsKey(vol)) testResultsPerVol.put(vol, new ArrayList<Boolean>());
            testResultsPerVol.get(vol).add(heard);
            // update to timesHeard / timesNotHeard
            if (heard)
                if (timesHeardPerVol.containsKey(vol))
                    mapReplace(timesHeardPerVol, vol, timesHeardPerVol.get(vol) + 1);

                else timesHeardPerVol.put(vol, 1);
            else
                if (timesNotHeardPerVol.containsKey(vol))
                    mapReplace(timesNotHeardPerVol, vol, timesNotHeardPerVol.get(vol) + 1);
                else timesNotHeardPerVol.put(vol, 1);

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
         * Gets the actual probability found of hearing the given volume
         *
         * @param vol The volume whose probability is to be found
         * @return The probability of hearing the volume
         * @throws IllegalArgumentException If the given volume was not tested
         */
        @SuppressWarnings("ConstantConditions")
        public float getActualProb(double vol) throws IllegalArgumentException {
            if (! this.getVolumes().contains(vol)) throw new IllegalArgumentException("Volume not present in results");
            int timesHeard, timesNotHeard;
            try {
                timesHeard = timesHeardPerVol.get(vol);
            } catch (NullPointerException e) {
                timesHeard = 0;
            }
            try {
                timesNotHeard = timesNotHeardPerVol.get(vol);
            } catch (NullPointerException e) {
                timesNotHeard = 0;
            }
            return (float) timesHeard / (float) (timesHeard + timesNotHeard);
        }

        /**
         * @return The largest volume which was never heard, or the lowest tested volume if all were heard
         */
        public double getVolFloor() {
            ArrayList<Double> unheardVols = new ArrayList<>();
            for (double vol : this.getVolumes()) if (! timesHeardPerVol.containsKey(vol)) unheardVols.add(vol);
            if (unheardVols.isEmpty()) return Collections.min(this.getVolumes());
            else return Collections.max(unheardVols);
        }

        /**
         * @return The smallest volume which was heard in every trial, or the highest tested volume if none were
         * heard every time
         */
        public double getVolCeiling() {
            ArrayList<Double> alwaysHeardVols = new ArrayList<>();
            for (double vol : this.getVolumes()) if (! timesNotHeardPerVol.containsKey(vol)) alwaysHeardVols.add(vol);
            if (alwaysHeardVols.isEmpty()) return Collections.max(this.getVolumes());
            else return Collections.min(alwaysHeardVols);
        }

        /**
         * Return the number of times that the given volume was sampled
         *
         * @param vol The volume whose number of samples is to be returned
         * @return The number of times that the given volume was sampled
         */
        @SuppressWarnings("ConstantConditions")
        public int getNumSamples(double vol) {
            int heard, notHeard;
            try {
                heard = this.timesHeardPerVol.get(vol);
            } catch (NullPointerException e) {
                heard = 0;
            }
            try {
                notHeard = this.timesNotHeardPerVol.get(vol);
            } catch (NullPointerException e) {
                notHeard = 0;
            }
            return heard + notHeard;
        }

        /**
         * Returns a new HearingTestSingleFreqResult containing the first n results for each volume stored within this
         * object
         *
         * @param n The number of results for each frequency
         * @return A new HearingTestSingleFreqResult containing a subset of the results in this one
         */
        @SuppressWarnings("ConstantConditions")
        public HearingTestSingleFreqResult getSubsetResult(int n) {
            HearingTestSingleFreqResult newResult = new HearingTestSingleFreqResult(this.freq);
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
        @SuppressWarnings("ConstantConditions")
        public String toString() {
            StringBuilder builder = new StringBuilder();
            ArrayList<Double> testedVolumes = new ArrayList<>(this.getVolumes());
            Collections.sort(testedVolumes);

            builder.append("Frequency: ");
            builder.append(this.freq);
            builder.append('\n');
            for (Double d : testedVolumes) {
                int timesHeard;
                try {
                    timesHeard = this.timesHeardPerVol.get(d);
                } catch (NullPointerException e) {
                    timesHeard = 0;
                }
                int timesNotHeard;
                try {
                    timesNotHeard = this.timesNotHeardPerVol.get(d);
                } catch (NullPointerException e) {
                    timesNotHeard = 0;
                }
                float pHeard = (float) timesHeard / (float) (timesHeard + timesNotHeard);
                builder.append("Volume ");
                builder.append(String.format("%.2f", d));
                builder.append(": P(heard) = ");
                builder.append(String.format("%.4f\n", pHeard));
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

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesHeardPerVol.clone();
        }

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesNotHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesNotHeardPerVol.clone();
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
