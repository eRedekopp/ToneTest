package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.util.Log;

import java.util.ArrayList;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.ReducibleTone;

public abstract class RampTest<T extends ReducibleTone> extends HearingTest<T> {

    protected static final int TIME_PER_VOL_MS = 50;
    protected static final double RAMP_RATE_1 = 1.05;
    protected static final double RAMP_RATE_2 = 1.025;
    protected static final double STARTING_VOL = 0.5;

    protected ArrayList<Float> freqs;
    protected ListIterator<Float> position;
    protected RampTestResults results;

    public RampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.results = new RampTestResults();
    }

    @Override
    protected void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    model.configureAudio();
                    iModel.setTestThreadActive(true);
                    double heardVol;

                    while (! isComplete()) {
                        if (iModel.testPaused() || ! iModel.testing()) return;
                        float currentFreq = position.next();

                        // test frequency, ramp up quickly
                        Log.i(testTypeName, "Testing frequency: " + currentFreq);
                        iModel.resetAnswer();
                        heardVol = rampUp(RAMP_RATE_1, currentFreq, STARTING_VOL);
                        if (heardVol == -1 || iModel.testPaused()) {
                            position.previous(); // move cursor back to starting location and return without doing
                            return;              // anything if user paused
                        }
                        saveLine(new FreqVolPair(currentFreq, heardVol).toString() + " first");

                        sleepThread(1000, 1000);  // sleep 1 second

                        // test frequency again, slower, starting from 1/10 the first heardVol
                        iModel.resetAnswer();
                        heardVol = rampUp(RAMP_RATE_2, currentFreq, heardVol / 10.0);
                        if (heardVol == -1 || iModel.testPaused()) {
                            position.previous(); // move cursor back to starting location and return without doing
                            return;              // anything if user paused
                        }

                        // save result
                        saveLine(new FreqVolPair(currentFreq, heardVol).toString() + " second");
                        results.addResult(currentFreq, heardVol);
                    }

                    // ramp test complete: configureTestTones reduce test with these results and setup to begin next
                    iModel.getReduceTest().initialize(results);
                    iModel.setCurrentTest(iModel.getReduceTest());
                    iModel.setTestThreadActive(false);
                    iModel.notifySubscribers();
                    iModel.setTestPaused(true);
                    view.showInformationDialog(iModel.getReduceTest().getTestInfo());

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    @Override
    public int[] getRequiredButtons() {
        return new int[]{ANSWER_HEARD};
    }

    @Override
    public boolean isComplete() {
        return ! position.hasNext();
    }

    @Override
    protected final String getLineEnd(SingleTrialResult result) {
        throw new RuntimeException("Operation not supported for ramp test");
    }

    @Override
    public void handleAnswerClick(int answer) {
        // RampTests have no need for this method because they do not track click times
    }

    public RampTestResults getResults() {
        return this.results;
    }

    /**
     * Play a tone of the given frequency at startingVol and slowly get louder until user presses "heard" or max
     * volume reached
     *
     * @param rateOfRamp The multiplier for the ramp speed (1.0 < rateOfRamp < ~1.1)
     * @param freq The frequency of the tone to be ramped up
     * @param startingVol The volume at which to start the ramp (0 < startingVol <= Short.MAX_VALUE)
     * @return The volume at which the user pressed "heard", or max volume if not pressed, or -1 if user paused test
     * during ramp
     */
    protected abstract double rampUp(double rateOfRamp, float freq, double startingVol);

    public class RampTestResults {

        ArrayList<FreqVolPair> results;

        public RampTestResults() {
            this.results = new ArrayList<>();
        }

        public void addResult(float freq, double vol) {
            results.add(new FreqVolPair(freq, vol));
        }

        public FreqVolPair[] getResults() {
            return results.toArray(new FreqVolPair[]{});
        }

    }
}
