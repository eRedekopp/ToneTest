package ca.usask.cs.tonesetandroid;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

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
public class MainActivity extends AppCompatActivity implements ModelListener, HearingTestView, SensorEventListener {

    public static final int REQUEST_PERMISSIONS = 1;

    /*
    * Android Studio doesn't like that there's a Context in a static field, but since this is such a small
    * application, the memory leaking issues shouldn't be much of a problem
    * */
    public static Context context; // for passing to other classes from inner methods

    // mvc elements
    Model model;
    HearingTestInteractionModel iModel;
    HearingTestController controller;
    FileIOController fileController;
    BackgroundNoiseController noiseController;

    // instance variables for keeping track of the state of pre-test dialog boxes
    private int dialogSelectedItem;
    private int dialogTestTypeID;
    private int dialogNoiseID;
    private int dialogVolume;

    // ui elements
    Button  calibButton,
            upButton,
            downButton,
            flatButton,
            heardButton,
            confidenceButton,
            resetButton,
            pauseButton;

    // vars for accelerometer / volume rocker
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastAccelEventTime = 0,
                 lastShakeTime      = 0,
                 lastUpClickTime    = 0,
                 lastDownClickTime  = 0;
    private float last_x = 0,
                  last_y = 0,
                  last_z = 0;
    private static final int SHAKE_SENSITIVITY = 250;

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

