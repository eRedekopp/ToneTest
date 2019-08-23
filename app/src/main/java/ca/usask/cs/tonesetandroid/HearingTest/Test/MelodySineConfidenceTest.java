package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolDurTrio;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Melody;
import ca.usask.cs.tonesetandroid.UtilFunctions;

/**
 * A hearing test that tests the user hearing Melody objects
 */
public class MelodySineConfidenceTest extends ConfidenceTest<Melody> {

    public static final String TEST_INFO =
            "In this test, short melodies of various frequencies, directions, and volumes will play at random times. " +
            "Please press the \"Up\" button if the melody went upward, \"Down\" if the melody went downward, and " +
            "\"Flat\" if the melody's pitch never changes. Press nothing if you aren't sure.";

    /**
     * Tones to sample in sampleTones() - configure these in configureTestTones along with testPairs
     */
    private ArrayList<Melody> sampleTones;

    public MelodySineConfidenceTest(CalibrationTestResults calibResults, BackgroundNoiseType noiseType) {
        super(calibResults, noiseType);
        this.testTypeName = "melody-sine-conf";
        this.testInfo = TEST_INFO;
        this.GRACE_PERIOD_MS = 1200;    // user gets 1.2 seconds after tone ends to enter a response
        this.MIN_WAIT_TIME_MS = 1500;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) {
        if (frequencies.length == 0) return;

        this.testTones = new ArrayList<>();
        ArrayList<Float> confFreqs = new ArrayList<>();
        for (int i = 0; i < volsPerFreq; i++) for (float freq : frequencies) confFreqs.add(freq);

        // all frequencies tested 3 times: one for each direction
        for (String id : new String[]{"maj-triad-up", "maj-triad-down", "single-freq-rhythm"}) {
            Collections.shuffle(confFreqs);

            testTones.add(new Melody(   // add a melody that will likely be heard every time
                    id,
                    confFreqs.get(0),
                    this.getCeilingEstimateAvg(Melody.getFrequenciesForPreset(id, confFreqs.get(0)))
            ));

            if (frequencies.length > 1) // add a melody that will likely not be heard at all
                testTones.add(new Melody(
                    id,
                    confFreqs.get(1),
                    this.getFloorEstimateAvg(Melody.getFrequenciesForPreset(id, confFreqs.get(1)))
                ));

            if (frequencies.length > 2) // add a melody that will very likely be heard every time
                testTones.add(new Melody(
                    id,
                    confFreqs.get(2),
                    this.getCeilingEstimateAvg(Melody.getFrequenciesForPreset(id, confFreqs.get(2))) * 1.25
                ));

            if (frequencies.length > 3) // add a melody that will extremely likely be heard every time
                testTones.add(new Melody(
                    id,
                    confFreqs.get(3),
                    this.getCeilingEstimateAvg(Melody.getFrequenciesForPreset(id, confFreqs.get(2))) * 1.5
                ));

            int hardCodedCases = 4;  // how many test cases are hard-coded like the ones above?

            // For every other frequency in the list, add a test case where it is somewhere between 40% and 100% of the
            // way between estimates for "completely inaudible" and "completely audible" volumes
            float pct = 0.4f;
            float jumpSize = (1 - pct) / (frequencies.length - hardCodedCases);
            for (int i = hardCodedCases; i < frequencies.length; i++, pct += jumpSize) {
                float freq = confFreqs.get(i);
                double volFloor = this.getFloorEstimateAvg(Melody.getFrequenciesForPreset(id, freq));
                double volCeiling = this.getCeilingEstimateAvg(Melody.getFrequenciesForPreset(id, freq));
                double testVol = volFloor + pct * (volCeiling - volFloor);
                this.testTones.add(new Melody(id, freq, testVol));
            }
        }

        // prepare list of all trials
        this.sampleTones = (ArrayList<Melody>) this.testTones.clone();  // configure sample tones
        ArrayList<Melody> allTrials = new ArrayList<>();
        for (int i = 0; i < trialsPerTone; i++) allTrials.addAll(this.testTones);
        Collections.shuffle(allTrials);
        this.testTones = allTrials;


        if (this.testTones.size() != trialsPerTone * frequencies.length * volsPerFreq * 3)  // sanity check
            Log.w("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length * volsPerFreq * 3 +
                " test pairs but generated " + this.testTones.size());
    }

    @Override
    protected double getModelCalibProbability(Melody melody, int n) {
        return model.getCalibProbability(melody, n);
    }

    @Override
    protected double getModelRampProbability(Melody melody, boolean withFloorResults) {
        return model.getRampProbability(melody, withFloorResults);
    }

    @Override
    public Runnable sampleTones() {
        if (this.sampleTones.isEmpty()) throw new IllegalStateException("No tones to sample");
        else return new Runnable() {
            /**
             * Play a sample of each melody stored in sampleTones
             */
            @Override
            public void run() {
                if (! iModel.sampleThreadActive())
                    try {
                        iModel.setSampleThreadActive(true);
                        for (Melody melody : sampleTones) {
                            if (!iModel.testPaused()) return;  // stop if user un-pauses
                            playTone(melody.newVol(70));
                            sleepThread(500, 500);
                        }
                    } finally {
                        iModel.setSampleThreadActive(false);
                    }
            }
        };
    }

    @Override
    protected void playTone(Melody tone) {
        for (FreqVolDurTrio note : tone.getNotes())
            playSine(note, note.durationMs());
    }

    @Override
    protected boolean wasCorrect() {
        int expected;
        switch(((Melody) this.currentTrial.tone()).direction()) {
            case Earcon.DIRECTION_DOWN:
                expected = ANSWER_DOWN;
                break;
            case Earcon.DIRECTION_UP:
                expected = ANSWER_UP;
                break;
            case Earcon.DIRECTION_FLAT:
                expected = ANSWER_FLAT;
                break;
            default:
                throw new RuntimeException("Found unexpected value for currentTrial.tone.direction: " +
                        ((Melody) this.currentTrial.tone()).direction());
        }
        return iModel.getAnswer() == expected;
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_UP, ANSWER_DOWN, ANSWER_FLAT};
    }

    @Override
    public String getLineEnd(SingleTrialResult trial) {
        return String.format("%.1f, %s, %s, %d clicks: %s",
                trial.tone().freq(),
                ((Melody) trial.tone()).directionAsString(),
                trial.wasCorrect() ? "Correct" : "Incorrect",
                trial.nClicks(),
                trial.getClicksAsString());
    }

    protected double getFloorEstimateAvg(float[] freqs) {
        double[] estimates = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) estimates[i] = calibResults.getVolFloorEstimateForFreq(freqs[i]);
        return UtilFunctions.mean(estimates);
    }

    protected double getCeilingEstimateAvg(float[] freqs) {
        double[] estimates = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) estimates[i] = calibResults.getVolCeilingEstimateForFreq(freqs[i]);
        return UtilFunctions.mean(estimates);
    }
}
