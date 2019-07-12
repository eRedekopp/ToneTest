package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

/**
 * A class for keeping track of interactions within a PureTone or RampUp test
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    boolean heard; // Has the tone been heard?

    private static final int ANSWER_NULL = -100;

    private int answer = ANSWER_NULL;

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
     * @return An integer indicating the direction that the user answered (Earcon.DIRECTION_*), or
     * HearingTestInteractionModel.ANSWER_NULL if user did not answer (ANSWER_NULL guaranteed to not be equal to an
     * Earcon direction)
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

