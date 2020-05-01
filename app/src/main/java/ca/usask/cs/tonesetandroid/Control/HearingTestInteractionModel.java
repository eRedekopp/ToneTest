package ca.usask.cs.tonesetandroid.Control;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.CalibrationTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.ConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.ReduceTest;

/**
 * A class for handling the state of the UI and HearingTest
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    /**
     * A list of all ModelListeners to be notified when the state of this model changes
     */
    private ArrayList<ModelListener> subscribers;

    /**
     * The msot recently registered response value, or ANSWER_NULL if answer cleared
     * Must be one of HearingTest.ANSWER_*
     */
    private int answer = HearingTest.ANSWER_NULL;

    /**
     * Is there currently a thread running a hearing test?
     */
    private boolean testThreadActive;

    /**
     * Is there currently a thread playing tone samples?
     */
    private boolean sampleThreadActive;

    /**
     * Is the current test paused?
     */
    private boolean testPaused;

    /**
     * The current HearingTest being performed
     */
    private HearingTest currentTest;

    /**
     * The RampTest of the 3-phase calibration, or null if a calibration is not being performed
     */
    private RampTest rampTest;

    /**
     * The ReduceTest of the 3-phase calibration, or null if a calibration is not being performed
     */
    private ReduceTest reduceTest;

    /**
     * The CalibrationTest of the 3-phase calibration, or null if a calibration is not being performed
     */
    private CalibrationTest calibrationTest;

    /**
     * The ConfidenceTest to be performed, or null if not applicable 
     */
    private ConfidenceTest confidenceTest;

    public HearingTestInteractionModel() {
        subscribers = new ArrayList<>();
        this.reset();
    }

    /**
     * Reset this iModel to its just-initialized state (does not affect the subscribers list)
     */
    public void reset() {
        this.testThreadActive = false;
        this.testPaused = false;
        this.currentTest = null;
        this.rampTest = null;
        this.reduceTest = null;
        this.calibrationTest = null;
    }

    /**
     * Set the current test state to "paused" and notify subscribers
     */
    public void setTestPaused(boolean paused) {
        this.testPaused = paused;
        this.notifySubscribers();
    }

    /**
     * Is the current test paused?
     */
    public boolean testPaused() {
        return this.testPaused;
    }

    /**
     * Is a test currently being performed?
     */
    public boolean testing() {
        return this.currentTest != null;
    }

    /**
     * Set the response value for the hearing test
     */
    public void setAnswer(int directionAnswered) {
        this.answer = directionAnswered;
    }

    /**
     * Set the response value to ANSWER_NULL
     */
    public void resetAnswer() {
        this.answer = HearingTest.ANSWER_NULL;
    }

    /**
     * @return The most recent response entered, or ANSWER_NULL if none entered
     */
    public int getAnswer() {
        return this.answer;
    }

    /**
     * Has the user entered any answer since it was last cleared?
     */
    public boolean answered() {
        return this.answer != HearingTest.ANSWER_NULL;
    }

    /**
     * Add a subscriber to the list of ModelListeners
     */
    public void addSubscriber(ModelListener sub) {
        subscribers.add(sub);
    }

    /**
     * Notify all subscribers that there has been a change to the current state
     */
    public void notifySubscribers() {
        for (ModelListener sub : subscribers) {
            sub.modelChanged();
        }
    }

    /**
     * Set the current HearingTest being performed
     */
    public void setCurrentTest(HearingTest currentTest) {
        this.currentTest = currentTest;
        this.notifySubscribers();
    }

    /**
     * Register a click at the current time to the current HearingTest 
     */
    public void addClick(int answer, boolean fromTouchInput) {
        if (this.currentTest == null) throw new IllegalStateException("There is no test currently stored");
        this.currentTest.handleAnswerClick(answer, fromTouchInput);
    }

    public BackgroundNoiseType getCurrentNoise() {
        return this.currentTest.getBackgroundNoiseType();
    }

    public RampTest getRampTest() {
        return rampTest;
    }

    public void setRampTest(RampTest rampTest) {
        this.rampTest = rampTest;
    }

    public ReduceTest getReduceTest() {
        return reduceTest;
    }

    public void setReduceTest(ReduceTest reduceTest) {
        this.reduceTest = reduceTest;
    }

    public CalibrationTest getCalibrationTest() {
        return calibrationTest;
    }

    public boolean testThreadActive() {
        return testThreadActive;
    }

    public void setTestThreadActive(boolean testThreadActive) {
        this.testThreadActive = testThreadActive;
    }

    public boolean sampleThreadActive() {
        return sampleThreadActive;
    }

    public void setSampleThreadActive(boolean sampleThreadActive) {
        this.sampleThreadActive = sampleThreadActive;
    }

    public void setCalibrationTest(CalibrationTest calibrationTest) {
        this.calibrationTest = calibrationTest;
    }

    /**
     * @return An array containing all of the responses that must be available for the current test
     */
    public int[] getCurrentRequiredButtons() {
        if (this.currentTest == null) return new int[]{};
        else return this.currentTest.getPossibleResponses();
    }

    /** 
     * @return The current test being performed, or null if there is none 
     */ 
    public HearingTest getCurrentTest() {
        return currentTest;
    }

    public ConfidenceTest getConfidenceTest() {
        return confidenceTest;
    }

    public void setConfidenceTest(ConfidenceTest confidenceTest) {
        this.confidenceTest = confidenceTest;
    }
}

