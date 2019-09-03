package ca.usask.cs.tonesetandroid;

/**
 * An interface for classes that handle display during a hearing test
 */
public interface HearingTestView {

    /**
     * Displays a dialog box with the title "Information" and the given message. Sets testPaused to false when cancelled
     *
     * @param message the message to be displayed
     */
    void showInformationDialog(String message);

    /**
     * Displays a dialog box with the title "Information" and the given message, and will execute the runnable on a
     * new thread when the user presses a button.
     *
     * @param r A background task to run when the user presses a button (play audio samples, specifically)
     * @param message The message to be displayed
     */
    void showSampleDialog(Runnable r, String message);
}
