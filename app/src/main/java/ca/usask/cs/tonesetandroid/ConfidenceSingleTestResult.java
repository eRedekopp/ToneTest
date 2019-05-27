package ca.usask.cs.tonesetandroid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A class to store the result of a single freq-vol pair test from a confidence test
 *
 * @author redekopp
 */
public class ConfidenceSingleTestResult {
    private double frequency, expectedVol, actualVol, volRatio;
    private boolean expectedResult, actualResult;
    private List<FreqVolPair> calibPairs;

    /**
     * Create a new single test result object
     *
     * @param pair The frequency tested and the volume at which it was tested
     * @param expectedVol The expected "just audible" volume of the frequency tested
     * @param actualResult The result of the test (True if heard, false if not)
     * @param calibPairs The frequency-volume pairs used to calibrate for this test
     */
    public ConfidenceSingleTestResult(FreqVolPair pair, double expectedVol, boolean actualResult,
                                      List<FreqVolPair> calibPairs) {
        actualVol = pair.vol;
        this.expectedVol = expectedVol;
        frequency = pair.freq;
        expectedResult = actualVol >= expectedVol;
        this.actualResult = actualResult;
        volRatio = round(actualVol / expectedVol, 3); // to avoid rounding errors
        this.calibPairs = calibPairs;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getExpectedVol() {
        return expectedVol;
    }

    public double getActualVol() {
        return actualVol;
    }

    public boolean getExpectedResult() {
        return expectedResult;
    }

    public boolean getActualResult() {
        return actualResult;
    }

    public double getVolRatio() {
        return volRatio;
    }

    public List<FreqVolPair> getCalibPairs() {
        return calibPairs;
    }

    /**
     * Round a double to the requested number of decimal places. Taken from StackOverflow user Jonik
     * -- because the vol ratio calculation is prone to rounding errors
     *
     * @param value The value to be rounded
     * @param places The number of places to round to
     * @return The rounded number
     */
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
