package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

/**
 * A class for keeping track of interactions within a PureTone or RampUp test
 *
 * @author alexscott
 */
public class HearingTestInteractionModel {

    boolean heard; //boolean value to indicate that the tone was heard
    boolean heardTwice; //this is used for the pure tone only

    //Boolean values representing whether the buttons are enabled or not
    private boolean testMode;

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

    public boolean isInTestMode() {
        return testMode;
    }

    public void setTestMode(boolean b) {
        this.testMode = b;
        this.notifySubscribers();
    }

    /**
     * A mutator method to set the heardTwice parameter to true
     */
    public void toneHeardTwice() {
        heardTwice = true;
        notifySubscribers();
    }


    /**
     * A mutator method to set the heardTwice parameter to false
     */
    public void notHeardTwice() {
        heardTwice = false;
        notifySubscribers();
    }

    /**
     * Add a subscriber to the array list of model listeners
     * @param sub
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

