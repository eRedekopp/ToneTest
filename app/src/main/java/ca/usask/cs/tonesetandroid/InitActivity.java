package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;

import ca.usask.cs.tonesetandroid.Control.FileIOController;

/**
 * Gets a subject ID from the user and goes back to caller with the subject ID and a pathname from which
 * to read calibration results (or "" if not applicable)
 *
 * @author redekopp
 */
public class InitActivity extends Activity {

    static FileIOController fileController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_setup_view);

        // locate view elements
        final Button newPartButton =      findViewById(R.id.idEntryButton);
        final Button skipButton =         findViewById(R.id.skipButton);
        final Button loadButton =         findViewById(R.id.loadButton);
        final EditText idEntryEditText =  findViewById(R.id.idEntryEditText);

        // set up event listeners
        newPartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int entry;
                try {
                    entry = Integer.parseInt(idEntryEditText.getText().toString());
                } catch (NumberFormatException e) {
                    showErrorDialog("Invalid subject id number");
                    return;
                }
                if (fileController.participantExists(entry)) {
                    showErrorDialog(String.format("Participant with ID %d already exists", entry));
                } else if (entry == 0) {
                    showErrorDialog("Participant ID 0 is reserved for testing. Please choose another ID");
                } else {
                    handleNewPartClick(entry);
                }
            }
        });
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSkipClick();
            }
        });
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int entry;
                try {
                    entry = Integer.parseInt(idEntryEditText.getText().toString());
                } catch (NumberFormatException e) {
                    showErrorDialog("Invalid subject id number");
                    return;
                }

                if (! fileController.participantExists(entry)) {
                    showErrorDialog("Participant with ID %d does not exist");
                } else {
                    handleLoadPartClick(entry);
                }
            }
        });
    }

    /**
     * Show a dialog with title "Error" and the given message
     * @param message The message to be displayed
     */
    private void showErrorDialog(String message) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Error");
        warningBuilder.setMessage(message);
        warningBuilder.show();
    }

    /**
     * Handle the "skip this step" button, ie. log in with user 0 and return to MainActivity
     */
    private void handleSkipClick() {
        if (! fileController.participantExists(0)) {
            fileController.createNewPartFiles(0);
        }
        returnToCaller(0);
    }

    /**
     * Create the files for the given participant and return to MainActivity. Participant must not already exist
     *
     * @param partID The participant ID of the new participant
     */
    private void handleNewPartClick(int partID) {
        fileController.createNewPartFiles(partID);
        returnToCaller(partID);
    }

    /**
     * Do not create any new files and return to MainActivity with the given ID. Participant must exist
     *
     * @param partID The participant ID of the new participant
     */
    private void handleLoadPartClick(int partID) {
        returnToCaller(partID);
    }

    /**
     * Return to the calling activity with a completely loaded or newly created Participant object
     */
    private void returnToCaller(int id) {

        // create intent and pass args
        Intent goBackIntent = new Intent();
        goBackIntent.putExtra("id", id);
        this.setResult(RESULT_OK, goBackIntent);

        // close this activity; shouldn't be called again until program restart
        this.finish();
    }
}
