package ca.usask.cs.tonesetandroid;

import java.util.HashMap;
import java.util.List;

public class HearingTestResultsContainer {

    public HashMap<Float, HearingTestSingleFreqResult> allResults;

    public HearingTestResultsContainer() {
        allResults = new HashMap<>();
    }

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (HearingTestSingleFreqResult result : allResults.values()) builder.append(result.toString());
        return builder.toString();
    }

    public boolean isEmpty() {
        return allResults.isEmpty();
    }

    /**
     * @return  the probability of hearing a tone of the given frequency at the given volume
     *          Only works on frequencies who have results stored in this container - returns < 0 otherwise
     */
    public float getProbOfHearingFVP(float freq, double vol) {
        try {
            return allResults.get(freq).getProbOfHearing(vol);
        } catch (NullPointerException e) {
            return -1;
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

}
