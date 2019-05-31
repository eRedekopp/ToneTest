package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

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
        confSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileController.handleConfSaveClick(context);
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! iModel.saveEnabled) model.clearConfidenceTestResults(); // clear results if exit during test
                // todo this doesn't work: figure out how to kill the thread and close the Audiotrack on exit
                if (confController.testThread.isAlive()) confController.testThread.interrupt();
                goBack();
            }
        });

        this.modelChanged();
        confController.beginConfidenceTest();
    }

    private void goBack() {
        this.finish();  // shouldn't need to pass anything since this method directly affects the Model
    }

    public void modelChanged() {
        yesButton.setEnabled(iModel.yesEnabled);
        noButton.setEnabled(iModel.noEnabled);
        confSaveButton.setEnabled(iModel.saveEnabled);
        exitButton.setEnabled(true);
    }
}
