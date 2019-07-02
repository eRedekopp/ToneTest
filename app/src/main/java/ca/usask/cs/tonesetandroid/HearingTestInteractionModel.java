package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

/**
 * A class for keeping track of interactions within a PureTone or RampUp test
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    boolean heard; //boolean value to indicate that the tone was heard

    ArrayList<ModelListener> subscribers;

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

