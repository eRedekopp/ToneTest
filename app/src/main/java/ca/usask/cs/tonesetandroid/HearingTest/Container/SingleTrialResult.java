package ca.usask.cs.tonesetandroid.HearingTest.Container;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;

import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

/**
 * A class to store information about one specific trial of a HearingTest
 * @param <T> The tone type of this trial's associated HearingTest
 */
public class SingleTrialResult<T extends Tone> {

    /**
     * The time in seconds since 1970 at which start() was called
     */
    private long startTime;

    /**
     * A list of all Clicks that were registered during this trial
     */
    private ArrayList<Click> clicks;

    /**
     * The tone being played for this trial
     */
    private final T tone;

    /**
     * Did the user hear the tone / discern the direction correctly?
     */
    private boolean wasCorrect = false;

    /**
     * @param tone The tone being played for this trial
     */
    public SingleTrialResult(T tone) {
        this.clicks = new ArrayList<>();
        this.tone = tone;
    }

    /**
     * @return The most recent click added to this result, or null if no clicks stored
     */
    public Click lastClick() {
        if (clicks.isEmpty()) return null;
        else return this.clicks.get(this.clicks.size() - 1);
    }

    /**
     * Set the start time to the current millisecond on the clock. Call this method immediately before the tone plays
     * in a test
     */
    public void start() {
        this.startTime = Calendar.getInstance().getTime().getTime();
    }

    /**
     * Add a single click to this result
     * @param click A new click object representing the click to be added
     */
    public void addClick(Click click) {
        this.clicks.add(click);
    }

    public T tone() {
        return tone;
    }

    /**
     * Set wasCorrect to the given value
     */
    public void setCorrect(boolean b) {
        this.wasCorrect = b;
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
        for (int i = 0; i < this.clicks.size(); i++) newArr[i] = this.clicks.get(i).time - this.startTime;
        return newArr;
    }

    /**
     * eg. "Up 200, Down 250, Flat 275"
     *
     * @return A string containing the direction and offset in milliseconds from the start time for each click
     */
    public String getClicksAsString() {
        StringBuilder builder = new StringBuilder();
        for (Click click : this.clicks) {
            builder.append(HearingTest.answerAsString(click.answer));
            builder.append(' ');
            builder.append(click.time - this.startTime);
            builder.append(", ");
        }
        return builder.toString();
    }

    public long getStartTime() {
        return this.startTime;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("Tone: %s, Correct? %b, nClicks: %d, clicks: %s",
                this.tone.toString(), this.wasCorrect, this.nClicks(), this.getClicksAsString());
    }
}
