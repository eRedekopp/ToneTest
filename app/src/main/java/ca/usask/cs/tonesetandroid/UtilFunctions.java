package ca.usask.cs.tonesetandroid;

import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Static global utility functions
 */
public final class UtilFunctions {

    /**
     * Given a collection of doubles, find the double nearest to d which is also less than d
     * Only intended for use with collections of positive numbers
     * @throws IllegalArgumentException if d is smaller than the smallest element of the collection
     */
    public static double findNearestBelow(double d, Collection<Double> dbls) {
        double closest = -1.0;
        double distance = Double.MAX_VALUE;
        for (double dbl : dbls) {
            if (0 < d - dbl && d - dbl < distance) {
                closest = dbl;
                distance = d - dbl;
            }
        }
        if (closest == -1) throw new IllegalArgumentException("No elements less than d found");
        else return closest;
    }

    /**
     * Given a collection of doubles, find the double nearest to d which is also greater than d
     * Only intended for use with collections of positive numbers
     * @throws IllegalArgumentException if d is larger than the largest element of the collection
     */
    public static double findNearestAbove(double d, Collection<Double> dbls) throws IllegalArgumentException {
        double closest = -1.0;
        double distance = Double.MAX_VALUE;
        for (double dbl : dbls) {
            if (0 < dbl - d && dbl - d < distance) {
                closest = dbl;
                distance = dbl - d;
            }
        }
        if (closest == -1) throw new IllegalArgumentException("No elements greater than d found");
        else return closest;
    }

    /**
     * Given a list of floats, return the number closest to f while being greater than f, or -1 if none found
     */
    public static float findNearestAbove(float f, Float[] lst) {
        float closest = -1;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < freq - f && freq - f < distance) {
                closest = freq;
                distance = freq - f;
            }
        }
        return closest;
    }

    public static float findNearestAbove(float f, Collection<Float> lst) {
        float closest = -1;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < freq - f && freq - f < distance) {
                closest = freq;
                distance = freq - f;
            }
        }
        return closest;
    }

    /*    public static double findNearestAbove(double d, Collection<Double> dbls) throws IllegalArgumentException {
        double closest = -1.0;
        double distance = Double.MAX_VALUE;
        for (double dbl : dbls) {
            if (0 < dbl - d && dbl - d < distance) {
                closest = dbl;
                distance = dbl - d;
            }
        }
        if (closest == -1) throw new IllegalArgumentException("No elements greater than d found");
        else return closest;
    }*/


    public static Tone findNearestAbove(float f, Tone[] lst) {
        Tone closest = null;
        double distance = Double.MAX_VALUE;
        for (Tone t : lst) {
            float freq = t.freq();
            if (0 < freq - f && freq - f < distance) {
                closest = t;
                distance = freq - f;
            }
        }
        return closest;
    }

    public static Tone findNearestBelow(float f, Tone[] lst) {
        Tone closest = null;
        double distance = Double.MAX_VALUE;
        for (Tone t : lst) {
            float curFloat = t.freq();
            if (0 < f - curFloat && f - curFloat < distance) {
                closest = t;
                distance = f-curFloat;
            }
        }
        return closest;
    }

    /**
     * Given a list of floats, return the number closest to f while being less than f, or -1 if none found
     */
    public static float findNearestBelow(float f, Float[] lst) {
        float closest = -1f;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < f - freq && f - freq < distance) {
                closest = freq;
                distance = f - freq;
            }
        }
        return closest;
    }

    public static float findNearestBelow(float f, Collection<Float> lst) {
        float closest = -1f;
        float distance = Float.MAX_VALUE;
        for (float freq : lst) {
            if (0 < f - freq && f - freq < distance) {
                closest = freq;
                distance = f - freq;
            }
        }
        return closest;
    }

    /**
     * @param arr An array of doubles
     * @return The mean of the array - does not include any values under 100
     */
    public static double mean(double[] arr) {
        double total = 0;
        for (double d : arr) total += d;
        return total / arr.length;
    }

    public static double mean(List<Number> lst) {
        double total = 0.0;
        for (Number n : lst) total = n.doubleValue() + total;
        return total / lst.size();
    }

    public static boolean contains(float[] lst, float item) {
        for (float f : lst) if (f == item) return true;
        return false;
    }

    public static boolean contains(Tone[] lst, Tone item) {
        for (Tone t : lst) if (t.equals(item)) return true;
        return false;
    }

    /**
     * @return true if lst contains a tone with the given frequency
     */
    public static boolean contains(Tone[] lst, float freq) {
        for (Tone t : lst) if (t.freq() == freq) return true;
        return false;
    }

    /**
     * @return The first Tone in lst with the given frequency, or null if none found
     */
    public static Tone get(Tone[] lst, float freq) {
        for (Tone t : lst) if (t.freq() == freq) return t;
        return null;
    }

}
