package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import java.util.ArrayList;
import java.util.Arrays;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

public abstract class WavConfidenceTest extends ConfidenceTest<WavTone> {

                                                      //        F4       C5       B5        G6        A7
    protected static final float[] DEFAULT_WAV_FREQUENCIES = {349.23f, 523.25f, 987.77f, 1567.98f, 3520.0f};

    /**
     * Tones to play in sampleTones. Configure this in configureTestTones
     */
    protected ArrayList<WavTone> sampleTones;

    public WavConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
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

    /**
     * Overridden method adds wav-analysis estimates
     */
    @Override
    public String summaryStatsAsString() {
        StringBuilder builder = new StringBuilder();
        RampTestResults regularResults = model.getRampResults().getRegularRampResults();
        SingleToneResult[] confResults = this.getConfResults();

        for (int n = 1; n <= model.getNumCalibrationTrials(); n++) {

            builder.append("########## n = ");
            builder.append(n);
            builder.append(" ##########\n");
            /*
                toneProbability : probability from calibration model using only freq and volume
                wavProbability : probability from calibration model using wav resource
                rampProbs:
                    WithReduceData : generates guesses using vol floor data from reduce test
                    WithoutReduceData : not that ^^
                    FreqVol : calculates based only on freq and vol
                    Wav     : calculates based on wav resource
                    Linear : Calculates probabilities on linear scale
                    Log    : Calculates probabilities on logarithmic scale
             */
            builder.append( "Tone confidenceProbability toneProbability " +
                    "wavProbability rampProbabilityFreqVolLinearWithReduceData " +
                    "rampProbabilityFreqVolLinearWithoutReduceData rampProbabilityFreqVolLogWithReduceData " +
                    "rampProbabilityFreqVolLogWithoutReduceData rampProbabilityWavLinearWithReduceData " +
                    "rampProbabilityWavLinearWithoutReduceData rampProbabilityWavLogWithReduceData " +
                    "rampProbabilityWavLogWithoutReduceData\n");

            for (SingleToneResult result : confResults) {

                double confProb = (double) result.getCorrect() / (result.getCorrect() + result.getIncorrect()),
                        toneProb, wavProb, rampProbFVLinFloor, rampProbFVLinReg, rampProbFVLogFloor, rampProbFVLogReg,
                        rampProbWavLinFloor, rampProbWavLinReg, rampProbWavLogFloor, rampProbWavLogReg;

                // get all 4 ramp estimates
                WavTone t = (WavTone) result.tone;
                regularResults.setModelEquation(0);
                model.getRampResults().setModelEquation(0);
                rampProbFVLinFloor = model.getRampProbability((Tone) t, true);
                rampProbFVLinReg   = model.getRampProbability((Tone) t, false);
                rampProbWavLinFloor = model.getRampProbability(t, true);
                rampProbWavLinReg   = model.getRampProbability(t, false);
                regularResults.setModelEquation(1);
                model.getRampResults().setModelEquation(1);
                rampProbFVLogFloor = model.getRampProbability((Tone) t, true);
                rampProbFVLogReg   = model.getRampProbability((Tone) t, false);
                rampProbWavLogFloor = model.getRampProbability(t, true);
                rampProbWavLogReg   = model.getRampProbability(t, false);

                // get calibration estimates
                toneProb = model.getCalibProbability((Tone) t, n);
                wavProb  = model.getCalibProbability(t, n);

                builder.append(String.format("%s %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f %.4f%n",
                        t.toString(), confProb, toneProb, wavProb, rampProbFVLinFloor, rampProbFVLinReg,
                        rampProbFVLogFloor, rampProbFVLogReg, rampProbWavLinFloor, rampProbWavLinReg,
                        rampProbWavLogFloor, rampProbWavLogReg));
            }
        }

        return builder.toString();
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }
}
