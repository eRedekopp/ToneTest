package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.util.Calendar;

public class Click {

    public final long time;  // the time in milliseconds since 1970 at which the click was registered
    public final int answer; // the response value associated with this click

    public Click(int direction) {
        this.time = Calendar.getInstance().getTime().getTime();
        this.answer = direction;
    }

}
