package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ConfidenceTestResultsContainer {

    // map each frequency to all single test results of that frequency
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResults;

    public ConfidenceTestResultsContainer() {
        this.allResults = new HashMap<>();
    }

    /**
     * Add a new confidence test result to this container
     *
     * @param freq The frequency of the trial
     * @param vol The volume of the trial
     * @param heard Whether the tone was heard in the trial
     */
    public void addResult(float freq, double vol, boolean heard) {
        FreqVolPair fvp = new FreqVolPair(freq, vol);
        ConfidenceSingleTestResult cstr = this.getResultForFVP(fvp);
        // create new cstr and/or allResults entry if required
        if (cstr == null) {
            cstr = new ConfidenceSingleTestResult(fvp);
            try {
                allResults.get(freq).add(cstr);
            } catch (NullPointerException e) {
                List<ConfidenceSingleTestResult> newList = new ArrayList<>();
                newList.add(cstr);
                allResults.put(freq, newList);
            }
        }
        cstr.addTrial(heard);
    }

    /**
     * Return a mapping of each tested volume to the number of times the volume was heard for the given frequency
     *
     * @param freq The frequency whose test results are to be queried
     * @return A mapping of each volume tested at the given frequency to the number of times it was heard
     */
    public HashMap<Double, Integer> getTimesHeardPerVolForFreq(float freq) {
        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = this.allResults.get(freq);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.vol, cstr.getTimesHeard());
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
    public HashMap<Double, Integer> getTimesNotHeardPerVolForFreq(float freq) {
        List<ConfidenceSingleTestResult> results;
        HashMap<Double, Integer> outMap= new HashMap<>();
        try {
            results = this.allResults.get(freq);
            for (ConfidenceSingleTestResult cstr : results) {
                outMap.put(cstr.vol, cstr.getTimesNotHeard());
            }
        } catch (NullPointerException e) { if (! outMap.isEmpty()) throw e; }
        return outMap;
    }

    /**
     * @return An array of all frequencies present in these confidence test results
     */
    public float[] getTestedFrequencies() {
        float[] outArr = new float[this.allResults.size()];
        Set<Float> freqSet = this.allResults.keySet();
        int i = 0;
        for (Float f : freqSet) outArr[i++] = f;
        return outArr;
    }

    /**
     * @return True if there are no results stored in this container
     */
    public boolean isEmpty() {
        return this.allResults.isEmpty();
    }

    /**
     * Returns an array conatining all the volumes tested at the given frequency
     *
     * @param freq The frequency whose tested volumes are to be found
     * @return All volumes at which the given frequency was tested
     */
    public double[] getTestedVolsForFreq(float freq) {
        try {
            List<ConfidenceSingleTestResult> resultList = this.allResults.get(freq);
            double[] outArr = new double[resultList.size()];
            int i = 0;
            for (ConfidenceSingleTestResult cstr : resultList) outArr[i++] = cstr.vol;
            return outArr;
        } catch (NullPointerException e) {
            return new double[0];
        }
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
    public float getActualResultForFVP(float freq, double vol) throws IllegalArgumentException {
        ConfidenceSingleTestResult result = this.getResultForFVP(freq, vol);
        if (result == null) throw new IllegalArgumentException("frequency-volume pair not present in results");
        return result.getActual();
    }

    public float getActualResultForFVP(FreqVolPair fvp) {
        return getActualResultForFVP(fvp.getFreq(), fvp.getVol());
    }

    /**
     * Get and return the ConfidenceSingleTestResult (CSTR) object for the given freq and vol, or null if does not exist
     *
     * @param freq The frequency of the desired CSTR
     * @param vol The volume of the desired CSTR
     * @return The CSTR with the given frequency and volume, or null if none found
     */
    private ConfidenceSingleTestResult getResultForFVP(float freq, double vol) {
        List<ConfidenceSingleTestResult> resultList = allResults.get(freq);
        if (resultList == null) return null;
        for (ConfidenceSingleTestResult cstr : resultList)
            if (cstr.vol == vol && cstr.freq == freq) return cstr;
            else if (cstr.freq != freq)
                throw new AssertionError("Result with frequency " + cstr.freq + " found in list for frequency " + freq);
        return null;
    }

    private ConfidenceSingleTestResult getResultForFVP(FreqVolPair fvp) {
        return getResultForFVP(fvp.getFreq(), fvp.getVol());
    }

    /**
     * A class to store the result of a single freq-vol pair test from a confidence test
     *
     * @author redekopp
     */
    private class ConfidenceSingleTestResult {

        public final float freq;
        public final double vol;

        private int nHeard;
        private int nNotHeard;

        public ConfidenceSingleTestResult(FreqVolPair fvp) {
            this.nHeard = 0;
            this.nNotHeard = 0;
            this.freq = fvp.getFreq();
            this.vol = fvp.getVol();
        }

        /**
         * Add a new trial to this single test result
         * @param heard Whether the tone was heard in the trial to be added
         */
        public void addTrial(boolean heard) {
            if (heard) this.nHeard++;
            else this.nNotHeard++;
        }

        /**
         * @return The fraction [0, 1] of times that the tone was heard
         */
        public float getActual() {
            return (float) this.nHeard / (float) (this.nNotHeard + this.nHeard);
        }

        /**
         * @return The number of tests in this result which were heard
         */
        public int getTimesHeard() {
            return this.nHeard;
        }

        /**
         * @return The number of tests in this result which were not heard
         */
        public int getTimesNotHeard() {
            return  this.nNotHeard;
        }
    }

}
