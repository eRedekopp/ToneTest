package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.ArrayList;
import java.util.Calendar;

import ca.usask.cs.tonesetandroid.Click;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class SingleTrialResult {

    public final long startTime;
    private ArrayList<Click> clicks;
    public final Tone tone;
    private boolean wasCorrect = false;

    public SingleTrialResult(Tone tone) {
        this.clicks = new ArrayList<>();
        this.tone = tone;
        this.startTime = Calendar.getInstance().getTime().getTime();
    }

    /**
     * @return The most recent click added to this result, or null if no clicks stored
     */
    public Click lastClick() {
        if (clicks.isEmpty()) return null;
        else return this.clicks.get(this.clicks.size() - 1);
    }

    /**
     * Add a single click to this result
     * @param click A new click object representing the click to be added
     */
    public void addClick(Click click) {
        this.clicks.add(click);
    }

    /**
     * Set wasCorrect to true if the last direction clicked was expectedDirection, else set it to false
     */
    public void setCorrect(int expectedDirection) {
        this.wasCorrect = this.lastClick().direction == expectedDirection;
    }

    /**
     * @return True if the user answered correctly for this trial, else false
     */
    public boolean wasCorrect() {
        return this.wasCorrect;
    }

    /**
     * @return The number of times that the user clicked an answer button during this trial
     */
    public int nClicks() {
        return clicks.size();
    }

    /**
     * @return An array of integers containing the offset in milliseconds from the start of the tone to each click,
     * in chronological order
     */
    public long[] clickTimes() {
        long[] newArr = new long[this.clicks.size()];
        for (int i = 0; i < this.clicks.size(); i++) newArr[i] = this.clicks.get(i).time.getTime() - this.startTime;
        return newArr;
    }
}
