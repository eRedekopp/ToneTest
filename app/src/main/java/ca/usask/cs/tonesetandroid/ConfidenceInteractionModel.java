package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;

/**
 *
 * @author alexscott
 */
public class ConfidenceInteractionModel  {


    //Button pushed status
    boolean yesPushed;
    boolean noPushed;
    boolean beginConfidence;

    //button enabled status
    boolean yesEnabled;
    boolean noEnabled;
    boolean saveEnabled;

    // saved status
    boolean resultsSaved;

    ArrayList<ModelListener> subscribers;

    /**
     * Constructor for the confidenceInteractionModel
     * Create the subscriber array list
     */
    public ConfidenceInteractionModel() {
        subscribers = new ArrayList<>();
        resultsSaved = false;
    }

    /**
     * Add a subscriber to the array list
     * @param sub
     */
    public void addSubscriber(ModelListener sub) {
        subscribers.add(sub);
    }

    /**
     * Notify the subscribers that there has been a change
     */
    public void notifySubscribers() {
        for (ModelListener l : subscribers) l.modelChanged();
    }

    /**
     * Update the pushed status for the yes button
     * By extension, also update the pushed status for the no button
     */
    public void yesBtnClicked() {
        yesPushed = true;
        noPushed = false;
    }

    /**
     * Update the pushed status for the no button
     * By extension, also update the pushed status for the yes button
     */
    public void noBtnClicked() {
        noPushed = true;
        yesPushed = false;
    }

    /**
     * Reset the pushed status for both the yes and no button
     */
    public void clearBtns() {
        yesPushed = false;
        noPushed = false;
    }

    public boolean resultsSaved() {
        return resultsSaved;
    }

    public void setResultsSaved(boolean resultsSaved) {
        this.resultsSaved = resultsSaved;
        this.notifySubscribers();
    }

    /**
     * If neither the yes or no button have been pushed then then return true to indicate that
     * we are still waiting for the user to click one of the two buttons. Return false otherwise
     * @return boolean indicating whether the yes or no buttons were pushed
     */
    public boolean waitingForClick() {
        return !yesPushed && !noPushed;
    }

    /**
     * Enable the yes button
     */
    public void enableYes(){
        yesEnabled = true;
        notifySubscribers();
    }

    /**
     * Disable the yes button
     */
    public void disableYes(){
        yesEnabled = false;
        notifySubscribers();
    }

    /**
     * Enable the no button
     */
    public void enableNo(){
        noEnabled = true;
        notifySubscribers();
    }

    /**
     * Disable the no button
     */
    public void disableNo(){
        noEnabled = false;
        notifySubscribers();
    }

    /**
     * Enable the save button
     */
    public void enableSave(){
        saveEnabled = true;
        notifySubscribers();
    }

    /**
     * Disable the save button
     */
    public void disableSave(){
        saveEnabled = false;
        notifySubscribers();
    }
}

