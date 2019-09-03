package ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;

/**
 * A ConfidenceTest that tests the user's ability to hear single piano tones
 */
public class PianoConfidenceTest extends WavConfidenceTest {

    public PianoConfidenceTest(CalibrationTestResults results, BackgroundNoiseType noiseType) {
        super(results, noiseType);
        this.testTypeName = "single-piano-conf";
    }

    /**
     * @throws IllegalArgumentException If frequencies[] contains a value that does not have a default wav resource
     * (see WavTone constructors)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void configureTestTones(int trialsPerTone, int volsPerFreq, float[] frequencies) 
            throws IllegalArgumentException { // todo build tones here rather than using default constructors

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
            Log.e("ConfigureTestPairs", "Error: " + "expected " + trialsPerTone * frequencies.length * volsPerFreq +
                    " test pairs but generated " + this.testTones.size());
    }
}
