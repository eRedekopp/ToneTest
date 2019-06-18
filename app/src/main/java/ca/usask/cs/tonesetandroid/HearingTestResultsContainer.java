package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
     *          Only works on frequencies who have results stored in this container - returns < 0 otherwise
     */
    public float getProbOfHearingFVP(float freq, double vol) {
        // todo make this work with non-tested frequencies
        try {
            return allResults.get(freq).getProbOfHearing(vol);
        } catch (NullPointerException e) {
            return -1;
        }
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
    public List<Double> getTestedVolumesForFreq(float freq) {
        try {
            return this.allResults.get(freq).getVolumes();
        } catch (NullPointerException e) {
            return new ArrayList<>();  // empty list if frequency not tested
        }
    }

    /**
     * @return An array of all the frequencies with data stored in the model
     */
    public Float[] getFreqs() {
        Float[] outArr = new Float[allResults.size()];
        allResults.keySet().toArray(outArr);
        return outArr;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (HearingTestSingleFreqResult result : allResults.values()) builder.append(result.toString());
        return builder.toString();
    }

    /**
     * A class for storing the hearing test results for a single frequency at multiple volumes
     */
    private class HearingTestSingleFreqResult {

        private float freq;

        private HashMap<Double, Integer> timesHeardPerVol;

        private HashMap<Double, Integer> timesNotHeardPerVol;

        private ArrayList<Double> testedVolumes;

        public HearingTestSingleFreqResult(float freq) {
            this.freq = freq;
            this.timesHeardPerVol = new HashMap<>();
            this.timesNotHeardPerVol = new HashMap<>();
            this.testedVolumes = new ArrayList<>();
        }

        /**
         * Add a new test result to this singleFreqResult
         *
         * @param vol The volume of the trial
         * @param heard Was the tone heard in the trial?
         */
        public void addResult(double vol, boolean heard) {
            if (!testedVolumes.contains(vol)) testedVolumes.add(vol);
            if (heard)
                if (timesHeardPerVol.containsKey(vol))
                    mapReplace(timesNotHeardPerVol, vol, timesNotHeardPerVol.get(vol) + 1);
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
        @SuppressWarnings("ConstantConditions")
        public float getProbOfHearing(double vol) {
            double volBelow = findNearestBelow(vol, (Double[]) timesHeardPerVol.keySet().toArray());
            double volAbove = findNearestAbove(vol, (Double[]) timesHeardPerVol.keySet().toArray());
            // what percentage of the way between volBelow and volAbove is vol?
            float pctBetween = (float) (vol - volBelow) / (float) (volAbove - volBelow);
            float probOfVolBelow =
                    (float) timesHeardPerVol.get(volBelow) /
                            (float) (timesHeardPerVol.get(volBelow) + timesNotHeardPerVol.get(volBelow));
            float probOfVolAbove =
                    (float) timesHeardPerVol.get(volAbove) /
                            (float) (timesHeardPerVol.get(volAbove) + timesNotHeardPerVol.get(volAbove));
            // return value on the line between the probabilities of the volumes just above and below the given volume
            return probOfVolBelow + pctBetween * (probOfVolAbove - probOfVolBelow);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
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

        /**
         * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being greater than f
         */
        public double findNearestAbove(double v, Double[] lst) {
            double closest = -1.0;
            double distance = Double.MAX_VALUE;
            for (double vol : lst) {
                if (0 < vol - v && vol - v < distance) {
                    closest = vol;
                    distance = vol - v;
                }
            }
            return closest;
        }

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesHeardPerVol.clone();
        }

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesNotHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesNotHeardPerVol.clone();
        }

        public List<Double> getVolumes() {
            return this.testedVolumes;
        }

        /**
         * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being less than f
         */
        public double findNearestBelow(double v, Double[] lst) {
            double closest = -1.0;
            double distance = Double.MAX_VALUE;
            for (double freq : lst) {
                if (0 < v - freq && v - freq < distance) {
                    closest = freq;
                    distance = v - freq;
                }
            }
            return closest;
        }
    }
}
