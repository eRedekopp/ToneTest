package ca.usask.cs.tonesetandroid;

import java.util.Calendar;
import java.util.Date;

public class Click {

    public final Date time;
    public final int direction;

    public Click(int direction) {
        this.time = Calendar.getInstance().getTime();
        this.direction = direction;
    }

}
