package ca.usask.cs.tonesetandroid;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class for performing PureTone and RampUp tests, and handling clicks on the main menu
 *
 * @author redekopp, alexscott
 */
public class HearingTestController {

    Model model;
    HearingTestInteractionModel iModel;

    private static final int TONE_DURATION_MS = 1500;

    /**
     * Perform a full hearing test
     *
     * @author redekopp
     */
    @SuppressWarnings("unchecked")
    public void hearingTest() {
        // todo test/tweak this

        // Algorithm:
        //      1 Get estimates for volumes at which the listener will hear the tone 100% of the time for each frequency
        //          - Use RampTest because it tends to overshoot anyway
        //      2 Get estimates for volumes at which the listener will hear the tone 0% of the time for each frequency
        //          - Slowly reduce volumes from RampTest levels until listener can't hear tone
        //      3 Select a set of volumes between the two estimates to test for each frequency
        //      4 Test all frequency-volume combinations selected in step 3 and store the results
        //          - Referred to elsewhere as "main test"

        new Thread(new Runnable() {
            Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                model.configureAudio();

                // get upper estimates with rampUpTest()
                rampUpTest();

                // use upper estimates as a starting off point for lowering volumes
                model.currentVolumes = (ArrayList) model.topVolEstimates.clone();

                // find lower limits by lowering volume until user can't hear
                while (model.continueTest()) {
                    Log.d("asdf", model.currentVolumes.toString());
                    model.reduceCurrentVolumes();
                    testCurrentVolumes();
                }
                // set bottom estimates after results found for each frequency
                model.bottomVolEstimates = (ArrayList) model.currentVolumes.clone();

                // configure pairs to be tested
                model.configureTestPairs();

                // test each pair and store results in model
                mainTest();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        iModel.setTestMode(false);
                        model.audioTrackCleanup();
                    }
                });
            }
        }).start();
    }

    /**
     * Test frequencies at model.currentVolumes and update model accordingly
     */
    @SuppressWarnings("ConstantConditions")
    private void testCurrentVolumes() {
        Collections.shuffle(model.currentVolumes);  // test in random order each time
        for (FreqVolPair fvp : model.currentVolumes) {
            // only test frequencies whose bottom ends hasn't already been estimated
            if (model.timesNotHeardPerFreq.get(fvp.getFreq()) > Model.TIMES_NOT_HEARD_BEFORE_STOP) continue;

            // play the sine, update the map if not heard
            this.playSine(fvp.getFreq(), fvp.getVol(), TONE_DURATION_MS);
            if (! iModel.heard)
                HearingTestSingleFreqResult.mapReplace(model.timesNotHeardPerFreq, fvp.getFreq(),
                        model.timesNotHeardPerFreq.get(fvp.getFreq()) + 1);

        }
    }

    /**
     * Perform the "main" hearing test: test all frequencies in testPairs and save results in model
     *
     * Runs on the current thread: do not call this function from the UI thread
     */
    private void mainTest() {

        // put copies of pairs from testPairs into a new list such that it contains one freqvolpair for each trial in
        // this whole part of the test, then shuffle the new list
        ArrayList<FreqVolPair> allTests = new ArrayList<>();
        for (int i = 0; i < Model.NUMBER_OF_TESTS_PER_VOL; i++) allTests.addAll(model.testPairs);
        Collections.shuffle(allTests);

        // run all the trials
        for (FreqVolPair trial : allTests) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {   // set iModel to notHeard on main thread
                    iModel.notHeard();
                }
            });
            playSine(trial.getFreq(), trial.getVol(), TONE_DURATION_MS);
            model.testResults.addResult(trial.getFreq(), trial.getVol(), iModel.heard);
        }
    }

    public void confidenceTest() {
        // todo

        // configure model for test
        iModel.setTestMode(true);
        model.configureAudio();
        model.configureConfidenceTestPairs();

        final ArrayList<Integer> indices = new ArrayList<>();  // for randomizing test order
        ConfidenceSingleTestResult[] results = new ConfidenceSingleTestResult[model.confidenceTestPairs.size()];
        for (int i = 0; i < results.length; i++) {
            FreqVolPair fvp = model.confidenceTestPairs.get(i);
            ConfidenceSingleTestResult result = new ConfidenceSingleTestResult(model.getProbabilityFVP(fvp), fvp);
            indices.add(i);
        }

        for (int k = 0; k < Model.CONF_NUMBER_OF_TRIALS_PER_FVP; k++) {
            Collections.shuffle(indices);
            // todo test each frequency
        }



    }

    /**
     * Play a sine wave through the Model at the given frequency and volume for the given amount of time.
     * Sets iModel.heard to false before playing, returns if iModel.heard becomes true.
     *
     * @param freq The frequency of the sine wave to be played
     * @param vol The volume of the sine wave to be played
     * @param duration_ms The duration of the sine wave in milliseconds
     */
    private void playSine(float freq, double vol, int duration_ms) {
        model.enforceMaxVolume();
        for (int i = 0; i < duration_ms * (float) 44100 / 1000; i++) {
            if (iModel.heard) break;

            float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
            double angle = 2 * i / (period) * Math.PI;
            short a = (short) (Math.sin(angle) * vol);
            model.buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
            model.buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
            model.lineOut.write(model.buf, 0, 2);
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
    public void rampUpTest() {

        iModel.notHeard();
        model.clearResults();

        model.configureAudio();
        model.lineOut.play();

        double initialHeardVol = 32767;//default with the maximum possible volume (unless the user does not click a button/hit a key to indicate they heard the tone, this value will be overwritten)

        //Loop through all of the frequencies for the hearing test
        for (float freq : Model.FREQUENCIES) {
            double rateOfRamp = 1.05;
            if (model.audioPlaying())
                rampUp(rateOfRamp, freq, 0.1); //play a tone for 50ms, then ramp up by 1.05 times until the tone is heard starting at a volume of 0.1
            else return;

            initialHeardVol = model.volume; //record the volume of the last played tone, this will either be the volume when the user clicked the button, or the maximum possible volume)

            try {
                Thread.sleep((long) (Math.random() * 2000 + 1000));//introduce a slight pause between the first and second ramp
            } catch (InterruptedException e) { break; }

            rateOfRamp = 1.01;
            //redo the ramp up test, this time starting at 1/10th the volume previously required to hear the tone
            //ramp up at a slower rate
            //initially only went up to 1.5*initialHeardVol, but decided to go up to the max instead just incase the user accidently clicked the heard button unitentionally
            rampUp(rateOfRamp, freq, initialHeardVol / 10.0);

            FreqVolPair results = new FreqVolPair(freq, model.volume);//record the frequency volume pair (the current frequency, the just heard Volume)
            model.topVolEstimates.add(results);//add the frequency volume pair to the results array list

            try {
                Thread.sleep((long) (Math.random() * 2000 + 1000));
            } catch (InterruptedException e) { break; }
        }
        model.audioTrackCleanup();


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

        for (model.volume = startingVol; model.volume < 32767; model.volume *= rateOfRamp) {
            if (! model.audioPlaying()) return;

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
    }

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
     * @return FreqVolPairs representing each frequency bin and its corresponding amplitude in the periodogram
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
            // Power Spectral Density in dB= 20 * log10(sqrt(re^2 + im^2)) using first N/2 complex numbers of FFT output
            // from StackOverflow user Ernest Barkowski
            // https://stackoverflow.com/questions/6620544/fast-fourier-transform-fft-input-and-output-to-analyse-the-frequency-of-audio
            psd[i] = (float) (20 * Math.log10(Math.sqrt(Math.pow(realPart, 2) + Math.pow(imagPart, 2))));
        }

        FreqVolPair[] freqBins = new FreqVolPair[psd.length];
        for (int i = 0; i < psd.length; i++) {
            float freq = (float) i * freqBinWidth;
            double vol = psd[i];
            freqBins[i] = new FreqVolPair(freq, vol);
        }
        return freqBins;
    }

    public void handleCalibClick() {
        iModel.setTestMode(true);
        this.hearingTest();
    }

    public void handleConfClick() {
        // todo
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
}
