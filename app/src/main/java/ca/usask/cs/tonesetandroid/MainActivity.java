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
import android.text.InputType;
import android.util.Log;
import android.view.View;
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

    Button  calibButton,
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

        // set up view elements for main screen
        calibButton =       findViewById(R.id.calibButton);
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
                controller.handleCalibClick();
            }
        });
        confidenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.handleConfClick();
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
                // get noise type from user, then save after user presses OK
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            fileController.handleSaveCalibClick(context, input.getText().toString());
                            model.setResultsSaved(true);
                        } catch (IllegalStateException e) {
                            showErrorDialog("No results currently stored");
                        } catch (RuntimeException e) {
                            showErrorDialog("Unable to create target file");
                        } finally {
                            dialog.cancel();
                        }
                    }
                });
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
                calibButton.setEnabled(!model.testing());
                heardButton.setEnabled(model.testing() && ! model.testPaused() );
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
}
