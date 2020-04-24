package ca.usask.cs.tonesetandroid.HearingTest.Container;

/**
 * A class to store information about a specific touchscreen button press, volume button press, or "shake" event
 */
public class Click {

    /**
     * the time in milliseconds at which the click was registered
     */
    public final long time;

    /**
     * the response value associated with this click
     */
    public final int answer;

    /**
     * was this click registered via the touchscreen?
     */
    public final boolean wasTouchInput;

    public Click(int answer, boolean wasTouchInput) {
        this.time = System.currentTimeMillis();
        this.answer = answer;
        this.wasTouchInput = wasTouchInput;
    }
}
