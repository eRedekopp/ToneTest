package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

/**
 * Setup view for confidence activity
 *
 * @author redekopp, alexscott
 */
public class ConfidenceActivity extends Activity implements ModelListener {

    private static Model model;

    private static FileNameController fileController;

    private Context context = this;  // for calling FileNameController save methods

    private ConfidenceInteractionModel iModel;

    private ConfidenceController confController;

    private Button yesButton, noButton, confSaveButton, exitButton;

    private String dialogSelectedItem; // for getSubsetFromUser()

    public static void setModel(Model aModel) {  // should be called from previous activity
        model = aModel;
    }

    public static void setFileController(FileNameController fController) {
        fileController = fController;
    }

    public void setiModel(ConfidenceInteractionModel iModel) {
        this.iModel = iModel;
    }

    public void setConfController(ConfidenceController confController) {
        this.confController = confController;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confidence_view);

        ConfidenceInteractionModel newIModel = new ConfidenceInteractionModel();
        ConfidenceController newConfController = new ConfidenceController();
        this.setiModel(newIModel);
        this.setConfController(newConfController);
        confController.setIModel(newIModel);
        confController.setModel(model);
        iModel.addSubscriber(this);
        model.addSubscriber(this);

        yesButton = findViewById(R.id.yesButton);
        noButton  = findViewById(R.id.noButton);
        confSaveButton = findViewById(R.id.confSaveButton);
        exitButton = findViewById(R.id.exitButton);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confController.handleYesClick();
            }
        });
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confController.handleNoClick();
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confController.handleExitClick();
                goBack();
            }
        });
        confSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileController.handleConfSaveClick(context);
                iModel.setResultsSaved(true);
            }
        });

        this.modelChanged();
        this.getSubsetAndBeginTest();
    }

    private void goBack() {
        this.finish();  // shouldn't need to pass anything since this method directly affects the Model
    }

    /**
     * Opens a dialog prompting the user to select a subset.
     * Does not support custom frequencies: must be hard-coded into this method
     *
     * @return A subset of model.HearingTestResults as selected by the user
     */
    @SuppressWarnings("unchecked")
    private void getSubsetAndBeginTest() {
        final String[] options = {
                "All",
                "200Hz, 1kHz, 4kHz",
                "500Hz, 2kHz, 8kHz",
                "500Hz, 4kHz",
                "200Hz, 2kHz"
        };
        AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
        optBuilder.setTitle("Select subset of test results from which to generate estimated volumes");
//        optBuilder.setMessage("Generate volume estimates using which subset of the hearing test results?");
        optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialogSelectedItem = options[which];
            }
        });
        optBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        optBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ArrayList<FreqVolPair> subset;

                switch (dialogSelectedItem) {
                    case "All":
                        subset = (ArrayList<FreqVolPair>) model.hearingTestResults.clone(); // all frequencies
                    case "200Hz, 1kHz, 4kHz":
                        float[] freqs2 = {200, 1000, 4000};
                        subset = confController.subsetTestResults(freqs2);
                        break;
                    case "500Hz, 2kHz, 8kHz":
                        float[] freqs3 = {500, 2000, 8000};
                        subset = confController.subsetTestResults(freqs3);
                        break;
                    case "500Hz, 4kHz":
                        float[] freqs4 = {500, 4000};
                        subset = confController.subsetTestResults(freqs4);
                        break;
                    case "200Hz, 2kHz":
                        float[] freqs5 = {200, 2000};
                        subset = confController.subsetTestResults(freqs5);
                        break;
                    default:
                        throw new RuntimeException("Found unexpected selection value: " + dialogSelectedItem);
                }
                confController.beginConfidenceTest(subset);
            }
        });
        optBuilder.show();
    }

    public void modelChanged() {
        yesButton.setEnabled(iModel.yesEnabled);
        noButton.setEnabled(iModel.noEnabled);
        confSaveButton.setEnabled(iModel.saveEnabled && !iModel.resultsSaved);  // do not allow save if results already
        exitButton.setEnabled(true);                                            // saved
    }
}
