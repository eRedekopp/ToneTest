package ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp;

import java.util.ArrayList;
import java.util.ListIterator;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResultsWithFloorInfo;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * Base class for "ramp tests", in which a tone starts quiet and slowly gets louder until the user indicates they've
 * heard the tone to get an estimate for a consistently audible volume 
 *
 * @param <T> The type of tone to be played in this RampTest
 */
public abstract class RampTest<T extends Tone> extends HearingTest<T> {

    /**
     * The volume at which to start the tone
     */
    protected double startingVol = 0.5; 

    protected static final String DEFAULT_TEST_INFO =
            "In this phase of the test, tones will play quietly and slowly get louder. Please press the \"Heard " +
            "Tone\" button as soon as the tone becomes audible";

    /**
     * All the tones to be played in this ramp test - volumes will be disregarded
     */
    protected ArrayList<T> tones;

    /**
     * An iterator for tones to keep track of the current tone being tested
     */
    protected ListIterator<T> position;

    /**
     * Play a tone of the given frequency at startingVol and slowly get louder until user presses "heard" or max
     * volume reached
     *
     * @param rateOfRamp The multiplier for the ramp speed (1.0 < rateOfRamp < ~1.1)
     * @param tone A Tone object representing the sound of the tone to be ramped up (tone.vol() disregarded)
     * @param startingVol The volume at which to setStartTime the ramp (0 < startingVol <= Short.MAX_VALUE)
     * @return The volume at which the user pressed "heard", or max volume if not pressed, or -1 if user paused test
     *         during ramp
     */
    protected abstract double rampUp(double rateOfRamp, T tone, double startingVol);

    /**
     * @return rateOfRamp for the first try of each frequency
     */
    protected abstract float getRampRate1();

    /**
     * @return rateOfRamp for the second try of each frequency
     */
    protected abstract float getRampRate2();

    public RampTest(BackgroundNoiseType noiseType) {
        super(noiseType);
        this.results = new RampTestResultsWithFloorInfo(noiseType, this.getTestTypeName());
    }

    @Override
    protected void run() {

        this.setStartTime();  // set the start time of this test (or do nothing if this has already been done)

        // sanity check
        if (this.tones == null || this.position == null)
            throw new IllegalStateException(String.format(
                    "RampTest not initialized. tones = null ? %b , position = null ? %b",
                    this.tones == null, this.position == null));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    model.configureAudio();
                    iModel.setTestThreadActive(true);
                    double heardVol;

                    while (! isComplete()) {

                        if (iModel.testPaused() || ! iModel.testing()) return; // quit if user pauses or exits
                        T currentTone = position.next();

                        // ramp up currentTone quickly
                        iModel.resetAnswer();
                        heardVol = rampUp(getRampRate1(), currentTone, startingVol);
                        if (heardVol == -1 || iModel.testPaused()) {
                            position.previous(); // move cursor back to starting location and return without doing
                            return;              // anything if user paused
                        }
                        double vol1 = heardVol;

                        sleepThread(1000, 1000);  // sleep 1 second

                        // test currentTone again, slower, starting from 1/10 the first heardVol
                        iModel.resetAnswer();
                        heardVol = rampUp(getRampRate2(), currentTone, heardVol / 10.0);
                        if (heardVol == -1 || iModel.testPaused()) {
                            position.previous(); // move cursor back to starting location and return without doing
                            return;              // anything if user paused
                        }

                        // save result
                        currentTrial = new SingleRampTrialResult(currentTone.newVol(vol1),
                                                                 currentTone.newVol(heardVol));
                        currentTrial.setStartTime();  // start time not really importanthere , just set it now
                        saveLine();
                        ((RampTestResultsWithFloorInfo) results).addResult(currentTone.freq(), vol1, heardVol);
                    }

                    // ramp test complete - move on to next step
                    controller.rampTestComplete();

                } finally {
                    iModel.setTestThreadActive(false);
                }
            }
        }).start();
    }

    @Override
    public int[] getPossibleResponses() {
        return new int[]{ANSWER_HEARD};
    }

    @Override
    public boolean isComplete() {
        return ! position.hasNext();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final String getLineEnd(SingleTrialResult result) {
        SingleRampTrialResult rampResult = (SingleRampTrialResult) result;
        return String.format("Freq: %.1f, vol1: %.4f, vol2: %.4f",
                rampResult.tone().freq(), rampResult.tone().vol(), rampResult.tone2().vol());
    }

    @Override
    public void handleAnswerClick(int answer, boolean fromTouchInput) {
        // RampTests have no need for this method because they do not track click times
    }

    public RampTestResultsWithFloorInfo getResults() {
        return (RampTestResultsWithFloorInfo) this.results;
    }

    /**
     * A modified SingleTrialResult for ramp tests. this.tone() is a FreqVolPair with freq=the frequency of this trial
     * and vol=the volume at which user pressed "heard" for the first tone in this trial
     */
    private class SingleRampTrialResult extends SingleTrialResult {

        /**
         * A FreqVolPair with freq=the frequency of this trial and vol=the volume at which user pressed "heard" for
         * the second tone in this trial
         */
        private FreqVolPair tone2;

        public SingleRampTrialResult(Tone tone1, Tone tone2) {
            super(new FreqVolPair(tone1.freq(), tone1.vol()));
            this.tone2 = new FreqVolPair(tone2.freq(), tone2.vol());
        }

        /**
         * @return A FreqVolPair with freq=the frequency of this trial and vol=the volume at which user pressed "heard" for
         *         the second tone in this trial
         */
        public Tone tone2() {
            return this.tone2;
        }
    }
}
