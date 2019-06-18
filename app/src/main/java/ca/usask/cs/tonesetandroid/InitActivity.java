package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Gets a subject ID from the user and goes back to caller with the subject ID and a pathname from which
 * to read calibration results (or "" if not applicable)
 *
 * @author redekopp
 */
public class InitActivity extends Activity {

    private String dialogSelectedString; // for getSelectionAndGoBack

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_setup_view);

        // set up view elements
        final Button idEntryButton =      findViewById(R.id.idEntryButton);
        final Button skipButton =         findViewById(R.id.skipButton);
        final Button loadButton =         findViewById(R.id.loadButton);
        final EditText idEntryEditText =  findViewById(R.id.idEntryEditText);

        // set up event listeners
        idEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int entry = Integer.parseInt(idEntryEditText.getText().toString());
                    handleSubjectIdClick(entry);
                } catch (NumberFormatException e) {
                    showErrorDialog("Invalid subject id number");
                }
            }
        });
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubjectIdClick(0);
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
                if (! FileNameController.directoryExistsForSubject(entry))
                    showErrorDialog("No configurations saved for subject with id " + entry);
                else {
                    String[] subjectFileNames;
                    try {
                        subjectFileNames = FileNameController.getFileNamesFromCalibDir(entry);
                    } catch (FileNotFoundException e) {
                        showErrorDialog(e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    if (subjectFileNames.length == 0)
                        showErrorDialog("No configurations saved for subject with id " + entry);
                    else {
                        getSelectionAndGoBack(subjectFileNames, entry);
                    }
                }
            }
        });
    }

    /**
     * For loading calibration results from file. Get a user selection from the given list of filename strings,
     * then go back to caller passing the data retrieved from user
     *
     * @param options A list of calibration filenames for the given subject
     * @param subjectID The subject to whom the "options" files belong
     */
    private void getSelectionAndGoBack(final String[] options, final int subjectID) {
        AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
        optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDialogSelectedItem(options[which]);
            }
        });
        optBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                File inFile;
                try {
                    inFile = FileNameController.getCalibFileFromName(subjectID, getDialogSelectedString());
                } catch (FileNotFoundException e) {
                    showErrorDialog("Unknown error: selected file not found in directory");
                    e.printStackTrace();
                    return;
                }
                returnToCaller(subjectID, inFile.getAbsolutePath());
            }
        });
        optBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        optBuilder.show();
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

    private void setDialogSelectedItem(String item) {
        this.dialogSelectedString = item;
    }

    private String getDialogSelectedString() {
        return this.dialogSelectedString;
    }

    /**
     * Handler for "new calibration" click : return to MainActivity with the given subject id
     * @param id The subject id with which to initalize the model
     */
    private void handleSubjectIdClick(final int id) {
        if (id != 0 && FileNameController.directoryExistsForSubject(id)) // show warning dialog if ID already used
            new AlertDialog.Builder(this)                                // unless it's subject ID 0 (dummy ID)
                    .setMessage("This subject ID has already been used")
                    .setTitle("Warning")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                            returnToCaller(id, "");
                        }
                    })
                    .show();
        else returnToCaller(id, "");
    }

    /**
     * Return to the caller method with extras "Subject ID" to indicate the ID of this test subject retrieved from
     * the user, and "Path Name" to indicate the path from which to load test results (or "" if not applicable)
     *
     * @param subjectID The subject ID retrieved from the user
     * @param pathName The absolute path name retrieved from the user, or ""
     */
    private void returnToCaller(int subjectID, String pathName) {

        // create intent and pass parameters
        Intent goBackIntent = new Intent();
        goBackIntent.putExtra("subjectID", subjectID);
        goBackIntent.putExtra("pathName", pathName);
        this.setResult(RESULT_OK, goBackIntent);

        // close this activity; shouldn't be called again until program restart
        this.finish();
    }
}
