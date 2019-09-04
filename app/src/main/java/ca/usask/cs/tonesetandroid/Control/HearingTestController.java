package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.util.Log;

import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.PianoCalibrationTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Calibration.SineCalibratonTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.ConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.IntervalSineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.MelodySineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.SingleSineCalibFreqConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.SingleSineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.PianoRampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.PianoReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.SineRampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.SineReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.PianoConfidenceTest;
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

    public static final String[] CALIB_TEST_OPTIONS = {"Single Tone Sine", "Single Tone Piano"};

    public static final String[] CONF_TEST_OPTIONS =
            {"Single Tone Sine", "Interval Sine", "Melody Sine", "Single Tone Piano",
                    "Single Sine w/ Calibration Pitches"};

    ////////////////////////////////////////// control /////////////////////////////////////////////

    /**
     * Resume or setStartTime the current hearing test, if necessary
     */
    public void checkForHearingTestResume() {
        if (! iModel.testThreadActive() && iModel.testing() && ! iModel.testPaused())
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
     * Prepare for a reduce test. Must only be called immediately after the ramp test is completed
     */
    public void rampTestComplete() {
        iModel.setTestPaused(true);
        view.showInformationDialog(iModel.getReduceTest().getTestInfo());
        iModel.getReduceTest().setRampResults(iModel.getRampTest().getResults());
        iModel.getReduceTest().initialize();
        iModel.setTestThreadActive(false);
        iModel.notifySubscribers();
        iModel.setCurrentTest(iModel.getReduceTest());

    }

    public void reduceTestComplete() {
        // add these results to RampTest
        iModel.getRampTest().getResults().setReduceResults(iModel.getReduceTest().getLowestVolumes());

        // set up CalibrationTest to run next
        iModel.getCalibrationTest().setRampResults(iModel.getRampTest().getResults());
        iModel.getCalibrationTest().setReduceResults(iModel.getReduceTest().getLowestVolumes());
        iModel.getCalibrationTest().initialize();
        iModel.setCurrentTest(iModel.getCalibrationTest());
        iModel.setTestThreadActive(false);
        iModel.notifySubscribers();
    }

    /**
     * Perform any final actions that need to be done before the calibration test is officially "complete"
     */
    public void calibrationTestComplete() {
        this.model.setCalibrationTestResults(this.iModel.getCalibrationResults());
        this.model.setRampResults(this.iModel.getRampTest().getResults());
        this.iModel.reset();
        this.model.printResultsToConsole();
        this.model.audioTrackCleanup();
        this.fileController.closeFile();
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
        this.fileController.saveString(String.format("Calibration Results:%n%s%nRamp Results:%n%s%n",
                                    this.model.calibrationTestResults.toString(), this.model.rampResults.toString()));

        this.view.showSampleDialog( this.iModel.getConfidenceTest().sampleTones(),
                                    this.iModel.getCurrentTest().getTestInfo());
    }

    /**
     * Perform any final actions that need to be done before the confidence test is officially "complete"
     */
    public void confidenceTestComplete() {
        this.model.audioTrackCleanup();
        this.fileController.saveString(this.iModel.getConfResultsAsString());
        this.fileController.closeFile();
        this.iModel.reset();
        this.iModel.notifySubscribers();
    }

    //////////////////////////////////// click handlers ////////////////////////////////////////////

    /**
     * Set up the iModel for a new calibration test of the appropriate type with the appropriate background noise,
     * then setStartTime the test
     *
     * @param noise The background noise to be played during this test
     * @param testTypeID The type of test to begin: given as an index of CALIB_TEST_OPTIONS
     */
    public void handleCalibClick(BackgroundNoiseType noise, int testTypeID) {

        iModel.reset();

        switch (testTypeID) {
            case 0:     // single sine
                iModel.setRampTest(new SineRampTest(noise));
                iModel.setReduceTest(new SineReduceTest(noise));
                iModel.setCalibrationTest(new SineCalibratonTest(noise));
                break;
            case 1:
                iModel.setRampTest(new PianoRampTest(noise));
                iModel.setReduceTest(new PianoReduceTest(noise));
                iModel.setCalibrationTest(new PianoCalibrationTest(noise));
                break;
            default:
                throw new IllegalArgumentException("Invalid test type ID given: " + testTypeID);
        }

        this.calibrationTest();
    }

    /**
     * Set up the iModel for a new confidence test of the appropriate type with the appropriate background noise,
     * then setStartTime the test
     *
     * @param noise The background noise to be played during this test
     * @param testTypeID The type of test to begin: given as an index of CONF_TEST_OPTIONS
     */
    public void handleConfClick(BackgroundNoiseType noise, int testTypeID) {
        if (! model.hasResults()) throw new IllegalStateException("No results stored in model");

        ConfidenceTest newTest;

        switch (testTypeID) {
            case 0:     // single sine
                newTest = new SingleSineConfidenceTest(model.getCalibrationTestResults(), noise);
                break;
            case 1:     // interval sine
                newTest = new IntervalSineConfidenceTest(model.getCalibrationTestResults(), noise);
                break;
            case 2:     // melody sine
                newTest = new MelodySineConfidenceTest(model.getCalibrationTestResults(), noise);
                break;
            case 3:     // single piano
                newTest = new PianoConfidenceTest(model.calibrationTestResults, noise);
                break;
            case 4:     // single sine, test default calibration frequencies
                newTest = new SingleSineCalibFreqConfidenceTest(model.calibrationTestResults, noise);
                break;
            default:
                throw new IllegalArgumentException("Invalid test type ID given: " + testTypeID);
        }

        newTest.initialize();
        iModel.setConfidenceTest(newTest);
        this.confidenceTest();
    }

    public void handleUpClick(boolean fromTouchInput) {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_UP, fromTouchInput);
                this.iModel.setAnswer(HearingTest.ANSWER_UP);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    public void handleDownClick(boolean fromTouchInput) {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_DOWN, fromTouchInput);
                this.iModel.setAnswer(HearingTest.ANSWER_DOWN);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    public void handleFlatClick() {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_FLAT, true);
                this.iModel.setAnswer(HearingTest.ANSWER_FLAT);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    public void handleHeardClick(boolean fromTouchInput) {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_HEARD, fromTouchInput);
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
