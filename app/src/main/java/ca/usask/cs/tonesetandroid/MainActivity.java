package ca.usask.cs.tonesetandroid;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Perform initial setup and launch
 *
 * @author redekopp
 */
public class MainActivity extends AppCompatActivity implements ModelListener {

    public static final int REQUEST_EXT_WRITE = 1;

    private String dialogSelectedString; // for getSelectionFromDialog

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestController controller;
    FileNameController fileController;
    MasterController masterController;

    Button rampButton, pureButton, heardButton, saveButton, confidenceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // get read/write permissions
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXT_WRITE);

        // instantiate self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create mvc elements
        final Model newModel = new Model();
        HearingTestInteractionModel newIModel = new HearingTestInteractionModel();
        HearingTestController newController = new HearingTestController();
        final FileNameController newFController = new FileNameController(this);
        final InitialSetupController newIController = new InitialSetupController();
        final MasterController newMController = new MasterController();

        // set up relations
        this.setFileController(newFController);
        this.setController(newController);
        this.setModel(newModel);
        this.setIModel(newIModel);
        this.setMasterController(newMController);

        this.model.addSubscriber(this);
        this.iModel.addSubscriber(this);
        this.controller.setModel(newModel);
        this.controller.setiModel(newIModel);

        // set up view elements for login screen
        final Button idEntryButton =      findViewById(R.id.idEntryButton);
        final Button skipButton =         findViewById(R.id.skipButton);
        final Button loadButton =         findViewById(R.id.loadButton);
        final EditText idEntryEditText =  findViewById(R.id.idEntryEditText);

        // set up event listeners for login screen
        idEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int entry = Integer.parseInt(idEntryEditText.getText().toString());
                newIController.handleSubjectIdClick(entry);
            }
        });
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newIController.handleSubjectIdClick(0);
            }
        });
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int entry = Integer.parseInt(idEntryEditText.getText().toString());
                if (! newFController.directoryExistsForSubject(entry))
                    showErrorDialog("No configurations saved for subject with id " + entry);
                else {
                    String[] subjectFileNames;
                    try {
                        subjectFileNames = newFController.getFileNamesFromCalibDir(entry);
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

        // set up view elements for main screen
        rampButton =        findViewById(R.id.rampButton);
        pureButton =        findViewById(R.id.pureButton);
        heardButton =       findViewById(R.id.heardButton);
        saveButton =        findViewById(R.id.saveButton);
        confidenceButton =  findViewById(R.id.confidenceButton);

        // set up event listeners for main screen
        pureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handlePureToneClick();
            }
        });
        heardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleHeardClick();
            }
        });
        rampButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                controller.handleRampUpClick();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fileController.handleSaveCalibClick();
            }
        });

        // start in init mode
        newMController.setMode(MasterController.Mode.INIT);
    }

    /**
     * Enable/disable the buttons given the new status of the Model and iModel
     */
    public void modelChanged() {
        rampButton.setEnabled(!iModel.isInTestMode());
        pureButton.setEnabled(!iModel.isInTestMode());
        heardButton.setEnabled(iModel.isInTestMode());
        confidenceButton.setEnabled(model.hasResults() && !iModel.isInTestMode());
        saveButton.setEnabled(model.hasResults() && !iModel.isInTestMode());
//        saveButton.setEnabled(true); // todo: remove this after done testing file io
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void setIModel(HearingTestInteractionModel iModel) {
        this.iModel = iModel;
    }

    public void setController(HearingTestController controller) {
        this.controller = controller;
    }

    public void setFileController(FileNameController fileController) {
        this.fileController = fileController;
    }

    public void setMasterController(MasterController masterController) {
        this.masterController = masterController;
    }

    /**
     * Show a dialog with title "Error" and the given message
     * @param message The message to be displayed
     */
    public void showErrorDialog(String message) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Error");
        warningBuilder.setMessage(message);
        warningBuilder.show();
    }

    /**
     * Get a user selection from the given list of filename strings, then initialize the model from
     * that file and tell the MasterController to go to the main menu. Note: the filenames in options
     * must belong to the user with the given subjectID
     *
     * Kind of a lot of stuff for one function, but due to scoping issues and the fact that it uses
     * callback functions, this is really the easiest way I can think of
     *
     * @param options A list of calibration filenames for the given subject
     * @param subjectID The subject to whom the "options" files belong
     */
    public void getSelectionAndInitialize(final String[] options, final int subjectID) {
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
                    inFile = fileController.getCalibFileFromName(subjectID, getDialogSelectedString());
                } catch (FileNotFoundException e) {
                    showErrorDialog("Unknown error: selected file not found in directory");
                    e.printStackTrace();
                    return;
                }
                FileNameController.initializeModelFromFileData(inFile, model);
                masterController.setMode(MasterController.Mode.MAIN);
            }
        });
    }

    public void setDialogSelectedItem(String item) {
       this.dialogSelectedString = item;
    }

    public String getDialogSelectedString() {
        return this.dialogSelectedString;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_EXT_WRITE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Permission successfully granted");
            }
            else System.out.println("Permission not granted");
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
