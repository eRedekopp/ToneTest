package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import java.util.ArrayList;
import java.util.Arrays;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class WavConfidenceTest extends ConfidenceTest<WavTone> {

                                                      //        F4       G5       B5        G6        A7
    protected static final float[] DEFAULT_WAV_FREQUENCIES = {349.23f, 783.99f, 987.77f, 1567.98f, 3520.0f};

    /**
     * Tones to play in sampleTones. Configure this in configureTestTones
     */
    protected ArrayList<WavTone> sampleTones;

    public WavConfidenceTest(BackgroundNoiseType noiseType) {
        super(noiseType);
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
                            playTone(tone.newVol(1000));
                            sleepThread(500, 500);
                        }
                    } finally {
                        iModel.setSampleThreadActive(false);
                    }
            }
        };
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        return String.format("freq: %.2f, vol: %.2f, %s, %d clicks: %s",
                trial.tone().freq(), trial.tone().vol(), trial.wasCorrect() ? "Correct" : "Incorrect", trial.nClicks(),
                Arrays.toString(trial.clickTimes()));
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
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_HEARD};
    }
}
