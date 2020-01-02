package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.util.Log;

import org.apache.commons.math3.analysis.function.Sin;

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
 * A class for performing HearingTests and handling UI events
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

    // test type identifiers. RR = ramp + reduce
    public static final int TEST_SUITE_FULL = 0;
    public static final int TEST_SUITE_RAMP = 1;
    public static final int TEST_SUITE_RR = 2;
    public static final int TEST_TYPE_CONF = 3;

    /**
     * Human-readable names of all CalibrationTest type options
     *
     * "Full" = a full 3-phase calibration
     * "Ramp" = ramp test only
     * "RR" = ramp and reduce only
     * */
    public static final String[] CALIB_TEST_OPTIONS = {"Sine Full", "Piano Full", "Sine Ramp", "Sine RR"};

    /**
     * Human-readable names of all ConfidenceTest type options
     */
    public static final String[] CONF_TEST_OPTIONS =
            {"Single Tone Sine", "Interval Sine", "Melody Sine", "Single Tone Piano",
             "Single Sine w/ Calibration Pitches"};

    ////////////////////////////////////////// control /////////////////////////////////////////////

    /**
     * Resume or start the current hearing test, if necessary
     */
    public void checkForHearingTestResume() {
        if (! iModel.testThreadActive() && iModel.testing() && ! iModel.testPaused())
            iModel.getCurrentTest().checkForHearingTestResume();
    }

    /**
     * Begin the calibration. iModel.rampTest must be fully configured before calling this method (see
     * handleCalibClick).
     * This method is only used to begin a new calibration test directly after getting user to input test information. 
     * To resume a test, use checkForHearingTestResume
     */
    public void calibrationTest() {
        this.model.configureAudio();
        this.iModel.setTestPaused(true);
        this.iModel.setCurrentTest(this.iModel.getRampTest());
        this.noiseController.playNoise(this.iModel.getCurrentNoise());
        this.fileController.setCurrentCalib(this.model.getCurrentParticipant());
        this.view.showInformationDialog(this.iModel.getCurrentTest().getTestInfo());
    }

    /**
     * Perform any necessary steps to finalize the RampTest and prepare the ReduceTest
     * Only call immediately after completing a RampTest
     */
    public void rampTestComplete() {
        this.iModel.setTestPaused(true);
        // add to model.hearingTestResults without reduce results
        this.model.getCurrentParticipant().getResults().addResults(
                this.iModel.getRampTest().getResults().getRegularRampResults());
        // go to ramp test if doing a full calibration
        if (iModel.getRampTest() != null) {
            this.view.showInformationDialog(this.iModel.getReduceTest().getTestInfo());
            this.iModel.getReduceTest().setRampResults(this.iModel.getRampTest().getResults());
            this.iModel.getReduceTest().initialize();
            this.iModel.setTestThreadActive(false);
            this.iModel.notifySubscribers();
            this.iModel.setCurrentTest(this.iModel.getReduceTest());
        } else { // clean up and exit if not doing a full calibration
            testComplete();
        }
    }

    /**
     * Perform any necessary steps to finalize the ReduceTest and prepare the RampTest.
     * Only call immediately after completing a ReduceTest
     */
    public void reduceTestComplete() {
        // Add ramp results to model list again, but with floor info this time
        this.iModel.getRampTest().getResults().setReduceResults(this.iModel.getReduceTest().getLowestVolumes());
        this.model.getCurrentParticipant().getResults().addResults(this.iModel.getRampTest().getResults());

        // set up CalibrationTest to run next, if doing a full test
        if (this.iModel.getCalibrationTest() != null) {
            this.iModel.getCalibrationTest().setRampResults(this.iModel.getRampTest().getResults());
            this.iModel.getCalibrationTest().setReduceResults(this.iModel.getReduceTest().getLowestVolumes());
            this.iModel.getCalibrationTest().initialize();
            this.iModel.setCurrentTest(this.iModel.getCalibrationTest());
            this.iModel.setTestThreadActive(false);
            this.iModel.notifySubscribers();
        } else {
            testComplete();
        }
    }

    /**
     * Perform any final actions that need to be done before the calibration test is officially complete
     */
    public void calibrationTestComplete() {
        this.model.getCurrentParticipant().getResults().addResults(this.iModel.getCalibrationTest().getResults());
        testComplete();
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
        this.fileController.setCurrentConf(this.model.getCurrentParticipant());
        this.view.showSampleDialog( this.iModel.getConfidenceTest().sampleTones(),
                                    this.iModel.getCurrentTest().getTestInfo());
    }

    /**
     * Perform any final actions that need to be done before the confidence test is officially complete
     */
    public void confidenceTestComplete() {
        this.model.audioTrackCleanup();
        this.fileController.saveString(
                this.model.getCurrentParticipant().getResults().compareToConfidenceTest(
                        this.iModel.getConfidenceTest().getConfResults()));
        this.iModel.reset();
        this.iModel.notifySubscribers();
    }

    /**
     * To be called once a test or suite of tests is completed and the model etc is to be reset
     */
    private void testComplete() {
        this.iModel.reset();
        this.model.audioTrackCleanup();
        this.iModel.notifySubscribers();
    }

    //////////////////////////////////// click handlers ////////////////////////////////////////////

    /**
     * Set up the iModel for a new 3-phase calibration of the appropriate type with the 
     * appropriate background noise, then begin the test
     *
     * @param noise The background noise to be played during this test
     * @param testTypeID The type of test to begin: given as an index of CALIB_TEST_OPTIONS 
     *        (eg. to start the test denoted by CALIB_TEST_OPTIONS[0], pass 0)
     */
    public void handleCalibClick(BackgroundNoiseType noise, int testTypeID) {

        iModel.reset();

        switch (testTypeID) {
            case 0:     // single sine
                iModel.setRampTest(new SineRampTest(noise));
                iModel.setReduceTest(new SineReduceTest(noise));
                iModel.setCalibrationTest(new SineCalibratonTest(noise));
                break;
            case 1:     // single piano
                iModel.setRampTest(new PianoRampTest(noise));
                iModel.setReduceTest(new PianoReduceTest(noise));
                iModel.setCalibrationTest(new PianoCalibrationTest(noise));
                break;
            case 2:     // sine ramp
                iModel.setRampTest(new SineRampTest(noise));
                // other 2 tests are null
                break;
            case 3:     // sine ramp/reduce
                iModel.setRampTest(new SineRampTest(noise));
                iModel.setReduceTest(new SineReduceTest(noise));
                // calibration test is null
                break;
            default:
                throw new IllegalArgumentException("Invalid test type ID given: " + testTypeID);
        }

        this.calibrationTest();
    }

    /**
     * Set up the iModel for a new confidence test of the appropriate type with the appropriate background noise,
     * then begin the test
     *
     * @param noise The background noise to be played during this test
     * @param testTypeID The type of test to begin: given as an index of CONF_TEST_OPTIONS
     *        (eg. to start the test denoted by CALIB_TEST_OPTIONS[0], pass 0)
     */
    public void handleConfClick(BackgroundNoiseType noise, int testTypeID) {
        if (! model.hasResults()) throw new IllegalStateException("No results stored in model");

        ConfidenceTest newTest;

        switch (testTypeID) {
            case 0:     // single sine
                newTest = new SingleSineConfidenceTest(noise);
                break;
            case 1:     // interval sine
                newTest = new IntervalSineConfidenceTest(noise);
                break;
            case 2:     // melody sine
                newTest = new MelodySineConfidenceTest(noise);
                break;
            case 3:     // single piano
                newTest = new PianoConfidenceTest(noise);
                break;
            case 4:     // single sine, test default calibration frequencies
                newTest = new SingleSineCalibFreqConfidenceTest(noise);
                break;
            default:
                throw new IllegalArgumentException("Invalid test type ID given: " + testTypeID);
        }

        newTest.initialize();
        iModel.setConfidenceTest(newTest);
        this.confidenceTest();
    }

    /**
     * Register a UI event with the answer "up"
     *
     * @param fromTouchInput Was the UI event a touchscreen button press?
     */
    public void handleUpClick(boolean fromTouchInput) {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_UP, fromTouchInput);
                this.iModel.setAnswer(HearingTest.ANSWER_UP);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    /**
     * Register a UI event with the answer "down"
     *
     * @param fromTouchInput Was the UI event a touchscreen button press?
     */
    public void handleDownClick(boolean fromTouchInput) {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_DOWN, fromTouchInput);
                this.iModel.setAnswer(HearingTest.ANSWER_DOWN);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    /**
     * Register a UI event with the answer "flat"
     */
    public void handleFlatClick() {
        if (iModel.testing())
            try {
                this.iModel.addClick(HearingTest.ANSWER_FLAT, true);
                this.iModel.setAnswer(HearingTest.ANSWER_FLAT);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
    }

    /**
     * Register a UI event with the answer "heard"
     *
     * @param fromTouchInput Was the UI event a touchscreen button press?
     */
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
