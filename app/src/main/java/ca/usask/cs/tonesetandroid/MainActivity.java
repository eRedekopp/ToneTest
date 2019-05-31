package ca.usask.cs.tonesetandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileNotFoundException;


/**
 * Launch application, setup main menu
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
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXT_WRITE);

        // instantiate self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create mvc elements
        final Model newModel = new Model();
        HearingTestInteractionModel newIModel = new HearingTestInteractionModel();
        HearingTestController newController = new HearingTestController();
        final FileNameController newFController = new FileNameController(this);

        // set up relations
        this.setFileController(newFController);
        this.setController(newController);
        this.setModel(newModel);
        this.setIModel(newIModel);
        this.model.addSubscriber(this);
        this.iModel.addSubscriber(this);
        this.controller.setModel(newModel);
        this.controller.setiModel(newIModel);
        this.fileController.setModel(this.model);

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
        confidenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToConfidence();
            }
        });

        // Initialize model with InitActivity, then onActivityResult will call modelChanged() and set up this screen
        this.goToInit();
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

    /**
     * Starts an InitActivity which initializes the model with an ID number and possibly data
     */
    private void goToInit() {
        // declare intent
        Intent initIntent = new Intent(this, InitActivity.class);
        int reqCode = 1;

        startActivityForResult(initIntent, reqCode);
    }

    private void goToConfidence() {
        Intent confIntent = new Intent(this, ConfidenceActivity.class);
        confIntent.putExtra("model", this.model);

        startActivity(confIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int subjectID = data.getIntExtra("subjectID", -1);
        String pathName = data.getStringExtra("pathName");

        if (subjectID < 0) throw new IllegalArgumentException("Found invalid subject ID number: " + subjectID);

        this.model.setSubjectId(subjectID);
        if (!pathName.equals(""))
            try {
                FileNameController.initializeModelFromFileData(pathName, this.model);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        this.modelChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_EXT_WRITE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Main","Permission successfully granted");
            }
            else Log.i("Main", "Permission not granted");
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
