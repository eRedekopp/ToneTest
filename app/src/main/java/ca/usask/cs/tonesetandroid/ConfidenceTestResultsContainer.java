package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ConfidenceTestResultsContainer {

    private HearingTestResultsContainer calibResults;

    // map each frequency to all single test results of that frequency
    private HashMap<Float, List<ConfidenceSingleTestResult>> allResults;

    public ConfidenceTestResultsContainer(HearingTestResultsContainer calibResults) {
        this.allResults = new HashMap<>();
        this.calibResults = calibResults;
    }

    public void addResult(float freq, double vol, boolean heard) {
        FreqVolPair fvp = new FreqVolPair(freq, vol);
        ConfidenceSingleTestResult cstr = this.getResultForFVP(fvp);
        // create new cstr and/or allResults entry if required
        if (cstr == null) {
            cstr = new ConfidenceSingleTestResult(calibResults.getProbOfHearingFVP(fvp), fvp);
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

    public float[] getTestedFrequencies() {
        float[] outArr = new float[this.allResults.size()];
        Set<Float> freqSet = this.allResults.keySet();
        int i = 0;
        for (Float f : freqSet) outArr[i++] = f;
        return outArr;
    }

    public boolean isEmpty() {
        return this.allResults.isEmpty();
    }

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

    public float getExpectedProbForFVP(float freq, double vol) {
        ConfidenceSingleTestResult cstr = this.getResultForFVP(freq, vol);
        if (cstr == null) throw new IllegalStateException("Freq-vol pair freq = " + freq +
                                                          " vol = " + vol + " not found in test results");
        else return cstr.expected;
    }

    public float getExpectedProbForFVP(FreqVolPair fvp) {
        return getExpectedProbForFVP(fvp.getFreq(), fvp.getVol());
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

}
