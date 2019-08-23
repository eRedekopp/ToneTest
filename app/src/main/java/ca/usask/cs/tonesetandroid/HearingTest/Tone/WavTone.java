package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import ca.usask.cs.tonesetandroid.R;

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

    /**
     * Pre-set constructor that automatically selects a resource ID for the given frequency, provided that there is a
     * default wav ID for that frequency
     *
     * @param freq The frequency of the WavTone (Must be one of 349.23, 523.25, 987.77, 1567.98, 3520.00)
     * @param vol The volume of the WavTone
     * @throws IllegalArgumentException If freq does not have a default wav resource associated with it
     */
    public WavTone(float freq, double vol) throws IllegalArgumentException {
        switch ((int) freq) {
            case 349:  // F4 ~= 349 Hz
                this.wavID = R.raw.f4piano;
                break;
            case 523:  // C5 ~= 523 Hz
                this.wavID = R.raw.c5piano;
                break;
            case 987:  // B5 ~= 987 Hz
                this.wavID = R.raw.b5piano;
                break;
            case 1567: // G6 ~= 1.567 kHz
                this.wavID = R.raw.g6piano;
                break;
            case 3520: // A7 ~= 3.52 kHz
                this.wavID = R.raw.a7piano;
                break;
            default:
                throw new IllegalArgumentException("No default resource id for frequency: " + freq);
        }
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
    public WavTone newVol(double vol) {
        return new WavTone(this.wavID, this.freq, vol);
    }

    public int wavID() {
        return wavID;
    }
}
