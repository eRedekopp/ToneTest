package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class SingleWavConfidenceTest extends ConfidenceTest<WavTone> {

                                                      //        F4       C5       B5        G6        A7
    protected static final float[] DEFAULT_WAV_FREQUENCIES = {349.23f, 523.25f, 987.77f, 1567.98f, 3520.0f};

    /**
     * Tones to play in sampleTones. Configure this in configureTestTones
     */
    protected ArrayList<WavTone> sampleTones;

    public SingleWavConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
        super(results, noiseType);
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
    protected double getModelCalibProbability(WavTone tone, int n) {
        return model.getCalibProbability(tone, n);
    }

    @Override
    protected double getModelRampProbability(WavTone tone, boolean withFloorResults) {
        return model.getRampProbability(tone, withFloorResults);
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
