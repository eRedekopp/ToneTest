package ca.usask.cs.tonesetandroid;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    // The length of each test tone
    private static final int TONE_DURATION_MS = 1500;

    private static final String rampInfo =
            "In this test, tones will play quietly and slowly get louder. Please press the \"Heard " +
            "Tone\" button as soon as the tone becomes audible";

    private static final String mainInfo =
            "In this test, tones of various frequencies and volumes will be played at random times. Please press the " +
            "\"Heard Tone\" button each time that you hear a tone";

    private static final String intervalInfo =
            "In this test, two tones will be played in sequence at various frequencies and volumes at random times. " +
            "Please press the \"Up\" button if the second tone was higher than the first tone, press the \"Down\" " +
            "button if the second tone was lower than the first tone, or do nothing if you couldn't tell";

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
        model.stopAudio();
    }

    /**
     * Play two sine waves consecutively
     *
     * @param freq1 The frequency of the first sine
     * @param freq2 The frequency of the second sine
     * @param vol The volume of the sine waves
     * @param duration_ms The duration of each sine in milliseconds
     */
    private void playInterval(float freq1, float freq2, double vol, int duration_ms) {
        model.enforceMaxVolume();
        model.startAudio();
        for (int i = 0; i < duration_ms * (float) 44100 / 1000; i++) {
            float period = (float) Model.OUTPUT_SAMPLE_RATE / freq1;
            double angle = 2 * i / (period) * Math.PI;
            short a = (short) (Math.sin(angle) * vol);
            model.buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
            model.buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
            model.lineOut.write(model.buf, 0, 2);
        }
        for (int i = 0; i < duration_ms * (float) 44100 / 1000; i++) {
            float period = (float) Model.OUTPUT_SAMPLE_RATE / freq2;
            double angle = 2 * i / (period) * Math.PI;
            short a = (short) (Math.sin(angle) * vol);
            model.buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
            model.buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
            model.lineOut.write(model.buf, 0, 2);
        }
    }

    /////////////////////////////////////// Methods for hearing test //////////////////////////////////////////////////

    /**
     * Begin a hearing test
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
                    model.reset();
                    model.configureAudio();

                    // show information for ramp segment of test
                    model.setTestPaused(true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            model.setTestPhase(Model.TEST_PHASE_RAMP);
                            view.showInformationDialog(rampInfo);
                        }
                    });
                } finally { model.testThreadActive = false; }
            }
        }).start();
    }

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
                    model.setTestPaused(true);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            model.bottomVolEstimates = (ArrayList<FreqVolPair>) model.currentVolumes.clone();
                            model.configureTestIntervals();
                            model.setTestPhase(Model.TEST_PHASE_MAIN);
                            // show information for next segment of test
                            view.showInformationDialog(intervalInfo);
                        }
                    });
                    // after getting bottom estimates, prepare for next phase of test
                } finally { model.testThreadActive = false; }
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
            if (model.timesNotHeardPerFreq.get(fvp.getFreq()) >= Model.TIMES_NOT_HEARD_BEFORE_STOP) continue;

            // play the sine, update the map if not heard
            iModel.notHeard();
            this.playSine(fvp.getFreq(), fvp.getVol(), TONE_DURATION_MS);
            if (! iModel.heard)
                mapReplace(model.timesNotHeardPerFreq, fvp.getFreq(),
                        model.timesNotHeardPerFreq.get(fvp.getFreq()) + 1);
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
                    for (FreqVolPair fvp : model.topVolEstimates) freqsToTest.remove(fvp.getFreq());

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
                    while (!model.testIntervals.isEmpty()) {
                        if (model.testPaused()) return;
                        Log.d("mainTest", model.testIntervals.toString());
                        Interval trial = model.testIntervals.get(0);
                        model.testIntervals.remove(0);
                        model.startAudio();
                        Log.i("mainTest", "Testing " + trial.toString());
                        iModel.resetAnswer();
                        playInterval(trial.freq1, trial.freq2, trial.vol, TONE_DURATION_MS);
                        boolean correct = (iModel.getAnswer() > 0 && trial.isUpward)
                                          || (iModel.getAnswer() < 0 && ! trial.isUpward);
                        model.hearingTestResults.addResult(trial, correct);
                        Log.i("mainTest", correct ? "Answered correctly" : "Answered incorrectly"); // log answer

                        model.stopAudio();
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
     * Perform a full confidence test
     */
    public void confidenceTest() {
        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Handler mainHandler = new Handler(Looper.getMainLooper());

                    // configure model for test
                    model.configureAudio();
                    model.configureConfidenceTestPairs();


                    // show info dialog
                    model.setTestPaused(true);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            model.setTestPhase(Model.TEST_PHASE_CONF);
                            view.showInformationDialog(mainInfo);
                        }
                    });
                } finally { model.testThreadActive = false; }
            }
        }).start();
    }

    private void mainConfTest() {
        model.testThreadActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // perform trials
                    while (! model.confidenceTestIntervals.isEmpty()) {
                        if (model.testPaused()) return;
                        Interval trial = model.confidenceTestIntervals.get(0);
                        model.confidenceTestIntervals.remove(0);
                        model.startAudio();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {   // set iModel to notHeard on main thread
                                iModel.notHeard();
                            }
                        });
                        Log.i("confTest", "Testing interval: " + trial.toString());
                        playInterval(trial.freq1, trial.freq2, trial.vol, TONE_DURATION_MS);
                        model.stopAudio();
                        model.confidenceTestResults.addResult(trial, iModel.heard);
                        try {  // sleep from 1 to 3 seconds
                            Thread.sleep((long) (Math.random() * 2000 + 1000));
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
     * Applies a Hann Window to the data set to improve the overall accuracy
     * This function is slightly less general than a typical Hann window function
     * Typically, you also want to know the starting index of the data to be windowed
     * In this case, the index will always begin at 0
     *
     * @param data The data that will be windowed
     * @return The windowed data set
     *
     * @author alexscott
     */
    private static float[] applyHannWindow(float[] data) {
        int length = data.length;
        for (int i = 0; i < length; i++) {
            data[i] = (float) (data[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / length)));
        }
        return data;
    }

    /**
     * Populate model.hearingTestResults with results automatically through the microphone
     *
     * @author redekopp
     */
    public void autoTest() {
        FreqVolPair[] periodogram = this.getPeriodogramFromLineIn(2048);

        // todo finish this later
    }

    /**
     * Get a sample of audio from the line in, then perform an FFT and get a periodogram from it
     *
     * @param sampleSize The number of audio samples to use for the calculations (must be a power of 2)
     * @return FreqVolPairs where each frequency is the central frequency of a bin and the volume is the volume of that
     *         frequency bin
     */
    public FreqVolPair[] getPeriodogramFromLineIn(int sampleSize) {
        // todo : take multiple samples and average them?

        int freqBinWidth = Model.INPUT_SAMPLE_RATE / sampleSize;

        // Object for performing FFTs: handle real inputs of size sampleSize
        NoiseOptimized noise = Noise.real().optimized().init(sampleSize, true);

        // apply Hann window to reduce noise
        float[] rawMicData = model.getAudioSample(sampleSize);
        float[] fftInput = applyHannWindow(rawMicData);

        // perform FFT
        float[] fftResult = noise.fft(fftInput); // noise.fft() returns from DC bin to Nyquist bin, no slicing req'd

        // convert to power spectral density
        float[] psd =  new float[fftInput.length / 2 + 1];
        for (int i = 0; i < fftResult.length / 2; i++) {
            float realPart = fftResult[i * 2];
            float imagPart = fftResult[i * 2 + 1];

/* Not using this anymore but keeping the comment here for now

// Power Spectral Density in dB= 20 * log10(sqrt(re^2 + im^2)) using first N/2 complex numbers of FFT output
// from StackOverflow user Ernest Barkowski
// https://stackoverflow.com/questions/6620544/fast-fourier-transform-fft-input-and-output-to-analyse-the-frequency-of-audio
*/

// Power Spectral Density in dB = magnitude(fftResult) ^ 2
// From StackOverflow user Jason R
// https://dsp.stackexchange.com/questions/4691/what-is-the-difference-between-psd-and-squared-magnitude-of-frequency-spectrum?lq=1
            psd[i] = (float) Math.pow(Math.sqrt(Math.pow(realPart, 2) + Math.pow(imagPart, 2)), 2);
        }

        FreqVolPair[] freqBins = new FreqVolPair[psd.length];
        for (int i = 0; i < psd.length; i++) {
            float freq = (float) i * freqBinWidth + freqBinWidth / 2.0f;
            double vol = psd[i];
            freqBins[i] = new FreqVolPair(freq, vol);
        }
        return freqBins;
    }

    //////////////////////////////////// click handlers + miscellaneous ////////////////////////////////////////////

    public void handleCalibClick() {
        this.hearingTest();
    }

    public void handleConfClick() {
        this.confidenceTest();
    }

    public void handleUpClick() {
        this.iModel.setAnswer(true);
    }

    public void handleDownClick() {
        this.iModel.setAnswer(false);
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

    public void mapReplace(HashMap<Float, Integer> map, Float key, Integer newValue) {
        map.remove(key);
        map.put(key, newValue);
    }
}
