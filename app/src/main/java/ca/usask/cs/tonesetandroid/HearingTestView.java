package ca.usask.cs.tonesetandroid;

/**
 * An interface for classes that handle display during a hearing test
 */
public interface HearingTestView {

    /**
     * Displays a dialog box with the title "Information" and the given message
     *
     * @param message the message to be displayed
     */
    void showInformationDialog(String message);
}
