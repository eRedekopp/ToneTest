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

import java.io.File;
import java.io.FileNotFoundException;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseController;
import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.FileIOController;
import ca.usask.cs.tonesetandroid.Control.HearingTestController;
import ca.usask.cs.tonesetandroid.Control.HearingTestInteractionModel;
import ca.usask.cs.tonesetandroid.Control.Model;
import ca.usask.cs.tonesetandroid.Control.ModelListener;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;


/**
 * Launcher for whole application and view for main menu
 *
 * @author redekopp
 */
public class MainActivity extends AppCompatActivity implements ModelListener, HearingTestView {

    public static final int REQUEST_PERMISSIONS = 1;

    public static Context context; // for passing to other classes from inner methods

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestController controller;
    FileIOController fileController;
    BackgroundNoiseController noiseController;

    private int dialogSelectedItem;  // for selecting background noise configurations
    private int dialogNoiseID;
    private int dialogVolume;

    Button  calibButton,
            upButton,
            downButton,
            flatButton,
            heardButton,
            confidenceButton,
            resetButton,
            pauseButton;

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
        context = this;

        // Create mvc elements
        Model newModel = new Model();
        HearingTestInteractionModel newIModel = new HearingTestInteractionModel();
        HearingTestController newController = new HearingTestController();
        FileIOController newFController = new FileIOController();
        BackgroundNoiseController newNoiseController = new BackgroundNoiseController(this);

        // set up relations
        this.setNoiseController(newNoiseController);    // this
        this.setController(newController);
        this.setIModel(newIModel);
        this.setModel(newModel);
        this.setFileController(newFController);

        this.fileController.setModel(this.model);       // FileIOController
        this.fileController.setiModel(this.iModel);
        this.fileController.setContext(this);

        this.noiseController.setiModel(this.iModel);    // BackgroundNoiseController
        this.noiseController.setContext(this);

        this.controller.setModel(this.model);           // HearingTestController
        this.controller.setiModel(this.iModel);
        this.controller.setFileController(this.fileController);
        this.controller.setNoiseController(this.noiseController);
        this.controller.setContext(this);
        this.controller.setView(this);

        HearingTest.setModel(this.model);               // HearingTest
        HearingTest.setContext(this);
        HearingTest.setController(this.controller);
        HearingTest.setFileController(this.fileController);
        HearingTest.setIModel(this.iModel);
        HearingTest.setView(this);

        this.model.addSubscriber(this);                 // model listeners
        this.iModel.addSubscriber(this);

        // set up view elements for main screen
        calibButton =       findViewById(R.id.calibButton);
        downButton =        findViewById(R.id.downButton);
        upButton =          findViewById(R.id.upButton);
        flatButton =        findViewById(R.id.flatButton);
        heardButton =       findViewById(R.id.heardButton);
        confidenceButton =  findViewById(R.id.confidenceButton);
        resetButton =       findViewById(R.id.resetButton);
        pauseButton =       findViewById(R.id.pauseButton);

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

        flatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.handleFlatClick();
            }
        });

        heardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleHeardClick();
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
                if (iModel.testPaused()) {
                    iModel.setTestPaused(false);
                } else {
                    iModel.setTestPaused(true);
                }
            }
        });

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
        // Always run on UI thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                // disable all response buttons
                for (Button b : new Button[]{upButton, downButton, flatButton, heardButton}) b.setEnabled(false);

                // enable response buttons as necessary, if a test is currently happening
                if (iModel.testing())
                    for (int option : iModel.getCurrentRequiredButtons()) {
                        switch (option) {
                            case HearingTest.ANSWER_DOWN:
                                downButton.setEnabled(! iModel.testPaused());
                                break;
                            case HearingTest.ANSWER_UP:
                                upButton.setEnabled(! iModel.testPaused());
                                break;
                            case HearingTest.ANSWER_FLAT:
                                flatButton.setEnabled(! iModel.testPaused());
                                break;
                            case HearingTest.ANSWER_HEARD:
                                heardButton.setEnabled(! iModel.testPaused());
                                break;
                            default: throw new RuntimeException("Unknown option value found: " + option);
                        }
                    }

                // set other buttons depending on current state
                calibButton.setEnabled(!iModel.testing());
                confidenceButton.setEnabled(model.hasResults() && !iModel.testing());
                resetButton.setEnabled(!iModel.testing() || iModel.testPaused());
                pauseButton.setEnabled(iModel.testing());
                pauseButton.setText(iModel.testPaused() ? "Resume" : "Pause");

                // resume hearing test if necessary
                controller.checkForHearingTestResume();
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

    public void setFileController(FileIOController fileController) {
        this.fileController = fileController;
    }

    public void setNoiseController(BackgroundNoiseController noiseController) {
        this.noiseController = noiseController;
    }

    /**
     * Starts an InitActivity which initializes the model with an ID number and possibly data
     */
    private void goToInit() {
        Intent initIntent = new Intent(this, InitActivity.class);
        int reqCode = 1;
        this.model.reset();
        this.iModel.reset();
        startActivityForResult(initIntent, reqCode);
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
                FileIOController.initializeModelFromFile(this.model, new File(pathName));
            } catch (RuntimeException e) {
                showErrorDialog("Unable to read file", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        goToInit();
                    }
                });
                e.printStackTrace();
            }

        model.printResultsToConsole();
        this.model.notifySubscribers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("mainActivity","Permission successfully granted");
            }
            else Log.i("mainActivity", "Permission not granted");
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
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", null);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setDialogNoiseID();
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
            BackgroundNoiseType noiseType = new BackgroundNoiseType(dialogNoiseID, dialogVolume);
            if (isCalib) controller.handleCalibClick(noiseType);
            else controller.handleConfClick(noiseType);
        } else {                                                            // else get volume from user
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final EditText editText = new EditText(context);
            editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            editText.setText("0");
            builder.setView(editText);
            builder.setTitle("Please enter the volume of the noise for this test");
            builder.setCancelable(false);
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
                        BackgroundNoiseType noiseType = new BackgroundNoiseType(dialogNoiseID, dialogVolume);
                        if (isCalib) controller.handleCalibClick(noiseType);
                        else controller.handleConfClick(noiseType);
                    }
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
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
    public void showInformationDialog(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                iModel.setTestPaused(true);
                AlertDialog.Builder infoBuilder = new AlertDialog.Builder(context);
                infoBuilder.setTitle("Information");
                infoBuilder.setMessage(message);
                infoBuilder.setCancelable(false);
                infoBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                infoBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        iModel.setTestPaused(false);
                    }
                });
                infoBuilder.show();
            }
        });
    }

    public void showSampleDialog(final Runnable r, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                // todo make sure that background noise actually starts

                iModel.setTestPaused(true);
                AlertDialog.Builder infoBuilder = new AlertDialog.Builder(context);
                infoBuilder.setTitle("Information");
                infoBuilder.setCancelable(false);
                MessageButtonView mesBut = new MessageButtonView(context);
                mesBut.setButtonAction(r);
                mesBut.setButtonText("Play Samples");
                mesBut.setMessageText(message);

                infoBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        iModel.setTestPaused(false);
                        while (iModel.sampleThreadActive()) continue;  // idle until sample finishes playing
                        noiseController.playNoise(iModel.getCurrentNoise());
                    }
                });

                infoBuilder.setView(mesBut);
                infoBuilder.show();
            }
        });
    }

    /**
     * Keep track of dialogs in GetBackgroundNoiseAndBeginTest - had to do it this way because of scoping issues
     */
    private void setDialogSelectedItem(int dialogSelectedItem) {
        this.dialogSelectedItem = dialogSelectedItem;
    }

    private void setDialogNoiseID() {
        this.dialogNoiseID = this.dialogSelectedItem;
    }

    private void setDialogVolume() {
        this.dialogVolume = this.dialogSelectedItem;
    }
}
