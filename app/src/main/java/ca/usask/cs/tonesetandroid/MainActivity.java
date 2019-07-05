package ca.usask.cs.tonesetandroid;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileNotFoundException;


/**
 * Launcher for whole application and view for main menu
 *
 * @author redekopp
 */
public class MainActivity extends AppCompatActivity implements ModelListener, HearingTestView {

    public static final int REQUEST_PERMISSIONS = 1;

    private Context context = this; // for passing to other classes from inner methods

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestController controller;
    FileNameController fileController;

    private int dialogSelectedItem;  // for selecting background noise configurations
    private int dialogNoiseID;
    private int dialogVolume;

    Button  calibButton,
            upButton,
            downButton,
            heardButton,
            saveCalibButton,
            saveConfButton,
            confidenceButton,
            resetButton,
            pauseButton /*,
            autoButton*/;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // get read/write/microphone permissions
        if (Build.VERSION.SDK_INT >= 23)
            requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_PERMISSIONS);

        // instantiate self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create mvc elements
        final Model newModel = new Model();
        HearingTestInteractionModel newIModel = new HearingTestInteractionModel();
        HearingTestController newController = new HearingTestController();
        final FileNameController newFController = new FileNameController();
        BackgroundNoiseController newNoiseController = new BackgroundNoiseController(this);

        // set up relations
        this.setFileController(newFController);
        this.setController(newController);
        this.setModel(newModel);
        this.setIModel(newIModel);
        this.model.addSubscriber(this);
        this.iModel.addSubscriber(this);
        this.controller.setModel(newModel);
        this.controller.setiModel(newIModel);
        this.controller.setView(this);
        this.fileController.setModel(this.model);
        this.controller.setNoiseController(newNoiseController);
        newNoiseController.setModel(this.model);

        // set up view elements for main screen
        calibButton =       findViewById(R.id.calibButton);
        downButton =        findViewById(R.id.downButton);
        upButton =          findViewById(R.id.upButton);
        heardButton =       findViewById(R.id.heardButton);
        saveCalibButton =   findViewById(R.id.saveCalibButton);
        saveConfButton =    findViewById(R.id.saveConfButton);
        confidenceButton =  findViewById(R.id.confidenceButton);
        resetButton =       findViewById(R.id.resetButton);
        pauseButton =       findViewById(R.id.pauseButton);
//        autoButton =        findViewById(R.id.autoButton);

        // set up event listeners for main screen
        calibButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                model.reset();
                getBackgroundNoiseAndBeginTest(true);
            }
        });
        confidenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.resetConfidenceResults();
                getBackgroundNoiseAndBeginTest(false);
            }
        });
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleUpClick();
            }
        });
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleDownClick();
            }
        });
        heardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleHeardClick();
            }
        });
        saveCalibButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    fileController.handleSaveCalibClick(context);
                    model.setResultsSaved(true);
                } catch (IllegalStateException e) {
                    showErrorDialog("No results currently stored");
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    showErrorDialog("Unable to create target file");
                    e.printStackTrace();
                }
            }
        });
        saveConfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    fileController.handleConfSaveClick(context);
                    model.setConfResultsSaved(true);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    showErrorDialog("No results currently stored");
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    showErrorDialog("Unable to create target file");
                }
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show warning dialog, then reset and return to InitActivity if user confirms
                AlertDialog.Builder warningBuilder = new AlertDialog.Builder(context);
                warningBuilder.setTitle("Warning");
                warningBuilder.setMessage("All unsaved results will be lost. Continue?");
                warningBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        goToInit();
                    }
                });
                warningBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                warningBuilder.show();
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.testPaused()) {
                    model.setTestPaused(false);
                } else {
                    model.setTestPaused(true);
                }
            }
        });
