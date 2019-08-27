package ca.usask.cs.tonesetandroid.HearingTest.Container;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolDurTrio;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.SinglePitchTone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;
import ca.usask.cs.tonesetandroid.UtilFunctions;

public class RampTestResults implements HearingTestResults {

    public static final int[] EQUATION_ID_NUMS = {0, 1};

    protected int equationID = 0;

    /**
     * A mapping of each frequency tested to the first and second volumes selected by the user at
     * that frequency
     */
    protected HashMap<Float, VolPair> allResults;

    public RampTestResults() {
        this.allResults = new HashMap<>();
    }

    @Override
    public double getProbability(Tone tone) throws IllegalStateException {
        if (equationID == 0) return getProbabilityLinear(tone);
        if (equationID == 1) return getProbabilityLogarithmic(tone);
        else throw new IllegalStateException("Equation ID set to invalid value: " + equationID);
    }

    @Override
    public double getProbability(SinglePitchTone tone) throws IllegalStateException {
        return getProbability((Tone) tone);
    }

    @Override
    public double getProbability(Interval tone) throws IllegalStateException {
        double f1Prob = this.getProbability(new FreqVolPair(tone.freq(), tone.vol()));
        double f2Prob = this.getProbability(new FreqVolPair(tone.freq2(), tone.vol()));
        return UtilFunctions.mean(new double[]{f1Prob, f2Prob});
    }

    @Override
    public double getProbability(Melody tone) {
        FreqVolDurTrio[] tones = tone.getTones();
        double[] probs = new double[tones.length];
        for (int i = 0; i < tones.length; i++) probs[i] = this.getProbability(tones[i]);
        return UtilFunctions.mean(probs);
    }

    @Override
    public double getProbability(WavTone tone) {
        return getProbability((Tone) tone);
    }

    /**
     * Return the probability of hearing the tone given these hearing test results, using the
     * modeling equation y = x, where x is the estimated distance between "perfectly audible"
     * and "perfectly inaudible" volumes for tone.freq()
     *
     * @param tone The tone whose probability is to be determined
     * @return The probability of hearing the tone, modeling using the equation y = x
     * @throws IllegalStateException If there are no results stored in this container
     */
    protected double getProbabilityLinear(Tone tone) throws IllegalStateException {
        // get floor/ceiling estimates
        double volFloor = this.getVolFloorEstimate(tone.freq());
        double volCeiling = this.getVolCeilingEstimate(tone.freq());

        // Return 1 or 0 if above/below ceiling/floor
        if (tone.vol() < volFloor) return 0.0;
        if (tone.vol() > volCeiling) return 1.0;

        // Return the percentage of the way between floor and ceiling that tone.vol() is
        return (tone.vol() - volFloor) / (volCeiling - volFloor);
    }

    /**
     * Return the probability of hearing the tone given these hearing test results, using the
     * modeling equation y = ln((e - 1)x + 1), where x is the estimated distance between "perfectly audible"
     * and "perfectly inaudible" volumes for tone.freq()
     *
     * @param tone The tone whose probability is to be determined
     * @return The probability of hearing the tone, modeling using the equation y = ln((e - 1)x + 1)
     * @throws IllegalStateException If there are no results stored in this container
     */
    protected double getProbabilityLogarithmic(Tone tone) throws IllegalStateException {
        // get floor/ceiling estimates
        double volFloor = this.getVolFloorEstimate(tone.freq());
        double volCeiling = this.getVolCeilingEstimate(tone.freq());

        // Return 1 or 0 if above/below ceiling/floor
        if (tone.vol() < volFloor) return 0.0;
        if (tone.vol() > volCeiling) return 1.0;

        // How much of the way between floor and ceiling is tone.vol()?
        double pctBetween = (tone.vol() - volFloor) / (volCeiling - volFloor);

        // Return
        return Math.log((Math.E - 1) * pctBetween + 1);
    }

    /**
     * Return an estimate for the volume floor (loudest volume which will be heard 0% of the
     * time) for the given frequency - must have results stored to call this method
     */
    protected double getVolFloorEstimate(float freq) {
        return this.getVolCeilingEstimate(freq) / 2.0;  // floor tends to be about 1/2 ceiling
    }

    /**
     * Return an estimate for the volume ceiling (quietest volume which will be heard 100% of the
     * time) for the given frequency - must have results stored to call this method
     */
    @SuppressWarnings("ConstantConditions")
    protected double getVolCeilingEstimate(float freq) {

        // Return max ramp vol if freq tested
        if (this.allResults.containsKey(freq)) return this.allResults.get(freq).min();

        // If freq is higher than highest or lower than lowest, return max ramp vol of nearest
        float maxFreq = Collections.max(this.getTestedFreqs()),
              minFreq = Collections.min(this.getTestedFreqs());
        if (freq > maxFreq) return this.allResults.get(maxFreq).min();
        if (freq < minFreq) return this.allResults.get(minFreq).min();

        // Get max ramp vols of nearest tested frequencies
        float freqAbove = UtilFunctions.findNearestAbove(freq, this.getTestedFreqs());
        float freqBelow = UtilFunctions.findNearestBelow(freq, this.getTestedFreqs());

        // How far between freqBelow and freqAbove is freq?
        double pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // Estimate vol ceiling linearly
        double volBelow = this.allResults.get(freqBelow).min();
        double volAbove = this.allResults.get(freqAbove).min();

        return volBelow + pctBetween * (volAbove - volBelow);
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

    public Collection<Float> getTestedFreqs() {
        return this.allResults.keySet();
    }

    @SuppressWarnings("ConstantConditions")
    public FreqVolPair[] getResultsArray() {
        FreqVolPair[] outArr = new FreqVolPair[this.allResults.size()];
        int i = 0;
        for (float freq : this.allResults.keySet()) {
            outArr[i++] = new FreqVolPair(freq, allResults.get(freq).vol2());
        }
        return outArr;
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

    @Override
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (float freq : this.getTestedFreqs())
            builder.append(String.format("Freq: %.1f, vol1 = %.3f, vol2 = %.3f%n",
                    freq, this.allResults.get(freq).vol1(), this.allResults.get(freq).vol2()));
        return builder.toString();
    }

    @Override
    public boolean isEmpty() {
        return this.allResults.isEmpty();
    }

    protected class VolPair {

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

        /**
         * @return max(vol1, vol2)
         */
        public double max() {
            return Math.max(vol1, vol2);
        }

        /**
         * @return min(vol1, vol2)
         */
        public double min() {
            return Math.min(vol1, vol2);
        }
    }
}
