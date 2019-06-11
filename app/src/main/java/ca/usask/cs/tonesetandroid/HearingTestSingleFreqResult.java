package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HearingTestSingleFreqResult {

    private float freq;

    private HashMap<Double, Integer> timesHeardPerVol;

    private HashMap<Double, Integer> timesNotHeardPerVol;

    public HearingTestSingleFreqResult(float freq) {
        this.freq = freq;
        this.timesHeardPerVol = new HashMap<>();
        this.timesNotHeardPerVol = new HashMap<>();
    }

    public void addResult(double vol, boolean heard) {
        if (heard)
            if (timesHeardPerVol.containsKey(vol))
                mapReplace(timesNotHeardPerVol, vol, timesNotHeardPerVol.get(vol) + 1);
            else timesHeardPerVol.put(vol, 1);
        else
            if (timesNotHeardPerVol.containsKey(vol))
                mapReplace(timesNotHeardPerVol, vol, timesNotHeardPerVol.get(vol) + 1);
            else timesNotHeardPerVol.put(vol, 1);
    }

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
        ArrayList<Double> testedVolumesSorted = new ArrayList<>(this.timesHeardPerVol.keySet());
        Collections.sort(testedVolumesSorted);

        builder.append("Frequency: ");
        builder.append(this.freq);
        builder.append('\n');
        for (Double d : testedVolumesSorted) {
            int timesHeard = this.timesHeardPerVol.get(d);
            int timesNotHeard = this.timesNotHeardPerVol.get(d);
            float pHeard = (float) timesHeard / (float) (timesHeard + timesNotHeard);
            builder.append("Volume ");
            builder.append(d);
            builder.append(": P(heard) = ");
            builder.append(String.format("%.2f\n", pHeard));
        }
        return builder.toString();
    }

    /**
     * Delete any mapping from the given key if it exists, then add a mapping between the given key and the new value
     * in the given map
     *
     * @param map The map to be affected
     * @param key The key whose value is to be replaced
     * @param newValue The new value to associate with the key
     */
    public static void mapReplace(HashMap<Double, Integer> map, Double key, Integer newValue) {
        map.remove(key);
        map.put(key, newValue);
    }

    public static void mapReplace(HashMap<Float, Integer> map, Float key, Integer newValue) {
        map.remove(key);
        map.put(key, newValue);
    }

    /**
     * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being greater than f
     */
    public static double findNearestAbove(double v, Double[] lst) {
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

    /**
     * Given a list of freqvolpairs, return the frequency of the freqvolpair closest to f while being less than f
     */
    public static double findNearestBelow(double v, Double[] lst) {
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
