package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;

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
        model.configureAudio();
        iModel.setTestPaused(true);
        iModel.setCurrentTest(iModel.getRampTest());
        noiseController.playNoise(iModel.getRampTest().getBackgroundNoiseType());
        view.showInformationDialog(iModel.getRampTest().getTestInfo());
    }

    public void calibrationTestComplete() {  // save results to model, write any necessary output to file, etc.
        // todo
    }

    /**
     * Begin a confidence test. Calibration results must be saved and iModel.confidenceTest must be configured before
     * calling this method. This method is only used to begin a new confidence test directly after getting user to
     * input test information. To resume a test, use checkForHearingTestResume
     */
    public void confidenceTest() {

    }

    public void confidenceTestComplete() {  // save results to model, write any necessary output to file, etc.

    }

    //////////////////////////////////// click handlers ////////////////////////////////////////////

    public void handleCalibClick() {
        //todo
    }

    public void handleConfClick() {
        //todo
    }

    public void handleUpClick() {
        //todo
    }

    public void handleDownClick() {
        //todo
    }

    public void handleFlatClick() {
        //todo
    }

    public void handleHeardClick() {
        //todo
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
}
