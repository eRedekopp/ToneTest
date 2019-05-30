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
import android.widget.NumberPicker;

import java.io.File;

/**
 * Functions as a View and also performs initial setup
 *
 * @author redekopp
 */
public class MainActivity extends AppCompatActivity implements ModelListener {

    public static final int REQUEST_EXT_WRITE = 1;

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestController controller;
    FileNameController fileController;

    Button rampButton, pureButton, heardButton, saveButton, confidenceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // get read/write permissions
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                           REQUEST_EXT_WRITE);

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
                    showWarningDialog("No configurations saved for subject with id " + entry);
                else {
                    String[] subjectFileNames = newFController.getFileNamesFromCalibDir(entry);
                    if (subjectFileNames.length == 0)
                        showWarningDialog("No configurations saved for subject with id " + entry);
                    else {
                        String fileName = getStringSelectionFromDialog(subjectFileNames);
                        File inFile = newFController.getFileFromName(entry, fileName);
                        FileNameController.initializeModelFromFileData(inFile, newModel);
                        newMController.setMode(MasterController.Mode.MAIN);
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

    public void showWarningDialog(String message) {
        // todo
    }

    public String getStringSelectionFromDialog(String[] options) {
        // todo
        return "";
    }

    /**
     * Prompt the user to input the ID number for the current test subject and update the model
     * with that number
     *
     * To be performed during initial setup only
     */
    private void setModelSubjectId() { // todo: not good mvc design
        // set up number picker object
        final NumberPicker numSelect = new NumberPicker(this);
        numSelect.setMinValue(0);
        numSelect.setMaxValue(100);
        numSelect.setWrapSelectorWheel(true);
        numSelect.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                model.setSubjectId(newVal); // change model ID every time the value changes until
            }                               // dialog box is closed
        });

        // set up dialog box
        AlertDialog.Builder entryBuilder = new AlertDialog.Builder(this);
        entryBuilder.setTitle("Enter Subject ID");
        entryBuilder.setView(numSelect);
        entryBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                warnIfSubjectIdAlreadyUsed(model.getSubjectId()); // warn if reusing ID
            }
        });
        entryBuilder.show();
    }

    /**
     * Show a dialog box warning the user if the given subject ID already exists within the
     * directory structure
     *
     * @param id The subject ID to be checked
     */
    private void warnIfSubjectIdAlreadyUsed(int id) {
        if (this.fileController.directoryExistsForSubject(id)) {
            System.out.println("printing warning message");
            AlertDialog.Builder warnBuilder = new AlertDialog.Builder(this);
            warnBuilder.setMessage("Warning: subject ID " + id + " has already been used");
            warnBuilder.show();
        }
        else System.out.println("New subject id: " + id);
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
