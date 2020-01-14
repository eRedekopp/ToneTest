package ca.usask.cs.tonesetandroid.HearingTest.Container;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolDurTrio;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Interval;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;
import ca.usask.cs.tonesetandroid.MainActivity;
import ca.usask.cs.tonesetandroid.Control.Model;
import ca.usask.cs.tonesetandroid.UtilFunctions;

/**
 * A container class for storing the results of a CalibrationTest
 */
public class CalibrationTestResults extends PredictorResults {

    /**
     * Each frequency tested mapped to its corresponding SingleFreqResult
     */
    private HashMap<Float, HearingTestSingleFreqResult> allResults;

    public CalibrationTestResults(BackgroundNoiseType backgroundNoise, String testTypeName) {
        super(backgroundNoise, testTypeName);
        allResults = new HashMap<>();
    }

    /**
     * Add the result of a single CalibrationTest trial to these results
     *
     * @param tone The tone played in the new trial
     * @param heard Was the trial heard?
     */
    @SuppressWarnings("ConstantConditions")
    public void addResult(Tone tone, boolean heard) {
        try {
            allResults.get(tone.freq()).addResult(tone.vol(), heard);
        } catch (NullPointerException e) {
            HearingTestSingleFreqResult r = new HearingTestSingleFreqResult(tone.freq());
            r.addResult(tone.vol(), heard);
            allResults.put(tone.freq(), r);
        }
    }

    @Override
    public boolean isEmpty() {
        return allResults.isEmpty();
    }

    /**
     * The Tone method uses simple linear estimation using the Tone's freq() and vol() values and nothing else. Use
     * the overloaded methods for specific tones
     *
     * @return  the model's estimate of the probability of hearing the given tone, given these calibration results
     */
    @Override
    public double getProbability(Tone tone) throws IllegalStateException {
        Float[] testedFreqs = this.getTestedFreqs();
        float[] testedFreqsPrimitive = new float[testedFreqs.length];  // JAVA WHY???
        for (int i = 0; i < testedFreqs.length; i++) testedFreqsPrimitive[i] = testedFreqs[i];
        return getProbability(tone.freq(), tone.vol(), testedFreqsPrimitive);
    }

    protected double getProbability(Interval tone) throws IllegalStateException {
        double f1Prob = getProbability(new FreqVolPair(tone.freq(), tone.vol()));
        double f2Prob = getProbability(new FreqVolPair(tone.freq2(), tone.vol()));
        return UtilFunctions.mean(new double[]{f1Prob, f2Prob});
    }

    protected double getProbability(Melody tone) {
        FreqVolDurTrio[] tones = tone.getAudibleTones();
        double[] probs = new double[tones.length];
        for (int i = 0; i < tones.length; i++) probs[i] = this.getProbability(tones[i]);
        return UtilFunctions.mean(probs);
    }

    protected double getProbability(WavTone tone) {
        // find most prominent frequencies in audio samples from wav file, return mean of their probabilities

        int nAudioSamples = 50;
        int nFreqsPerSample = 3;  // top 3 frequencies in every sample

        float[][] topFreqs = Model.topNFrequencies(tone.wavID(), nAudioSamples, 3);

        ArrayList<Number> probEstimates = new ArrayList<>();

        for (int i = 0; i < nAudioSamples; i++)
            for (int j = 0; j < nFreqsPerSample; j++)
                if (topFreqs[i] != null && topFreqs[i][j] > 100)
                    probEstimates.add(getProbability(new FreqVolPair(topFreqs[i][j], tone.vol())));

        return UtilFunctions.mean(probEstimates);
    }

    /**
     * Given a subset of the tested frequencies, return the probability of hearing a tone of the given frequency and
     * volume, as though the frequencies in subset
     *
     * @param freq The frequency of the tone whose probability is to be determined
     * @param vol The volume of the tone whose probability is to be determined
     * @param subset A subset of the calibration frequencies to be used to generate the model
     * @return The estimated probability of hearing the tone, based on the given subset
     * @throws IllegalArgumentException If the given subset is not a subset of the tested frequencies
     */
    @SuppressWarnings("ConstantConditions")
    public float getProbability(float freq, double vol, float[] subset) throws IllegalArgumentException {
        Float[] subsetAsObj = new Float[subset.length];
        for (int i = 0; i < subset.length; i++)
            if (! this.freqTested(subset[i]))
                throw new IllegalArgumentException("Subset argument must be a subset of tested frequencies");
            else subsetAsObj[i] = subset[i];

        // find subset frequencies just above and below requested frequency
        float freqAbove = UtilFunctions.findNearestAbove(freq, subsetAsObj);
        float freqBelow = UtilFunctions.findNearestBelow(freq, subsetAsObj);

        // if freq is higher than the highest or lower than the lowest, return the probability of the nearest
        if (freqAbove == -1) return this.allResults.get(freqBelow).getProbOfHearing(vol);
        if (freqBelow == -1) return this.allResults.get(freqAbove).getProbOfHearing(vol);

        // find probabilities of each of these frequencies
        float probAbove = this.allResults.get(freqAbove).getProbOfHearing(vol);
        float probBelow = this.allResults.get(freqBelow).getProbOfHearing(vol);

        // how far of the way between freqBelow and freqAbove is freq?
        float pctBetween = (freq - freqBelow) / (freqAbove - freqBelow);

        // estimate this probability linearly between the results above and below
        return probBelow + pctBetween * (probAbove - probBelow);
    }

