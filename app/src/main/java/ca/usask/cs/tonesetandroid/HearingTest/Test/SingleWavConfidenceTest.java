package ca.usask.cs.tonesetandroid.HearingTest.Test;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public class SingleWavConfidenceTest extends ConfidenceTest<WavTone> {

                                                    //        F4       C5       B5        G6        A7
    private static final float[] DEFAULT_WAV_FREQUENCIES = {349.23f, 523.25f, 987.77f, 1567.98f, 3520.0f};

    /**
     * Tones to play in sampleTones. Configure this in configureTestTones
     */
    private ArrayList<WavTone> sampleTones;

    public SingleWavConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
        super(results, noiseType);
    }

    /**
     * @throws IllegalArgumentException If frequencies[] contains a value that does not have a default wav resource
     * (see WavTone constructors)
     */
    @Override
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies)
            throws IllegalArgumentException {
        // todo
    }

    @Override
    public void initialize() {
        this.initialize(DEFAULT_TRIALS_PER_TONE, DEFAULT_VOLS_PER_FREQ, DEFAULT_WAV_FREQUENCIES);
    }

    @Override
    public Runnable sampleTones() {
        if (this.sampleTones.isEmpty()) throw new IllegalStateException("No tones to sample");
        else return new Runnable() {
            @Override
            public void run() {
                if (! iModel.sampleThreadActive())
                    try {
                        iModel.setSampleThreadActive(true);
                        for (WavTone tone : sampleTones) {
                            if (!iModel.testPaused()) return; // stop if user un-pauses
                            playTone(tone);
                            sleepThread(500, 500);
                        }

                    } finally {
                        iModel.setSampleThreadActive(false);
                    }
            }
        };
    }

    @Override
    protected void playTone(WavTone tone) {
        this.playWav(tone);
    }

    @Override
    protected boolean wasCorrect() {
        return iModel.answered();
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
