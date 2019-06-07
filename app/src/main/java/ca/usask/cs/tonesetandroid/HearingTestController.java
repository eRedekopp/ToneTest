package ca.usask.cs.tonesetandroid;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A class for performing PureTone and RampUp tests, and handling clicks on the main menu
 *
 * @author redekopp, alexscott
 */
public class HearingTestController {

    Model model;
    HearingTestInteractionModel iModel;


    private static float VOLUME_MULTIPLIER = 1.33f;

    /**
     * Perform a Pure Tone test
     *
     * Plays the specified tone (as denoted by frequency and amplitude) for
     * duration_ms milliseconds
     *
     * Play a tone loud enough that they should hear it Decrease by 10 dB until
     * they no longer hear it Increase by 5dB until they can hear it Go back to
     * the point they could not hear the tone at and increase again until they
     * get two matching within 10%
     */
    public void pureTone() {

        model.clearResults();
        iModel.notHeard();

        // for updating gui elements in main thread
        final Handler mainHandler = new Handler(Looper.getMainLooper());


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                model.configureAudio();
                try {
                    model.lineOut.play();
                    ArrayList<Double> justAudibleVol = new ArrayList<>();
                    for (float freq : Model.FREQUENCIES) {
                        iModel.notHeardTwice();
                        model.volume = 500;
                        boolean decreasePhase = false;
                        boolean increasePhase = false;
                        double notHeardVol = 1;//initially assign to 1;
                        justAudibleVol.clear();
                        while (!iModel.heardTwice) {
                            model.enforceMaxVoume();  // force max volume always
                            if (! model.audioPlaying()) return;
                            iModel.notHeard();
                            for (int i = 0; i < model.duration_ms * (float) 44100 / 1000; i++) {
                                //1000 ms in 1 second
                                if (iModel.heard) {
                                    break;
                                }
                                float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
                                double angle = 2 * i / (period) * Math.PI;
                                short a = (short) (Math.sin(angle) * model.volume);
                                model.buf[0] = (byte) (a & 0xFF); //write lower 8bits (________WWWWWWWW) out of 16
                                model.buf[1] = (byte) (a >> 8); //write upper 8bits (WWWWWWWW________) out of 16
                                model.lineOut.write(model.buf, 0, 2);
                            }

                            if (model.volume == 32767) {
                                //this condition was added to avoid dealing with an infinite loop in the unlikely case that the user never heard the tone
                                model.hearingTestResults.add(new FreqVolPair(freq, model.volume));
                                try {
                                    Thread.sleep((long) (Math.random() * 4000 + 1000)); //silence for 1-5 seconds
                                } catch (InterruptedException e) { break; }
                                break;
                            } else if (!iModel.heard && !decreasePhase && !increasePhase) {
                                //We began the test at a volume that should be noticeable for most people
                                //If the tone was not detected, increase the volume in steps so that they can hear it
                                model.volume *= 10; //should be +10dB

                                //do not exceed maximum volume;
                                //this condition should never happen as we start well max volume
                                if (model.volume > 32767) {
                                    model.volume = 32767;
                                }
                            } else if (iModel.heard && !decreasePhase && !increasePhase) {
                                //The user heard the tone, now we decrease the volume
                                // in an effort to reduce it enough so they can't hear it
                                decreasePhase = true;
                                model.volume /= 5;
                            } else if (iModel.heard && decreasePhase) {
                                //Even though we previously decreased the volume of the tone
                                //they still heard it. Continue to decrease it until they can no longer hear it
                                model.volume /= 5; //decrease until they no longer hear the tone
                            } else if (!iModel.heard && decreasePhase) {
                                //The volume of the tone was decreased to the point they could not hear it
                                //Begin increasing the volume in a smaller step until they can eventually hear it (just heard point)
                                notHeardVol = model.volume;
                                decreasePhase = false;
                                increasePhase = true;
                                model.volume *= VOLUME_MULTIPLIER;

                                //do not exceed maximum volume;
                                if (model.volume > 32767) {
                                    model.volume = 32767;
                                }
                            } else if (!iModel.heard && increasePhase) {
                                //The volume of the tone was previously increased, but the user still did not hear it
                                //Increase the volume by another small step
                                model.volume *= VOLUME_MULTIPLIER;

                                //do not exceed maximum volume;
                                if (model.volume > 32767) {
                                    model.volume = 32767;
                                }

                            } else if (iModel.heard && increasePhase) {
                                //We previously found the point where the user could not hear the tone
                                //We than increased the volume just enough so that they could hear it
                                //Record this volume that they heard it at
                                //Redo this entire process for this frequency until we get the same result (within 10%)
                                //twice
                                if (containsWithinError(justAudibleVol, model.volume, 0.1f)) {
                                    iModel.toneHeardTwice();
                                    model.hearingTestResults.add(new FreqVolPair(freq, model.volume));
                                } else {
                                    justAudibleVol.add(model.volume);
                                }
                                model.volume = 0.5 * Collections.min(justAudibleVol);
                            }
                            //Silence for 1-5 seconds
                            try {
                                Thread.sleep((long) (Math.random() * 4000 + 1000)); //silence for 1-5 seconds
                            } catch (InterruptedException e) { e.printStackTrace(); }
                            System.out.println("Freq: " + freq + ", Vol: " + model.volume + "Just audible vol: " + justAudibleVol);
                        }
                    }
                    model.lineOut.stop();
                } finally {
                    model.audioTrackCleanup();
                }

                //The user can no longer click the heard button since the test is over
                //The user can now start a new hearing test
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        iModel.setTestMode(false);
                    }
                });
            }
        });
        thread.start();
    }

    // todo update javadoc
    /**
     * see new-test-design.txt
     *
     * @author redekopp
     */
    public void hearingTest() {
        // todo add a visual cue when tone plays?
        int REDUCTION_RATE = 10;

        // get upper estimates with rampUpTest()
        this.rampUpTest();

        // find lower limits by lowering volume
        while (! this.model.currentVolumes.isEmpty()) {
            this.model.reduceCurrentVolumes(); // todo
            this.testFrequencies();
        }
        this.model.bottomVolEstimates = this.model.currentVolumes.clone(); // todo shoot lower?

        // configure pairs to be tested
        ArrayList<FreqVolPair> testVolumes = new ArrayList<>();
            // ...

        // test each pair
        // todo new Object for storing results?
            // ...

    }

    /**
     * Test all frequencies in currentVolumes, remove from currentVolumes if not heard
     */
    private void testFrequencies() {
        // todo require not heard twice?
        // todo this
        return;
    }

    /**
     * Perform a ramp up test
     *
     * For each frequency, start quiet then get louder until user hears tone, then go to a fraction of that volume
     * and ramp up again, but slower.
     */
    public void rampUpTest() {

        iModel.notHeard();
        model.clearResults();

        // for updating gui elements on main thread
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        iModel.setTestMode(false);
                    }
                });
            }
            model.setCurrentVolumes(model.getTopVolEstimates.clone());
        });
        thread.start();
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

        model.enforceMaxVoume(); // force max volume always

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

        // apply hann window to reduce noise
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

    /**
     * Reduce the volume component of each FreqVolPair in the list by the given percentage
     * Mutates the original list
     *
     * @param lst A list of FreqVolPairs
     * @param pct The percentage (in [0, 100]) by which to reduce each volume
     */
    public static void reduceAllByPercentage(List<FreqVolPair> lst, int pct) {
        lst.forEach((fvp) -> {
            lst.remove(fvp);
            lst.add(new FreqVolPair(fvp.getFreq(), fvp.getVol() * (1 - pct/100)))
        });
    }

    public void handlePureToneClick() {
        this.iModel.setTestMode(true);
        pureTone();
    }

    public void handleRampUpClick() {
        //Make it so the user can't begin another test
        iModel.setTestMode(true);
        iModel.notifySubscribers();

        //Run the hearing test
        rampUpTest();
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
