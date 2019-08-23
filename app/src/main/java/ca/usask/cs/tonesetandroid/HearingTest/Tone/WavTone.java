package ca.usask.cs.tonesetandroid.HearingTest.Tone;

/**
 * A class for a tone whose audio data is stored in a .wav file
 */
public class WavTone extends ReducibleTone {

    float freq;

    double vol;

    /**
     * The android resource ID of the wav file associated with this WavTone
     */
    private final int wavID;

    public WavTone(int wavResourceID, float freq, double vol) {
        this.wavID = wavResourceID;
        this.freq = freq;
        this.vol = vol;
    }

    @Override
    public double vol() {
        return this.vol;
    }

    @Override
    public float freq() {
        return this.freq;
    }

    @Override
    public ReducibleTone newVol(double vol) {
        return new WavTone(this.wavID, this.freq, vol);
    }

    public int wavID() {
        return wavID;
    }
}
