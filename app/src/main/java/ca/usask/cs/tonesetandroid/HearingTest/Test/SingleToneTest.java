package ca.usask.cs.tonesetandroid.HearingTest.Test;

/**
 * Parent class for all HearingTests that one individual Tone at a time (ie. all except RampTest)
 */
public abstract class SingleToneTest<T extends Tone> extends HearingTest<T> {  // todo make others extend this

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
     * Populate testTones with a Tone for each individual trial that will be performed in this test
     *
     * @param rampResults The results of the ramp test run previous to this calibration test
     * @param reduceResults The results of the reduce test run previous to this calibration test
     * @param nVolsPerFreq The number of volumes at which to test each frequency
     * @param nTrialsPerVol The number of times to test each tone
     */
    protected abstract void configureTestTones(RampTestResults rampResults,
                                            FreqVolPair[] reduceResults,
                                            int nVolsPerFreq,
                                            int nTrialsPerVol);

    /**
     * Prepare for this test to be run 
     */
    public void initialize();
    
}