    /**
     * Returns a mapping of volumes to the number of times each volume was heard in the test for the given frequency
     *
     * @param freq The frequency whose volume results are to be returned
     * @return A mapping of volumes to the number of times they were heard
     */
    @SuppressWarnings("ConstantConditions")
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
    @SuppressWarnings("ConstantConditions")
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
    @SuppressWarnings("ConstantConditions")
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

    /**
     * Is there a result stored in this container for the given frequency?
     */
    public boolean freqTested(float freq) {
        return allResults.containsKey(freq);
    }

    /**
     * Returns an estimate of the highest volume which has a "0%" chance of 
     * being heard for the given frequency
     *
     * @param freq The frequency whose volume floor is to be estimated
     * @return An estimate of the volume floor for the given frequency
     */
    @SuppressWarnings("ConstantConditions")
    public double getVolFloorEstimate(float freq) {
        if (this.freqTested(freq)) return this.allResults.get(freq).getVolFloor();

        float nearestBelow = UtilFunctions.findNearestBelow(freq, this.getTestedFreqs());
        float nearestAbove = UtilFunctions.findNearestAbove(freq, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return this.allResults.get(this.getNearestTestedFreq(freq)).getVolFloor();

        float pctBetween = (freq - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = this.allResults.get(nearestBelow).getVolFloor();
        double floorAbove = this.allResults.get(nearestAbove).getVolFloor();

        return floorBelow + (floorAbove - floorBelow) * pctBetween;
    }

    /**
     * Returns an estimate of the lowest volume which has a "100%" percent chance of 
     * being heard for the given frequency
     *
     * @param freq The frequency whose volume ceiling is to be estimated
     * @return An estimate of the volume ceiling for the given frequency
     */
    @SuppressWarnings("ConstantConditions")
    public double getVolCeilingEstimate(float freq) {
        if (this.freqTested(freq)) return this.allResults.get(freq).getVolCeiling();

        float nearestBelow = UtilFunctions.findNearestBelow(freq, this.getTestedFreqs());
        float nearestAbove = UtilFunctions.findNearestAbove(freq, this.getTestedFreqs());

        if (nearestAbove == -1 || nearestBelow == -1)
            return this.allResults.get(this.getNearestTestedFreq(freq)).getVolCeiling();

        float pctBetween = (freq - nearestBelow) / (nearestAbove - nearestBelow);
        double floorBelow = this.allResults.get(nearestBelow).getVolCeiling();
        double floorAbove = this.allResults.get(nearestAbove).getVolCeiling();

        return floorBelow + (floorAbove - floorBelow) * pctBetween;
    }

    public double getVolFloorEstimateForEarcon(int wavResId) {

        // find most prominent frequencies in samples of .wav file, return average of their floor estimates

        int nAudioSamples = 50;

        float[] topFreqs = Model.topFrequencies(wavResId, nAudioSamples);
        double[] floorEstimates = new double[nAudioSamples];

        for (int i = 0; i < nAudioSamples; i++) floorEstimates[i] = this.getVolFloorEstimate(topFreqs[i]);

        return UtilFunctions.mean(floorEstimates);
    }

    public double getVolCeilingEstimateForEarcon(int wavResId) {

        // find most prominent frequencies in samples of .wav file, return average of their ceiling estimates

        int nAudioSamples = 50;

        float[] topFreqs = Model.topFrequencies(wavResId, nAudioSamples);
        double[] ceilingEstimates = new double[nAudioSamples];

        for (int i = 0; i < nAudioSamples; i++) ceilingEstimates[i] = this.getVolCeilingEstimate(topFreqs[i]);

        return UtilFunctions.mean(ceilingEstimates);
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
     * Return a new CalibrationTestResults with the same results as this one, but only containing the first n
     * results for each frequency-volume pair (ie. as though Model.NUMBER_OF_TESTS_PER_VOL == n)
     *
     * @param n The number of trials per freq-vol pair in the new container
     * @return A new container containing a subset of this one's results
     * @throws IllegalArgumentException If n is greater than the number of trials performed in this test
     */
    public CalibrationTestResults getSubsetResults(int n) throws IllegalArgumentException {
        if (n > this.getNumOfTrials())
            throw new IllegalArgumentException(
                    "n = " + n + " is larger than the actual sample size = " + this.getNumOfTrials());
        else if (n == this.getNumOfTrials()) return this;

        CalibrationTestResults newContainer = new CalibrationTestResults(this.getNoiseType(), this.getTestTypeName());
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
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("");
        for (HearingTestSingleFreqResult result : allResults.values()) builder.append(result.toString());
        return builder.toString();
    }

    @Override
    public String getPredictionString(Tone tone) {
        return String.format("%s: %.4f",
                this.getTestIdentifier(), this.getFormattedStartTime(), this.getProbability(tone));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A class for storing the hearing test results for a single frequency at multiple volumes
     */
    protected class HearingTestSingleFreqResult {

        /**
         * The frequency of the tones that this result represents
         */
        private final float freq;

        /**
         * Each volume at which this frequency was tested mapped to the number of trials in which it was heard
         */
        private HashMap<Double, Integer> timesHeardPerVol;  // do not mutate these maps except in addResult()

        /**
         * Each volume at which this frequency was tested mapped to the number of trials in which it was not heard
         */
        private HashMap<Double, Integer> timesNotHeardPerVol;

        /**
         * Each volume at which this frequency was tested mapped to a list containing the user's responses to each
         * trial in order
         */
        private HashMap<Double, List<Boolean>> testResultsPerVol;

        private HearingTestSingleFreqResult(float freq) {
            this.freq = freq;
            this.timesHeardPerVol = new HashMap<>();
            this.timesNotHeardPerVol = new HashMap<>();
            this.testResultsPerVol = new HashMap<>();
        }

        /**
         * Add a new test result to this SingleFreqResult
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
                    timesHeardPerVol.put(vol, timesHeardPerVol.get(vol) + 1);
                else timesHeardPerVol.put(vol, 1);
            else
                if (timesNotHeardPerVol.containsKey(vol))
                    timesNotHeardPerVol.put(vol, timesNotHeardPerVol.get(vol) + 1);
                else timesNotHeardPerVol.put(vol, 1);
        }

        /**
         * Get the probability of hearing a tone of this.freq Hz at the given volume - estimates 
         * linearly between the two nearest volumes
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
                volBelow = UtilFunctions.findNearestBelow(vol, this.getVolumes());
            } catch (IllegalArgumentException e) {
                return this.getActualProb(UtilFunctions.findNearestAbove(vol, this.getVolumes()));
            }
            try {
                volAbove = UtilFunctions.findNearestAbove(vol, this.getVolumes());
            } catch (IllegalArgumentException e) {
                return this.getActualProb(UtilFunctions.findNearestBelow(vol, this.getVolumes()));
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
            ListIterator<Boolean> iter = null;
            HearingTestSingleFreqResult newResult = null;
            newResult = new HearingTestSingleFreqResult(this.freq);
            for (Double vol : this.getVolumes()) {
                // addResult for the first n responses in the hearing test
                iter  = this.testResultsPerVol.get(vol).listIterator();
                    for (int i = 0; i < n; i++) {
                        try {
                            newResult.addResult(vol, iter.next());
                        } catch (NoSuchElementException e) {
                            break;  // if not enough trials for volume, use as many samples
                                    // as possible
                        }
                    }
            }
            return newResult;
        }

        @Override
        @NonNull
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
                builder.append(String.format("%.4f", d));
                builder.append(": P(heard) = ");
                builder.append(String.format("%.4f\n", pHeard));
            }
            return builder.toString();
        }

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesHeardPerVol.clone();
        }

        @SuppressWarnings("unchecked")
        public HashMap<Double, Integer> getTimesNotHeardPerVol() {
            return (HashMap<Double, Integer>) this.timesNotHeardPerVol.clone();
        }

        /**
         * @return a Collection containing all volumes that were tested at this frequency
         */
        public Collection<Double> getVolumes() {
            return this.testResultsPerVol.keySet();
        }
    }
}
