package ca.usask.cs.tonesetandroid;

import java.util.Calendar;

/**
 * A class to store information about a specific button press
 */
public class Click {

    /**
     * the time in milliseconds since 1970 at which the click was registered
     */
    public final long time;

    /**
     * the response value associated with this click
     */
    public final int answer;

    public Click(int answer) {
        this.time = Calendar.getInstance().getTime().getTime();
        this.answer = answer;
    }
}
