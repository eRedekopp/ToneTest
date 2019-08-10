package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;

/**
 * A class for keeping track of interactions within a PureTone or RampUp test
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    boolean heard; // Has the tone been heard?

    public static final int ANSWER_NULL = 0;

    public static final int ANSWER_HEARD = 1;

    public static final int ANSWER_UP = Earcon.DIRECTION_UP;     // == 2

    public static final int ANSWER_DOWN = Earcon.DIRECTION_DOWN; // == 3

    public static final int ANSWER_FLAT = Earcon.DIRECTION_FLAT; // == 4

    private int answer = ANSWER_NULL;

    private boolean testThreadActive;

    private boolean testPaused;

    public boolean testThreadActive() {
        return testThreadActive;
    }

    public void setTestThreadActive(boolean testThreadActive) {
        this.testThreadActive = testThreadActive;
    }

    public void setTestPaused(boolean paused) {
        this.testPaused = paused;
    }

    public boolean testPaused() {
        return this.testPaused;
    }

    private ArrayList<ModelListener> subscribers;

    /**
     * Constructor for the interaction model
     * Create an instance of a model listener array list
     */
    public HearingTestInteractionModel() {
        subscribers = new ArrayList<>();
    }

    /**
     * A mutator method to set the heard parameter to true
     */
    public void toneHeard() {
        heard = true;
        notifySubscribers();
    }

    /**
     * A mutator method to set the heard parameter to false
     */
    public void notHeard() {
        heard = false;
        notifySubscribers();
    }

    public void setAnswer(int directionAnswered) {
        this.answer = directionAnswered;
    }

    public void resetAnswer() {
        this.answer = ANSWER_NULL;
    }

    /**
     * @return An integer indicating the direction that the user answered (HearingTestInteractionModel.ANSWER_*)
     */
    public int getAnswer() {
        return this.answer;
    }

    public boolean answered() {
        return this.answer != ANSWER_NULL;
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
}