        // set up accelerometer
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

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
                getTestTypeAndBegin(true);
            }
        });
        confidenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTestTypeAndBegin(false);
            }
        });

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleUpClick(true);
            }
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleDownClick(true);
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
                controller.handleHeardClick(true);
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
     * Enable/disable the buttons given the new status of the Model and iModel, and check if a test needs to be
     * started / resumed
     */
    public void modelChanged() {
        // Always run on UI thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                // disable all response buttons
                for (Button b : new Button[]{upButton, downButton, flatButton, heardButton}) b.setEnabled(false);

                // re-enable response buttons as necessary, if a test is currently happening
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

                // set other buttons
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

    /**
     * Register shake events with accelerometer
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long curTime = System.currentTimeMillis();
            long timeDiff = curTime - this.lastAccelEventTime;

            if (timeDiff > 200) {   // only accept sensor events every 200ms

                this.lastAccelEventTime = curTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float speed = Math.abs(x + y + z - this.last_x - this.last_y - this.last_z) / timeDiff * 10000;

                if (speed > SHAKE_SENSITIVITY && curTime - this.lastShakeTime > 250) {  // only allow shake events
                    this.lastShakeTime = curTime;                                       // every 250ms
                    Log.d("onSensorChanged", "shake registered");
                    this.controller.handleHeardClick(false);
                }

                this.last_x = x;
                this.last_y = y;
                this.last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not used
    }

    /**
     * Override volume rocker functionality
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long curTime = System.currentTimeMillis();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (curTime - this.lastDownClickTime > 250) { // only allow clicks every 250ms
                this.controller.handleDownClick(false);
                this.lastDownClickTime = curTime;
                return true;
            } else return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (curTime - this.lastUpClickTime > 250) {    // only allow clicks every 250ms
                this.controller.handleUpClick(false);
                this.lastUpClickTime = curTime;
                return true;
            } else return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * Clear the mvc elements and go to the "login" screen
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
        final String pathName = data.getStringExtra("pathName");

        if (subjectID < 0) throw new IllegalArgumentException("Found invalid subject ID number: " + subjectID);

        this.model.reset();
        this.model.setSubjectId(subjectID);
        if (!pathName.equals(""))
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileIOController.initializeModelFromFile(model, new File(pathName));
                    }
                }).start();
                waitUntilLoadingComplete();
            } catch (RuntimeException e) {
                showErrorDialog("Unable to read file", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        goToInit();
                    }
                });
                e.printStackTrace();
            }
        this.modelChanged();
    }

    /**
     * This gets called after asking for permissions: show an error message if something went wrong, else do nothing
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            Log.e("mainActivity", "Permission not granted");
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Show a dialog prompting the user to select a test type, then call the next step to setStartTime the test
     *
     * @param isCalib Is the new test to be started a calibration test? If not, it is a confidence test
     */
    private void getTestTypeAndBegin(final boolean isCalib) {
        AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
        setDialogSelectedItem(0);
        optBuilder.setTitle("Please select the type of test you wish to begin");
        optBuilder.setSingleChoiceItems(
                isCalib ? HearingTestController.CALIB_TEST_OPTIONS : HearingTestController.CONF_TEST_OPTIONS,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDialogSelectedItem(which);
                    }
                });
        optBuilder.setNegativeButton("Cancel", null);
        optBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDialogTestTypeID();
                dialog.cancel();
                getBackgroundNoiseAndBeginTest(isCalib);
            }
        });
        optBuilder.show();
    }

    /**
     * Show a dialog prompting the user to select a background noise type, then call the next step to setStartTime the test
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
        setDialogSelectedItem(0);
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setDialogNoiseID();
                dialogInterface.cancel();
                getNoiseVolAndBeginTest(isCalib);
            }
        });
        builder.setTitle("Select the background noise type for this test");
        builder.show();
    }

    /**
     * Show a dialog prompting the user for a background noise volume if necessary, then call the appropriate method
     * in HearingTestController to begin the test
     *
     * @param isCalib Is the new test to be started a calibration test? If not, it is a confidence test
     */
    private void getNoiseVolAndBeginTest(final boolean isCalib) {
        if (this.dialogNoiseID == BackgroundNoiseType.NOISE_TYPE_NONE) {    // set volume to 0 and continue if no noise
            this.setDialogSelectedItem(0);
            this.setDialogVolume();
            BackgroundNoiseType noiseType = new BackgroundNoiseType(dialogNoiseID, dialogVolume);
            if (isCalib) controller.handleCalibClick(noiseType, dialogTestTypeID);
            else controller.handleConfClick(noiseType, dialogTestTypeID);
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
                                getNoiseVolAndBeginTest(isCalib);
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
                                        getNoiseVolAndBeginTest(isCalib);
                                    }
                                });
                    } else {
                        setDialogVolume();
                        dialogInterface.cancel();
                        BackgroundNoiseType noiseType = new BackgroundNoiseType(dialogNoiseID, dialogVolume);
                        if (isCalib) controller.handleCalibClick(noiseType, dialogTestTypeID);
                        else controller.handleConfClick(noiseType, dialogTestTypeID);
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

    @Override
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

    @Override
    public void showSampleDialog(final Runnable r, final String message) {
        final String sampleMessage =
                "To hear a sample of the tones that will be played in this test, press the \"Play Samples\" button. " +
                "Once you are familiar with the tones, press the \"Done\" button";


        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                iModel.setTestPaused(true);
                AlertDialog.Builder infoBuilder = new AlertDialog.Builder(context);
                infoBuilder.setTitle("Information");
                infoBuilder.setCancelable(false);
                MessageButtonView mesBut = new MessageButtonView(context);
                mesBut.setButtonAction(r);
                mesBut.setButtonText("Play Samples");
                mesBut.setMessageText(message + ' ' + sampleMessage);

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
     * Show an un-cancelable AlertDialog that says "loading" and automatically dismisses itself when model.hasResults()
     * becomes true
     */
    public void waitUntilLoadingComplete() {
        final AlertDialog alertDialog;

        AlertDialog.Builder waitBuilder = new AlertDialog.Builder(this);
        waitBuilder.setTitle("Please Wait");
        waitBuilder.setMessage("Loading...");
        waitBuilder.setCancelable(false);

        alertDialog = waitBuilder.create();

        Thread listener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (! model.hasResults()) continue;
                alertDialog.cancel();
                model.notifySubscribers();
            }
        });

        alertDialog.show();
        listener.start();
    }

    // Keep track of dialogs in GetBackgroundNoiseAndBeginTest - had to do it this way because of scoping issues
    private void setDialogSelectedItem(int dialogSelectedItem) {
        this.dialogSelectedItem = dialogSelectedItem;
    }

    private void setDialogNoiseID() {
        this.dialogNoiseID = this.dialogSelectedItem;
    }

    private void setDialogVolume() {
        this.dialogVolume = this.dialogSelectedItem;
    }

    public void setDialogTestTypeID() {
        this.dialogTestTypeID = this.dialogSelectedItem;
    }

    // mutators for mvc elements
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
}