//        autoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showErrorDialog("This method is not complete and does not affect calibration");
//                goToAuto();
//            }
//        });

        // configure audio
        model.setAudioManager((AudioManager) this.getSystemService(Context.AUDIO_SERVICE));
        model.setUpLineOut();

        // Initialize model with InitActivity, then onActivityResult will call modelChanged() and set up this screen
        this.goToInit();
    }

    /**
     * Enable/disable the buttons given the new status of the Model and iModel
     */
    public void modelChanged() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {  // always run on UI thread
            @Override
            public void run() {
                // choose which test button to use depending on which test phase we are in
                if (model.getTestPhase() == Model.TEST_PHASE_RAMP || model.getTestPhase() == Model.TEST_PHASE_REDUCE) {
                    heardButton.setVisibility(View.VISIBLE);
                    heardButton.setEnabled(true);
                    upButton.setVisibility(View.GONE);
                    upButton.setEnabled(false);
                    downButton.setVisibility(View.GONE);
                    downButton.setEnabled(false);
                } else {
                    heardButton.setVisibility(View.GONE);
                    heardButton.setEnabled(false);
                    upButton.setVisibility(View.VISIBLE);
                    upButton.setEnabled(model.testing() && ! model.testPaused());
                    downButton.setVisibility(View.VISIBLE);
                    downButton.setEnabled(model.testing() && ! model.testPaused());
                }

                calibButton.setEnabled(!model.testing());
                confidenceButton.setEnabled(model.hasResults() && !model.testing());
                saveCalibButton.setEnabled(model.hasResults() && !model.testing() && !model.resultsSaved());
                saveConfButton.setEnabled(model.hasConfResults() && !model.testing() && !model.confResultsSaved());
                resetButton.setEnabled(!model.testing() || model.testPaused());
                pauseButton.setEnabled(model.testing());

                controller.checkForHearingTestResume(); // resume hearing test if necessary
            }
        });
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
        if (model.testing()) model.setTestPhase(Model.TEST_PHASE_NULL);
        Intent initIntent = new Intent(this, InitActivity.class);
        int reqCode = 1;
        startActivityForResult(initIntent, reqCode);
    }

    /**
     * Performs an autoTest and sets HearingTestResults, then displays the current noise to a graph in a GraphActivity
     */
    private void goToAuto() {

        // todo implement autoTest later on

        FreqVolPair[] periodogram = controller.getPeriodogramFromLineIn(2048);
        GraphActivity.setData(periodogram);

        Intent graphIntent = new Intent(this, GraphActivity.class);
        startActivity(graphIntent);
    }

    /**
     * Handle data received from InitActivity
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int subjectID = data.getIntExtra("subjectID", -1);
        String pathName = data.getStringExtra("pathName");

        if (subjectID < 0) throw new IllegalArgumentException("Found invalid subject ID number: " + subjectID);

        this.model.reset();
        this.model.setSubjectId(subjectID);
        if (!pathName.equals(""))
            try {
                FileNameController.initializeModelFromFileData(pathName, this.model);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        model.printResultsToConsole();
        this.modelChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Main","Permission successfully granted");
            }
            else Log.i("Main", "Permission not granted");
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Show dialogs to get a background noise type from the user, then pass them to the model and begin the
     * appropriate test
     *
     * @param isCalib Is the new test to be started a calibration test? If not, it is a confidence test
     */
    private void getBackgroundNoiseAndBeginTest(final boolean isCalib) {
        // show dialog to get noise type
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setSingleChoiceItems(BackgroundNoiseType.NOISE_TYPE_STRINGS, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setDialogSelectedItem(i);
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setDialogNoiseID();
                Log.d("mainActivity", "noise type ID set as " + dialogNoiseID);
                dialogInterface.cancel();
                getBackgroundNoiseAndBeginTest_2(isCalib);
            }
        });
        builder.setTitle("Select the background noise type for this test");
        builder.show();
    }

    /**
     * Show the second dialog (if required) for beginning a background noise test. This method should only be called
     * by getBackgroundNoiseAndBeginTest
     */
    private void getBackgroundNoiseAndBeginTest_2(final boolean isCalib) {
        if (this.dialogNoiseID == BackgroundNoiseType.NOISE_TYPE_NONE) {    // set volume to 0 and continue if no noise
            this.setDialogSelectedItem(0);
            this.setDialogVolume();
            Log.d("mainActivity", "noise volume set as " + dialogNoiseID);
            getBackgroundNoiseAndBeginTest_3(isCalib);
        } else {                                                            // else get volume from user
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final EditText editText = new EditText(context);
            editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            editText.setText("0");
            builder.setView(editText);
            builder.setTitle("Please enter the volume of the noise for this test");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int oldDialogSelectedItem = dialogSelectedItem;
                    try {
                        setDialogSelectedItem(Integer.parseInt(editText.getText().toString()));
                    } catch (NumberFormatException e) {
                        dialogInterface.cancel();
                        showErrorDialog("Unable to parse input", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                getBackgroundNoiseAndBeginTest_2(isCalib);
                            }
                        });
                        return;
                    }
                    if (dialogSelectedItem > 100 || dialogSelectedItem < 0) { // ensure number in proper range
                        setDialogSelectedItem(oldDialogSelectedItem);
                        dialogInterface.cancel();
                        showErrorDialog("Volume out of range: please enter a value from 0 to 100",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                        getBackgroundNoiseAndBeginTest_2(isCalib);
                                    }
                                });
                    } else {
                        setDialogVolume();
                        dialogInterface.cancel();
                        getBackgroundNoiseAndBeginTest_3(isCalib);
                    }
                }
            });
            builder.show();
        }
    }

    /**
     * Setup for test and ask user to press OK to begin. This method should only be called by
     * getBackgroundNoiseAndBeginTest_2
     */
    private void getBackgroundNoiseAndBeginTest_3(final boolean isCalib) {
        // prompt user to press OK to begin test
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Press the button when you are ready to begin the test");
        builder.setPositiveButton("BEGIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                BackgroundNoiseType noiseType = new BackgroundNoiseType(dialogNoiseID, dialogVolume);
                if (isCalib) {
                    model.hearingTestResults.setNoiseType(noiseType);
                    controller.handleCalibClick();
                }
                else {
                    model.confidenceTestResults.setNoiseType(noiseType);
                    controller.handleConfClick();
                }
            }
        });
        builder.show();
    }


    /**
     * Show a dialog with title "Error" and the given message
     * @param message The message to be displayed
     */
    public void showErrorDialog(String message, DialogInterface.OnClickListener onOKClick) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Error");
        warningBuilder.setMessage(message);
        warningBuilder.setPositiveButton("OK", onOKClick);
        warningBuilder.show();
    }

    public void showErrorDialog(String message) {
        showErrorDialog(message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
    }


    /**
     * Show a dialog with the title "Information" and the given message
     *
     * This method sets model.testPaused to true just to be sure, but for concurrency reasons it will likely have to
     * be set to true in the caller thread as well
     *
     * @param message The message to be displayed
     */
    public void showInformationDialog(String message) {
        model.setTestPaused(true);
        AlertDialog.Builder infoBuilder = new AlertDialog.Builder(this);
        infoBuilder.setTitle("Information");
        infoBuilder.setMessage(message);
        infoBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        infoBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                model.setTestPaused(false);
            }
        });
        infoBuilder.show();
    }

    public void setDialogSelectedItem(int dialogSelectedItem) {
        this.dialogSelectedItem = dialogSelectedItem;
    }

    public void setDialogNoiseID() {
        this.dialogNoiseID = this.dialogSelectedItem;
    }

    public void setDialogVolume() {
        this.dialogVolume = this.dialogSelectedItem;
    }
}
