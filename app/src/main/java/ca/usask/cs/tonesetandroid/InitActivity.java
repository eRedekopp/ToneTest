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
 * Activity for initial setup "login" screen. Either initialize a test subject with a new ID, or load a
 * test subject's calibration file. Either way, update the model passed by the caller directly then return
 * to caller (which should be MainActivity)
 */
public class InitActivity extends Activity {
    
    private FileNameController fController;
    private Model model;

    private String dialogSelectedString; // for getSelectionFromDialog

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_setup_view);

        // get mvc elements
        Intent callerActivity = getIntent();
        try {
            // todo pass FileController to this from MainActivity
            this.fController = (FileNameController) callerActivity.getExtras().get("fileController");
        } catch (NullPointerException e) {
            System.out.println("No file controller passed to InitActivity");
            e.printStackTrace();
            System.exit(1);
        }
        try {
            // todo pass Model to this from MainActivity
            this.model = (Model) callerActivity.getExtras().get("model");
        } catch (NullPointerException e) {
            System.out.println("No Model passed to InitActivity");
            e.printStackTrace();
            System.exit(1);
        }

        // set up view elements
        final Button idEntryButton =      findViewById(R.id.idEntryButton);
        final Button skipButton =         findViewById(R.id.skipButton);
        final Button loadButton =         findViewById(R.id.loadButton);
        final EditText idEntryEditText =  findViewById(R.id.idEntryEditText);

        // set up event listeners
        idEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int entry = Integer.parseInt(idEntryEditText.getText().toString());
                handleSubjectIdClick(entry);
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
                int entry = Integer.parseInt(idEntryEditText.getText().toString());
                if (! fController.directoryExistsForSubject(entry))
                    showErrorDialog("No configurations saved for subject with id " + entry);
                else {
                    String[] subjectFileNames;
                    try {
                        subjectFileNames = fController.getFileNamesFromCalibDir(entry);
                    } catch (FileNotFoundException e) {
                        showErrorDialog(e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    if (subjectFileNames.length == 0)
                        showErrorDialog("No configurations saved for subject with id " + entry);
                    else {
                        getSelectionAndInitialize(subjectFileNames, entry);
                    }
                }
            }
        });
    }

    /**
     * For loading calibration results from file
     *
     * Get a user selection from the given list of filename strings, then initialize the model from
     * that file and tell the MasterController to go to the main menu. Note: the filenames in options
     * must belong to the user with the given subjectID.
     *
     * @param options A list of calibration filenames for the given subject
     * @param subjectID The subject to whom the "options" files belong
     */
    private void getSelectionAndInitialize(final String[] options, final int subjectID) {
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
                    inFile = fController.getCalibFileFromName(subjectID, getDialogSelectedString());
                } catch (FileNotFoundException e) {
                    showErrorDialog("Unknown error: selected file not found in directory");
                    e.printStackTrace();
                    return;
                }
                FileNameController.initializeModelFromFileData(inFile, model);
                handleSubjectIdClick(subjectID); // enter subject id and return to caller after initializing data
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

    private void setDialogSelectedItem(String item) {
        this.dialogSelectedString = item;
    }

    private String getDialogSelectedString() {
        return this.dialogSelectedString;
    }

    private void handleSubjectIdClick(int id) {
        this.model.setSubjectId(id);
        returnToCaller();
    }

    private void returnToCaller() {
        if (this.model.getSubjectId() == -1)
            throw new IllegalStateException("Returned from InitActivity without setting subject id");

        // no data sent back because model gets updated directly
        this.setResult(RESULT_OK);

        // close this activity; shouldn't be called again until program restart
        this.finish();
    }
}
