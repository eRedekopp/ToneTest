package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.util.Log;

import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SineCalibratonTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SineRampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SineReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.SingleSineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTestView;

/**
 * A class for performing PureTone and RampUp tests, and handling clicks on the main menu
 *
 * @author redekopp, alexscott
 */
public class HearingTestController {

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestView view;
    BackgroundNoiseController noiseController;
    FileIOController fileController;

    Context context;

    ////////////////////////////////////////// control /////////////////////////////////////////////

    public void checkForHearingTestResume() {
        if (! iModel.testThreadActive() && iModel.getCurrentTest() != null && ! iModel.testPaused())
            iModel.getCurrentTest().checkForHearingTestResume();
    }

    /**
     * Begin the full calibration test. iModel.rampTest must be configured before calling this method. This method is
     * only used to begin a new calibration test directly after getting user to input test information. To resume a
     * test, use checkForHearingTestResume
     */
    public void calibrationTest() {
        this.model.configureAudio();
        this.iModel.setTestPaused(true);
        this.iModel.setCurrentTest(this.iModel.getRampTest());
        this.noiseController.playNoise(this.iModel.getCurrentNoise());
        this.fileController.startNewSaveFile(true);
        this.view.showInformationDialog(this.iModel.getCurrentTest().getTestInfo());
    }

    /**
     * Perform any final actions that need to be done before the calibration test is officially "complete"
     */
    public void calibrationTestComplete() {  // save results to model, write any necessary output to file, etc.
        this.fileController.closeFile();
        this.model.setCalibrationTestResults(this.iModel.getCalibrationResults());
        this.iModel.reset();
        this.model.printResultsToConsole();
        this.model.audioTrackCleanup();
        this.iModel.notifySubscribers();
    }

    /**
     * Begin a confidence test. Calibration results must be saved and iModel.confidenceTest must be configured before
     * calling this method. This method is only used to begin a new confidence test directly after getting user to
     * input test information. To resume a test, use checkForHearingTestResume
     */
    public void confidenceTest() {
        this.model.configureAudio();
        this.iModel.setTestPaused(true);
        this.iModel.setCurrentTest(this.iModel.getConfidenceTest());
        this.fileController.startNewSaveFile(false);
        this.view.showSampleDialog( this.iModel.getConfidenceTest().sampleTones(),
                                    this.iModel.getCurrentTest().getTestInfo());
    }

    public void confidenceTestComplete() {  // save results to model, write any necessary output to file, etc.
        this.model.audioTrackCleanup();
        this.fileController.saveString(this.iModel.getConfResultsAsString());
        this.fileController.closeFile();
        this.iModel.reset();
        this.iModel.notifySubscribers();
    }

    //////////////////////////////////// click handlers ////////////////////////////////////////////

    public void handleCalibClick(BackgroundNoiseType noise) {

        iModel.reset();
        iModel.setRampTest(new SineRampTest(noise));
        iModel.setReduceTest(new SineReduceTest(noise));
        iModel.setCalibrationTest(new SineCalibratonTest(noise));

        this.calibrationTest();
    }

    public void handleConfClick(BackgroundNoiseType noise) {
        if (! model.hasResults()) throw new IllegalStateException("No results stored in model");
        iModel.setConfidenceTest(new SingleSineConfidenceTest(model.getCalibrationTestResults(), noise));

        this.confidenceTest();
    }

    public void handleUpClick() {
        try {
            this.iModel.addClick(HearingTest.ANSWER_UP);
            this.iModel.setAnswer(HearingTest.ANSWER_UP);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void handleDownClick() {
        try {
            this.iModel.addClick(HearingTest.ANSWER_DOWN);
            this.iModel.setAnswer(HearingTest.ANSWER_DOWN);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void handleFlatClick() {
        try {
            this.iModel.addClick(HearingTest.ANSWER_FLAT);
            this.iModel.setAnswer(HearingTest.ANSWER_FLAT);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void handleHeardClick() {
        try {
            this.iModel.addClick(HearingTest.ANSWER_HEARD);
            this.iModel.setAnswer(HearingTest.ANSWER_HEARD);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////// accessor/mutator ////////////////////////////////////////////

    public void setModel(Model model) {
        this.model = model;
    }

    public void setiModel(HearingTestInteractionModel iModel) {
        this.iModel = iModel;
    }

    public void setView(HearingTestView view) {
        this.view = view;
    }

    public void setNoiseController(BackgroundNoiseController noiseController) {
        this.noiseController = noiseController;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setFileController(FileIOController fileController) {
        this.fileController = fileController;
    }
}
