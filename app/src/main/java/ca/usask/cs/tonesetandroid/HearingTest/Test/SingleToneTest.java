package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Parent class for all HearingTests that one individual Tone at a time (ie. all except RampTest)
 */
public abstract class SingleToneTest<T extends Tone> extends HearingTest<T> {

    /**
     * All tones to be tested, with duplicates for each individual time it is to be played
     */
    protected ArrayList<T> testTones;

    /**
     * An iterator for testTones to keep track of the current trial
     */
    protected ListIterator<T> position;

    /**
     * Play a tone of the appropriate type for this calibration test
     */
    protected abstract void playTone(T tone);

    /**
     * Prepare for this test to be run 
     */
    public abstract void initialize();

    public SingleToneTest(BackgroundNoiseType noiseType) {
        super(noiseType);
    }
}
