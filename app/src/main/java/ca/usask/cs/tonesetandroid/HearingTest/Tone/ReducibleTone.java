package ca.usask.cs.tonesetandroid.HearingTest.Tone;

/**
 * A tone which can be used in a ReduceTest
 */
public abstract class ReducibleTone extends Tone {

    /**
     * @return A new ReducibleTone which is identical to this one except the volume is set to the given volume
     */
    public abstract ReducibleTone newVol(double vol);
}
