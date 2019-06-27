package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

/**
 * A class for keeping track of interactions within a PureTone or RampUp test
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    boolean heard; // Has the tone been heard?

    private int answer = ANSWER_NONE;

    private static final int ANSWER_UP = 1;     // indicate how the user answered
    private static final int ANSWER_DOWN = -1;
    private static final int ANSWER_NONE = 0;

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

    public void setAnswer(boolean answeredUp) {
        if (answeredUp) this.answer = ANSWER_UP;
        else this.answer = ANSWER_DOWN;
    }

    public void resetAnswer() {
        this.answer = ANSWER_NONE;
    }

    /**
     * @return > 0 if answered up, 0 if no answer, < 0 if answered down
     */
    public int getAnswer() {
        return this.answer;
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

