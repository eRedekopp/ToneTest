package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.HashMap;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class RampTestResults implements HearingTestResults {

    public static final int[] EQUATION_ID_NUMS = {0, 1};

    private int equationID = 0;

    /**
     * A mapping of each frequency tested to the first and second volumes selected by the user at
     * that frequency
     */
    private HashMap<Float, VolPair> allResults;

    public RampTestResults() {
        this.allResults = new HashMap<>();
    }

    @Override
    public double getProbability(Tone tone) throws IllegalStateException {
        if (equationID == 0) return getProbabilityLinear(tone);
        if (equationID == 1) return getProbabilityLogarithmic(tone);
        else throw new IllegalStateException("Equation ID set to invalid value: " + equationID);
    }

    /**
     * Return the probability of hearing the tone given these hearing test results, using the
     * modeling equation y = x
     *
     * @param tone The tone whose probability is to be determined
     * @return The probability of hearing the tone, modeling using the equation y = x
     * @throws IllegalStateException If there are no results stored in this container
     */
    protected double getProbabilityLinear(Tone tone) throws IllegalStateException {
        return 0.0; // todo
    }

    /**
     * Return the probability of hearing the tone given these hearing test results, using the
     * modeling equation y = ln((e - 1)x + 1)
     *
     * @param tone The tone whose probability is to be determined
     * @return The probability of hearing the tone, modeling using the equation y = ln((e - 1)x + 1)
     * @throws IllegalStateException If there are no results stored in this container
     */
    protected double getProbabilityLogarithmic(Tone tone) throws IllegalStateException {
        return 0.0; // todo
    }

    /**
     * Return an estimate for the volume floor (loudest volume which will be heard 0% of the
     * time) for the given frequency
     */
    protected double getVolFloorEstimate(float freq) {
        return 0.0; // todo
    }

    /**
     * Return an estimate for the volume ceiling (quietest volume which will be heard 100% of the
     * time) for the given frequency
     */
    protected double getVolCeilingEstimate(float freq) {
        return 0.0; // todo
    }

    /**
     * Set this results container such that all subsequent calls to getProbability will be
     * calculated using the equation represented by the given ID
     *
     * @param equationID The id of the desired equation
     * @throws IllegalArgumentException If an invalid ID is given (see EQUATION_ID_NUMS)
     */
    public void setModelEquation(int equationID) throws IllegalArgumentException {
        for (int ID : EQUATION_ID_NUMS) if (equationID == ID) {
            this.equationID = equationID;
            return;
        }
        throw new IllegalArgumentException("Invalid equation ID number");
    }


    /**
     * Add a single trial result to these test results
     *
     * @param freq The frequency of the trial
     * @param vol1 The first volume selected by the user
     * @param vol2 The second volume selected by the user
     */
    public void addResult(float freq, double vol1, double vol2) {
        allResults.put(freq, new VolPair(vol1, vol2));
    }

    private class VolPair {

        private final double vol1;
        private final double vol2;

        public VolPair(double vol1, double vol2) {
            this.vol1 = vol1;
            this.vol2 = vol2;
        }

        public double vol1() {
            return vol1;
        }

        public double vol2() {
            return vol2;
        }
    }
}
