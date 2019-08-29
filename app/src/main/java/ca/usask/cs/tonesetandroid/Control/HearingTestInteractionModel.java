package ca.usask.cs.tonesetandroid.Control;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.CalibrationTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.ConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.ReduceTest;

/**
 * Handles the menu and current test being performed
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    private ArrayList<ModelListener> subscribers;

    // most recent answer entered by user, or null if answer cleared
    private int answer = HearingTest.ANSWER_NULL;

    // current state of test
    private boolean testThreadActive;
    private boolean sampleThreadActive;
    private boolean testPaused;
    private HearingTest currentTest;

    // variables for each stage of calibration test
    private RampTest rampTest;
    private ReduceTest reduceTest;
    private CalibrationTest calibrationTest;

    private ConfidenceTest confidenceTest;

    /**
     * Reset this iModel to its just-initialized state
     */
    public void reset() {
        this.testThreadActive = false;
        this.testPaused = false;
        this.currentTest = null;
        this.rampTest = null;
        this.reduceTest = null;
        this.calibrationTest = null;
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

    public void setTestPaused(boolean paused) {
        this.testPaused = paused;
        this.notifySubscribers();
    }

    public boolean testPaused() {
        return this.testPaused;
    }

    public boolean testing() {
        return this.currentTest != null;
    }

    /**
     * Constructor for the interaction model
     * Create an instance of a model listener array list
     */
    public HearingTestInteractionModel() {
        subscribers = new ArrayList<>();
        this.reset();
    }

    public void setAnswer(int directionAnswered) {
        this.answer = directionAnswered;
    }

    public void resetAnswer() {
        this.answer = HearingTest.ANSWER_NULL;
    }

    /**
     * @return An integer indicating the direction that the user answered (HearingTestInteractionModel.ANSWER_*)
     */
    public int getAnswer() {
        return this.answer;
    }

    public boolean answered() {
        return this.answer != HearingTest.ANSWER_NULL;
    }

    /**
     * Add a subscriber to the array list of model listeners
     */
    public void addSubscriber(ModelListener sub) {
        subscribers.add(sub);
    }

    /**
     * Notify all subscribers that there has been a change to the iModel
     */
    public void notifySubscribers() {
        for (ModelListener sub : subscribers) {
            sub.modelChanged();
        }
    }

    /**
     * @return An array containing all of the responses that must be available for the current stage of the test
     */
    public int[] getCurrentRequiredButtons() {
        if (this.currentTest == null) return new int[]{};
        else return this.currentTest.getRequiredButtons();
    }

    public HearingTest getCurrentTest() {
        return currentTest;
    }

    public void setCurrentTest(HearingTest currentTest) {
        this.currentTest = currentTest;
        this.notifySubscribers();
    }

    public void addClick(int answer) {
        if (this.currentTest == null) throw new IllegalStateException("There is no test currently stored");
        this.currentTest.handleAnswerClick(answer);
    }

    /**
     * @return The results of the confidence test currently stored as a string, or the string "Confidence Statistics
     * Unavailable" if none stored
     */
    public String getConfResultsAsString() {
        if (this.getConfidenceTest() == null || ! this.getConfidenceTest().isComplete())
            return "Confidence Statistics Unavailable";
        return this.getConfidenceTest().summaryStatsAsString();
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

    public void setCalibrationTest(CalibrationTest calibrationTest) {
        this.calibrationTest = calibrationTest;
    }

    public CalibrationTestResults getCalibrationResults() {
        if (this.calibrationTest == null) throw new IllegalStateException("No calibration test stored");
        else return this.calibrationTest.getResults();
    }

    public ConfidenceTest getConfidenceTest() {
        return confidenceTest;
    }

    public void setConfidenceTest(ConfidenceTest confidenceTest) {
        this.confidenceTest = confidenceTest;
    }
}

