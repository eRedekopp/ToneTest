package ca.usask.cs.tonesetandroid;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.io.InputStream;

/**
 * A class for performing PureTone and RampUp tests, and handling clicks on the main menu
 *
 * @author redekopp, alexscott
 */
public class HearingTestController {

    /*
    * Note: Tests should only be started with the hearingTest or confidenceTest methods, and resumed from pause with
    * the checkForHearingTestResume method
    */

    Model model;
    HearingTestInteractionModel iModel;
    HearingTestView view;
    BackgroundNoiseController noiseController;

    Context context;

    // The length of each test tone for initial calibration
    private static final int TONE_DURATION_MS = 1500;

    private static final String rampInfo =
            "In this phase of the test, tones will play quietly and slowly get louder. Please press the \"Heard " +
            "Tone\" button as soon as the tone becomes audible";

    private static final String mainInfo =
            "In this phase of the test, tones of various frequencies and volumes will be played at random times. " +
            "Please press the \"Heard Tone\" button each time that you hear a tone";

    private static final String earconInfo =
            "In this test, notification sounds will be played at various volumes at random times. Please press the " +
            "\"Up\" button if the notification moved upward, \"Down\" if the notification moved downward, and " +
            "\"Flat\" if the pitch did not change";

    /**
     * Checks if a test phase is supposed to be started or resumed, then starts a test on a new thread if it is
     */
    public void checkForHearingTestResume() {
        int testPhase = model.getTestPhase();
        if (testPhase == Model.TEST_PHASE_NULL || model.testPaused() || model.testThreadActive) return;
        if (testPhase == Model.TEST_PHASE_RAMP) this.rampUpTest();
        if (testPhase == Model.TEST_PHASE_REDUCE) this.reducePhase();
        if (testPhase == Model.TEST_PHASE_MAIN) this.mainTest();
        if (testPhase == Model.TEST_PHASE_CONF) this.mainConfTest();
    }

    /**
     * Play a sine wave through the Model at the given frequency and volume for the given amount of time. Returns
     * immediately if iModel.heard is true or becomes true from another thread.
     *
     * @param freq The frequency of the sine wave to be played
     * @param vol The volume of the sine wave to be played
     * @param duration_ms The duration of the sine wave in milliseconds
     */
    private void playSine(float freq, double vol, int duration_ms) {
        try {
            model.enforceMaxVolume();
            model.startAudio();
            for (int i = 0; i < duration_ms * (float) 44100 / 1000; i++) {
                if (iModel.heard) break;

                float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
                double angle = 2 * i / (period) * Math.PI;
                short a = (short) (Math.sin(angle) * vol);
                model.buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
                model.buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
                model.lineOut.write(model.buf, 0, 2);
            }

        } finally {
            model.stopAudio();
        }
    }

    private void playEarcon(Earcon earcon) {

        model.startAudio();
        try {
            InputStream rawPCM = this.context.getResources().openRawResource(earcon.audioResourceID);

            try {
                while (rawPCM.available() > 0) {
                    rawPCM.read(model.buf, 0, 2);       // read data from stream

                    byte b = model.buf[0];              // convert to big-endian
                    model.buf[0] = model.buf[1];
                    model.buf[1] = b;

                    short sample = (short) (model.buf[0] << 8 | model.buf[1] & 0xFF);  // convert to short
                    double amplitude = (double) sample / (double) Short.MIN_VALUE;
                    sample = (short) (amplitude * earcon.volume);                   // convert to same vol scale as
                                                                                    // sines
                    model.lineOut.write(new short[]{sample}, 0, 1);                 // write sample to line out

                }
            } catch (IOException e) {
                Log.e("playEarcon", "Error playing earcon file");
                e.printStackTrace();
            }

        } finally {
            model.stopAudio();
        }
    }

//    /**
//     * For testing: Play a an earcon and a sine wave of the same volume
//     */
//    public void testVolume() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("testVolume", "Playing sine");
//                model.startAudio();
//                playSine(1046, 200, 800);
//                model.stopAudio();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                Log.d("testVolume", "Playing earcon");
//                model.startAudio();
//                playEarcon(new Earcon(1046, R.raw.ec1046hzmaritriadupshort, 200, Earcon.DIRECTION_UP));
//                model.stopAudio();
//            }
//        }).start();
//    }

