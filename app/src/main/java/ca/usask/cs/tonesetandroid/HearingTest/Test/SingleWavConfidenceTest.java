package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
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
        this.testTypeName = "piano-single-tone-conf";
    }

    /**
     * @throws IllegalArgumentException If frequencies[] contains a value that does not have a default wav resource
     * (see WavTone constructors)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies)
            throws IllegalArgumentException {

        if (frequencies.length == 0) return;

        // create a list containing one copy of freq for each volume at which freq should be tested
        this.testTones = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (int i = 0; i < volsPerFreq; i++) for (float freq : frequencies) confFreqs.add(freq);
        Collections.shuffle(confFreqs);

        testTones.add(new WavTone(  // add a test that will likely be heard every time
                confFreqs.get(0),
                this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(0))
        ));

        if (frequencies.length > 1)
            testTones.add(new WavTone(  // add a test that will likely not be heard at all
                    confFreqs.get(1),
                    this.calibResults.getVolFloorEstimateForFreq(confFreqs.get(1))
            ));

        if (frequencies.length > 2)
            testTones.add(new WavTone(  // add a test that will very likely be heard every time
                    confFreqs.get(2),
                    this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(2)) * 1.25
            ));

        if (frequencies.length > 3)
            testTones.add(new WavTone(  // add a test that will extremely likely be heard every time
                    confFreqs.get(3),
                    this.calibResults.getVolCeilingEstimateForFreq(confFreqs.get(3)) * 1.5
            ));

        int hardCodedCases = 4; // how many test cases are hard-coded like the ones above?

        // For every other frequency in the list, add a test case where it is somewhere between 40% and 100% of the
        // way between estimates for "completely inaudible" and "completely audible" volumes
        float pct = 0.4f;
        float jumpSize = (1 - pct) / (frequencies.length - hardCodedCases);
        for (int i = hardCodedCases; i < frequencies.length; i++, pct += jumpSize) {
            float freq = confFreqs.get(i);
            double volFloor = this.calibResults.getVolFloorEstimateForFreq(freq);
            double volCeiling = this.calibResults.getVolCeilingEstimateForFreq(freq);
            double testVol = volFloor + pct * (volCeiling - volFloor);
            this.testTones.add(new WavTone(freq, testVol));
        }

        // prepare list of all trials
        this.sampleTones = (ArrayList<WavTone>) this.testTones.clone();
        ArrayList<WavTone> allTrials = new ArrayList<>();
        for (int i = 0; i < trialsPerTone; i++) allTrials.addAll(this.testTones);
        Collections.shuffle(allTrials);
        this.testTones = allTrials;

        if (this.testTones.size() != trialsPerTone * frequencies.length * volsPerFreq)
            Log.w("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length * volsPerFreq +
                    " test pairs but generated " + this.testTones.size());
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
                            playTone(tone.newVol(100));
//                            sleepThread(500, 500);
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
