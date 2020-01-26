package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;

import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.ConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.IntervalSineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.MelodySineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.SingleSineConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Confidence.SinglePianoConfidenceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.SineRampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.SineReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
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
     * Set up a new calibration suite to be run. The part where it actually starts running happens in
     * checkForHearingTestResume, probably called from MainActivity.modelChanged()
     *
     * @param noiseTypeID The identifier for the type of noise to be used in the test
     *                    (one of BackgroundNoiseType.TYPE_*)
     * @param noiseVol The volume from 0 to 100 of the noise in this test
     * @param toneTimbreID The identifier for the timbre of the tones to be played in this test
     *                     (one of Tone.TIMBRE_*)
     * @param testTypeID The identifier for the test suite to be run (one of this.TEST_SUITE_*)
     */
    public void calibrationTest(int noiseTypeID, int noiseVol, int toneTimbreID, int testTypeID)
            throws TestNotAvailableException {

        BackgroundNoiseType noiseType = new BackgroundNoiseType(noiseTypeID, noiseVol);

        this.model.configureAudio();
        this.iModel.setTestPaused(true);
        // set current file to current participant's calibration file
        this.fileController.setCurrentCalib(this.model.getCurrentParticipant());

        // Each test suite must:
        //  - create and place into the iModel all tests that will be run in this suite
        //  - call setup*Test() for the first test of the suite
        // The rest of the setup is generic and is done in the next code block
        switch (testTypeID) {
            case TEST_SUITE_FULL:
                if (toneTimbreID == Tone.TIMBRE_SINE) {
                    this.iModel.setRampTest(new SineRampTest(noiseType));
                    this.iModel.setReduceTest(new SineReduceTest(noiseType));
                    this.iModel.setConfidenceTest(new SingleSineConfidenceTest(noiseType));
                    this.setupRampTest();
                } else {
                    throw new TestNotAvailableException();
                }
                break;
            case TEST_SUITE_RAMP:
                // TODO
            case TEST_SUITE_RR:
                // TODO
                throw new TestNotAvailableException();
            default:
                throw new RuntimeException("Unknown testTypeID: " + testTypeID);
        }

        // start bg noise
        this.noiseController.playNoise(this.iModel.getCurrentNoise());
        // show the information dialog to the user, which will start the test once the user closes it
        this.view.showInformationDialog(this.iModel.getCurrentTest().getTestInfo());
    }

    /**
     * Perform any necessary steps to get ready for a ramp test.
     */
    private void setupRampTest() {
        // save the header line with test information for the first test
        this.fileController.saveTestHeader(this.iModel.getRampTest());
        this.iModel.setCurrentTest(this.iModel.getRampTest());
    }

    /**
     * Perform any necessary steps to finalize the RampTest and prepare the ReduceTest or CalibrationTest
     * Only call immediately after completing a RampTest
     */
    public void rampTestComplete() {
        // indicate that the test is complete in the participant's calibration file
        this.fileController.saveEndTest();
        // add to model.hearingTestResults without reduce results
        this.model.getCurrentParticipant().getResults().addResults(
                this.iModel.getRampTest().getResults().getRegularRampResults());
        // go to reduce test or calibration test, if necessary
        if (iModel.getReduceTest() != null) {
            this.setupReduceTest();
            this.view.showInformationDialog(this.iModel.getReduceTest().getTestInfo());
        } else if (iModel.getCalibrationTest() != null) {
            this.setupCalibrationTest();
            this.view.showInformationDialog(this.iModel.getCalibrationTest().getTestInfo());
        } else { // clean up and exit if not doing a full calibration
            testComplete();
        }
    }

    /**
     * Perform any necessary steps to get ready for a reduce test
     */
    private void setupReduceTest() {
        this.iModel.getReduceTest().setRampResults(this.iModel.getRampTest().getResults());
        this.iModel.getReduceTest().initialize();
        this.iModel.setTestThreadActive(false);
        this.iModel.notifySubscribers();
        this.iModel.setCurrentTest(this.iModel.getReduceTest());
        this.fileController.saveTestHeader(this.iModel.getReduceTest());
    }

    /**
     * Perform any necessary steps to finalize the ReduceTest and prepare the RampTest.
     * Only call immediately after completing a ReduceTest
     */
    public void reduceTestComplete() {
        // indicate that the test is complete
        this.fileController.saveEndTest();
        // Add ramp results to model list again, but with floor info this time
        this.iModel.getRampTest().getResults().setReduceResults(this.iModel.getReduceTest().getLowestVolumes());
        this.model.getCurrentParticipant().getResults().addResults(this.iModel.getRampTest().getResults());

        // set up CalibrationTest to run next, if necessary
        if (this.iModel.getCalibrationTest() != null) {
            this.setupCalibrationTest();
            this.view.showInformationDialog(this.iModel.getCalibrationTest().getTestInfo());
        } else {
            testComplete();
        }
    }

    /**
     * Perform any necessary steps to get ready for a calibration test
     */
    private void setupCalibrationTest() {
        this.iModel.getCalibrationTest().setRampResults(this.iModel.getRampTest().getResults());
        this.iModel.getCalibrationTest().setReduceResults(this.iModel.getReduceTest().getLowestVolumes());
        this.iModel.getCalibrationTest().initialize();
        this.iModel.setCurrentTest(this.iModel.getCalibrationTest());
        this.fileController.saveTestHeader(this.iModel.getCurrentTest());
        this.iModel.setTestThreadActive(false);
        this.iModel.notifySubscribers();
    }

    /**
     * Perform any final actions that need to be done before the calibration test is officially complete
     */
    public void calibrationTestComplete() {
        this.model.getCurrentParticipant().getResults().addResults(this.iModel.getCalibrationTest().getResults());
        this.fileController.saveEndTest();
        testComplete();
    }

    /**
     * Set up a new confidence test to be run. The part where it actually starts running happens in
     * checkForHearingTestResume, probably called from MainActivity.modelChanged()
     *
     * @param noiseTypeID The identifier for the type of noise (one of BackgroundNoiseType.NOISE_TYPE_*)
     * @param noiseVol The volume from 0 to 100 of the background noise
     * @param toneTimbreID The identifier for the timbre of the tones to be played in the confidence test (one of
     *                     Tone.TIMBRE_*)
     * @param toneTypeID The identifier for the type of tone (one of Tone.TYPE_*)
     * @param trialsPerTone The number of trials per individual freq-vol combination in the confidence test
     * @throws TestNotAvailableException If the given configuration is possible in principle but not yet implemented
     */
    public void confidenceTest(int noiseTypeID, int noiseVol, int toneTimbreID, int toneTypeID, int trialsPerTone)
            throws TestNotAvailableException {
        BackgroundNoiseType noiseType = new BackgroundNoiseType(noiseTypeID, noiseVol);
        ConfidenceTest confTest = null;

        switch (toneTimbreID) {
            case Tone.TIMBRE_SINE:
                if (toneTypeID == Tone.TYPE_SINGLE) {
                    confTest = new SingleSineConfidenceTest(noiseType);
                } else if (toneTypeID == Tone.TYPE_MELODY) {
                    confTest = new MelodySineConfidenceTest(noiseType);
                } else if (toneTypeID == Tone.TYPE_INTERVAL) {
                    confTest = new IntervalSineConfidenceTest(noiseType);
                } else {
                    throw new RuntimeException("Unknown toneTypeID: " + toneTypeID);
                }
                break;
            case Tone.TIMBRE_PIANO:
                if (toneTypeID == Tone.TYPE_SINGLE) {
                    confTest = new SinglePianoConfidenceTest(noiseType);
                } else if (toneTypeID == Tone.TYPE_MELODY) {
                    throw new TestNotAvailableException();
                } else if (toneTypeID == Tone.TYPE_INTERVAL) {
                    throw new TestNotAvailableException();
                } else {
                    throw new RuntimeException("Unknown toneTypeID: " + toneTypeID);
                }
                break;
            case Tone.TIMBRE_WAV:
                throw new TestNotAvailableException();
            default:
                throw new RuntimeException("Unknown toneTimbreID " + toneTimbreID);
        }

        this.model.configureAudio();
        this.iModel.setTestPaused(true);
        this.iModel.setConfidenceTest(confTest);
        this.iModel.setCurrentTest(confTest);
        this.fileController.setCurrentConf(model.getCurrentParticipant());
        this.view.showSampleDialog(confTest.sampleTones(), confTest.getTestInfo());
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
        try {
            this.fileController.setCurrentFile(null);
        } catch (FileNotFoundException e) {
            Log.e("HearingTestController", "Something very very very very strange has happened in testComplete()");
            e.printStackTrace();
        }
        this.iModel.reset();
        this.model.audioTrackCleanup();
        this.iModel.notifySubscribers();
    }

    //////////////////////////////////// click handlers ////////////////////////////////////////////

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

    ////////////////////////////////////////////// misc ///////////////////////////////////////////////

    public class TestNotAvailableException extends RuntimeException {
    }

}