    /////////////////////////////////////// Methods for hearing test //////////////////////////////////////////////////

    /**
     * Begin a hearing test. Model must have a background noise set before calling this method
     *
     * @author redekopp
     */
    public void hearingTest() {

        // Algorithm:
        //      1 Get estimates for volumes at which the listener will hear the tone 100% of the time for each frequency
        //          - Use RampTest because it tends to overshoot anyway
        //      2 Get estimates for volumes at which the listener will hear the tone 0% of the time for each frequency
        //          - Slowly reduce volumes from RampTest levels until listener can't hear tone
        //      3 Select a set of volumes between the two estimates to test for each frequency
        //      4 Test all frequency-volume combinations selected in step 3 and store the results
        //          - Referred to elsewhere as "main test"

        // To allow the user to pause the test, the hearing test is broken up into 3 phases. The appropriate
        // phase is selected in checkForHearingTestResume

        model.testThreadActive = true;

        new Thread(new Runnable() {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                try {
                    model.configureAudio();

                    // show information for ramp segment of test
                    model.setTestPaused(true);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            model.setTestPhase(Model.TEST_PHASE_RAMP);
                            noiseController.playNoise(model.hearingTestResults.getBackgroundNoise());
                            view.showInformationDialog(rampInfo);
                        }
                    });
                } finally { model.testThreadActive = false; }
            }
        }).start();
    }

    /**
     * Perform the reduction phase of the calibration test: get bottom volume estimates by reducing volumes and asking
     * user whether they can hear them until no tones are audible.
     */
    @SuppressWarnings("unchecked")
    private void reducePhase() {
        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (model.continueTest() && ! model.testPaused()) {
                        model.reduceCurrentVolumes();
                        testCurrentVolumes();
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            model.bottomVolEstimates = (ArrayList<FreqVolPair>) model.currentVolumes.clone();
                            model.configureTestPairs();
                            model.setTestPhase(Model.TEST_PHASE_MAIN);
                        }
                    });
                    // after getting bottom estimates, prepare for next phase of test
                } finally {
                    model.testThreadActive = false;
                    model.notifySubscribers();  // begin next test phase
                }
            }
        }).start();
    }

    /**
     * Test frequencies at model.currentVolumes for reduction phase and update model accordingly
     */
    @SuppressWarnings("ConstantConditions")
    private void testCurrentVolumes() {
        Collections.shuffle(model.currentVolumes);  // test in random order each time
        for (FreqVolPair fvp : model.currentVolumes) {

            if (model.testPaused()) return; // check if test paused before each tone

            Log.i("reducePhase", "Testing " + fvp.toString());

            // only test frequencies whose bottom ends hasn't already been estimated
            if (model.timesNotHeardPerFreq.get(fvp.freq) >= Model.TIMES_NOT_HEARD_BEFORE_STOP) continue;

            // play the sine, update the map if not heard
            iModel.notHeard();
            this.playSine(fvp.freq, fvp.vol, TONE_DURATION_MS);
            if (! iModel.heard)
                mapReplace(model.timesNotHeardPerFreq, fvp.freq,
                        model.timesNotHeardPerFreq.get(fvp.freq) + 1);
            Log.i("reducePhase", iModel.heard ? "Tone Heard" : "Tone not heard");   // print message indicating whether
            // tone was heard

            try {
                Thread.sleep((long) (Math.random() * 2000 + 1000));
            } catch (InterruptedException e) { return; }
        }
    }

    /**
     * Perform a ramp up test, and set the results as the top estimates in the model
     *
     * For each frequency, start quiet then get louder until user hears tone, then go to a fraction of that volume
     * and ramp up again, but slower.
     *
     * @author alexscott
     */
    @SuppressWarnings("unchecked")
    public void rampUpTest() {

        model.testThreadActive = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    model.configureAudio();

                    double heardVol;

                    // test all frequencies in Model.FREQUENCIES which haven't already been tested
                    ArrayList<Float> freqsToTest = new ArrayList<>();
                    for (float freq : Model.FREQUENCIES) freqsToTest.add(freq);
                    for (FreqVolPair fvp : model.topVolEstimates) freqsToTest.remove(fvp.freq);

                    //Loop through all of the frequencies for the hearing test
                    for (Float freq : freqsToTest) {
                        if (model.testPaused()) return; // check if paused before each frequency
                        Log.i("rampUpTest", "Testing frequency " + freq);

                        double rateOfRamp = 1.05;
                        rampUp(rateOfRamp, freq, 0.1);

                        heardVol = model.volume; // record the volume when user paused

                        if (model.testPaused()) return; // check if user paused test before playing next tone

                        try {
                            Thread.sleep(1000); // sleep 1 second
                        } catch (InterruptedException e) {
                            break;
                        }

                        rateOfRamp = 1.01;
                        // redo the ramp up test, this time starting at 1/10th the volume previously required to hear the
                        // tone and ramping up at a slower rate
                        rampUp(rateOfRamp, freq, heardVol / 10.0);

                        if (model.testPaused()) return; // check if user paused test before adding result

                        FreqVolPair results = new FreqVolPair(freq, model.volume);
                        model.topVolEstimates.add(results); //add the frequency volume pair to list of top estimates

                        try {
                            Thread.sleep(1000);  // sleep 1 second
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    model.setTestPaused(true);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // after testing all frequencies, prepare for next phase of test
                            // use upper estimates as a starting off point for lowering volumes
                            model.currentVolumes = (ArrayList) model.topVolEstimates.clone();
                            model.setTestPhase(Model.TEST_PHASE_REDUCE);
                            // show information for next segment of test
                            view.showInformationDialog(mainInfo);
                        }
                    });
                } finally { model.testThreadActive = false; }
            }
        }).start();
    }

    /**
     * A method to play a tone that ramps up at the rate specified by the input
     * parameter This method was made to reduce redundancy in the rampUpTest
     * method
     *
     * @param rateOfRamp: the rate that the volume is ramped up at
     * @param freq: the frequency of the tone
     * @param startingVol: the initial volume of the tone
     *
     * @author alexscott
     */
    public void rampUp(double rateOfRamp, float freq, double startingVol) {

        model.enforceMaxVolume(); // force max volume always
        model.startAudio();

        for (model.volume = startingVol; model.volume < 32767; model.volume *= rateOfRamp) {
            if (! model.audioPlaying() || model.testPaused()) return;

            if (iModel.heard) {
                iModel.notHeard();//reset the iModel for the next ramp
                break;
            }
            model.duration_ms = 50; //play the tone at this volume for 0.05s
            for (int i = 0; i < model.duration_ms * (float) 44100 / 1000; i++) { //1000 ms in 1 second
                float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
                double angle = 2 * i / (period) * Math.PI;
                short a = (short) (Math.sin(angle) * model.volume);
                model.buf[0] = (byte) (a & 0xFF); //write 8bits ________WWWWWWWW out of 16
                model.buf[1] = (byte) (a >> 8); //write 8bits WWWWWWWW________ out of 16
                model.lineOut.write(model.buf, 0, 2);
            }
        }
        model.stopAudio();
    }

    /**
     * Perform the "main" hearing test: test all frequencies in testIntervals and save results in model
     */
    private void mainTest() {
        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!model.currentVolumes.isEmpty()) {
                        if (model.testPaused()) return;

                        FreqVolPair trial = model.currentVolumes.get(0);
                        model.currentVolumes.remove(0);
                        model.startAudio();
                        Log.i("mainTest", "Testing " + trial.toString());
                        iModel.notHeard();
                        playSine(trial.freq, trial.vol, TONE_DURATION_MS);
                        model.hearingTestResults.addResult(trial.freq, trial.vol, iModel.heard);
                        model.stopAudio();
                        Log.i("mainTest", "Tone " + (iModel.heard ? "heard" : "not heard"));

                        try {               // sleep for for random length 1-3 seconds
                            Thread.sleep((long) (Math.random() * 2000 + 1000));
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            model.setTestPhase(Model.TEST_PHASE_NULL);
                            model.audioTrackCleanup();
                            model.printResultsToConsole();
                        }
                    });

                } finally { model.testThreadActive = false; }
            }
        }).start();

    }

    /////////////////////////////////// methods for confidence test ///////////////////////////////////////////////////

    /**
     * Begin a confidence test
     */
    public void confidenceTest() {
        // todo add dialog while calculating floor/ceiling volumes
        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    // configure model for test
                    model.configureAudio();
                    model.configureConfidenceTestEarcons();
                    model.setConfResultsSaved(false);

                    // show info dialog
                    model.setTestPaused(true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            model.setTestPhase(Model.TEST_PHASE_CONF);
                            noiseController.playNoise(model.confidenceTestResults.getNoiseType());
                            view.showInformationDialog(earconInfo);
                        }
                    });
                } finally { model.testThreadActive = false; }
            }
        }).start();
    }

    /**
     * The actual legs of the confidence test - test all remaining confidence trials stored in model and store
     * results in confidenceTestResults
     */
    private void mainConfTest() {

        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // perform trials
                    while (! model.confidenceTestEarcons.isEmpty()) {
                        if (model.testPaused()) return;
                        Earcon trial = model.confidenceTestEarcons.get(0);
                        model.confidenceTestEarcons.remove(0);
                        model.startAudio();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {   // set iModel to notHeard on main thread
                                iModel.resetAnswer();
                            }
                        });
                        Log.i("confTest", "Testing Earcon: " + trial.toString());
                        playEarcon(trial);
                        model.stopAudio();

                        try {  // User gets 1.5 seconds to enter response
                            Thread.sleep((long) (Math.random() * 2000 + 2000));
                        } catch (InterruptedException e) { return; }

                        model.confidenceTestResults.addResult(trial, trial.direction == iModel.getAnswer());
                        Log.i("confTest", trial.direction == iModel.getAnswer() ? "Answered Correctly"
                                                                                : "Answered Incorrectly");
                        try {  // sleep from 0 to 2 seconds
                            Thread.sleep((long) (Math.random() * 2000));
                        } catch (InterruptedException e) { return; }
                    }

                    // finish / cleanup
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() { // run on main thread
                            model.audioTrackCleanup();
                            model.setTestPhase(Model.TEST_PHASE_NULL);
                        }
                    });
                } finally {
                    model.testThreadActive = false;
                }
            }
        }).start();
    }

    ///////////////////////////////////// methods for auto test ///////////////////////////////////////////////////////

    /**
     * Populate model.hearingTestResults with results automatically through the microphone
     *
     * @author redekopp
     */
    public void autoTest() {

        // todo finish this later
    }

    //////////////////////////////////// click handlers + miscellaneous ////////////////////////////////////////////

    public void handleCalibClick() {
        if (this.model.hearingTestResults.getBackgroundNoise() == null)
            throw new IllegalStateException("Background noise must be configured before beginning calibration");
        this.hearingTest();
    }

    public void handleConfClick() {
        if (this.model.confidenceTestResults.getNoiseType() == null)
            throw new IllegalStateException("Background noise must be configured before beginning confidence test");
        this.confidenceTest();
    }

    public void handleUpClick() {
        this.iModel.setAnswer(Earcon.DIRECTION_UP);
    }

    public void handleDownClick() {
        this.iModel.setAnswer(Earcon.DIRECTION_DOWN);
    }

    public void handleFlatClick() {
        this.iModel.setAnswer(Earcon.DIRECTION_NONE);
    }

    public void handleHeardClick() {
        this.iModel.toneHeard();
    }

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

    /**
     * Return true if the list contains an element that is equal or almost equal to the given value, +/- the error
     * percentage
     *
     * @param list A list of Doubles
     * @param value The value for which to search
     * @param error The maximum difference between the value and a "hit" result in the list (ratio; 0 < error < 1)
     * @return True if the list contains an appropriate value, else false
     */
    public static boolean containsWithinError(List<Double> list, double value, float error) {
        for (Double d : list) if (d * (1 + error) > value && d * (1 - error) < value) return true;
        return false;
    }

    /**
     * How isn't there a built-in method for this??
     *
     * @param arr An array of floats
     * @return The mean of the array
     */
    private float mean(float[] arr) {
        float sum = 0;
        for (float f : arr) sum += f;
        return sum / (float) arr.length;
    }

    /**
     * Replace the value associated with the given key in the map with the given new value, or just associate the key
     * with the value if not already present.
     *
     * @param map A HashMap
     * @param key A valid key for that hashmap (not necessarily present in map)
     * @param newValue The new value with which to associate the key
     */
    public void mapReplace(HashMap<Float, Integer> map, Float key, Integer newValue) {
        map.remove(key);
        map.put(key, newValue);
    }
}
