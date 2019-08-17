package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.HearingTest.Test.ReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.UtilFunctions;

/**
 * A class for ramp test results that stores results from a reduce test and uses them to
 * calculate volume floors differently
 */
public class RampTestResultsWithFloorInfo extends RampTestResults {

    private ReduceTest.ReduceTestResults reduceResults = null;

    public RampTestResultsWithFloorInfo() {
        super();
    }

    public void setReduceResults(ReduceTest.ReduceTestResults reduceResults) {
        this.reduceResults = reduceResults;
    }

    /**
     * @return A RampTestResults container identical to this one but without the floor info
     */
    @SuppressWarnings("unchecked")
    public RampTestResults getRegularRampResults() {
        RampTestResults newResults = new RampTestResults();
        newResults.allResults = (HashMap<Float, VolPair>) this.allResults.clone();
        return newResults;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected double getVolFloorEstimate(float freq) {

        if (this.reduceResults == null) throw new IllegalStateException("No reduce results stored");

        FreqVolPair[] results = this.reduceResults.getResults();
        Float[] testedFreqs = FreqVolPair.getFreqsFromPairs(results);

        // If freq tested, return result from reduce test
        Tone testedTone = UtilFunctions.get(results, freq);
        if (testedTone != null) return testedTone.vol();

        // If freq is higher than highest or lower than lowest, return test result of nearest
        float maxFreq = Collections.max(Arrays.asList(testedFreqs)),
              minFreq = Collections.min(Arrays.asList(testedFreqs));
        if (freq > maxFreq) return UtilFunctions.get(results, maxFreq).vol();
        if (freq < minFreq) return UtilFunctions.get(results, minFreq).vol();

        // Get max ramp vols of nearest tested frequencies
        Tone toneAbove = UtilFunctions.findNearestAbove(freq, results);
        Tone toneBelow = UtilFunctions.findNearestBelow(freq, results);
        float freqAbove = toneAbove.freq();
        float freqBelow = toneBelow.freq();

        // How far between freqBelow and freqAbove is freq?
        double pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // Estimate vol floor linearly
        double volBelow = UtilFunctions.get(results, freqBelow).vol();
        double volAbove = UtilFunctions.get(results, freqAbove).vol();

        return volBelow + pctBetween * (volAbove - volBelow);
    }
}
