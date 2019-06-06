package ca.usask.cs.tonesetandroid;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author redekopp, alexscott
 */
public class ConfidenceController {

    private static final int NUM_OF_TESTS = 1;

    Model model;
    ConfidenceInteractionModel iModel;
    Thread testThread;

    /**
     * Set the model for the controller
     */
    public void setModel(Model aModel) {
        model = aModel;
    }

    /**
     * Set the interaction model for the controller
     */
    public void setIModel(ConfidenceInteractionModel anIModel) {
        iModel = anIModel;
    }

    /**
     * Given a Float:Double hashmap, return the Float key closest to the given float f
     *
     * @param f The number that the answer should be closest to
     * @param map The map containing Float:Double pairs
     * @return The closest key to f in the map
     */
    public static Float getClosestKey(Float f, HashMap<Float, Double> map) {
        Float closest = Float.POSITIVE_INFINITY;
        for (Float cur : map.keySet()) if (Math.abs(cur - f) < Math.abs(closest - f)) closest = cur;
        return closest;
    }

    /**
     * Return a new list containing a subset of model.hearingTestResults such that the new list only has the
     * FreqVolPairs with the given frequencies
     *
     * @param frequencies The frequencies of the subset
     * @return The subset containing only the pairs with the given frequencies
     */
    public ArrayList<FreqVolPair> subsetTestResults(float[] frequencies) {
        ArrayList<FreqVolPair> newList = new ArrayList<>();
        for (FreqVolPair p: model.hearingTestResults) if (arrayContains(frequencies, p.freq)) newList.add(p);
        return newList;
    }

    /**
     * Return true if array contains target, else false
     */
    public static boolean arrayContains(float[] array, float target) {
        for (float f : array) if (f == target) return true;
        return false;
    }

    /**
     * Perform the confidence test, generating estimates from the given subset of the test results
     */
    public void beginConfidenceTest(List<FreqVolPair> subset) {

        //set up an array of frequency volume pairs that is to be used for the confidence test
        model.clearConfidenceTestResults();

        model.configureConfidenceTestPairs(subset);

        iModel.disableSave();

        this.setTestThread(subset);

        testThread.start();
    }

    public void setTestThread(final List<FreqVolPair> subset) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //set up the audio output stream
                model.configureAudio();
                model.lineOut.play();

                // for updating GUI elements on main thread
                Handler mainHandler = new Handler(Looper.getMainLooper());

                try {
                    // test each freq twice
                    for (int i = 0; i < NUM_OF_TESTS; i++) {
                        //Randomize the order of the frequency volume pairs used for the confidence test
                        Collections.shuffle(model.getConfidenceTestPairs());
                        for (FreqVolPair pair : model.getConfidenceTestPairs()) {
                            model.enforceMaxVoume();  // force max volume always

                            // Enable yes and no buttons
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    iModel.enableNo();
                                    iModel.enableYes();
                                }
                            });
                            iModel.clearBtns(); // reset the iModel's status for the yes or no buttons being clicked

                            //get the current frequency and volume pair
                            float freq = pair.getFreq();
                            double volume = pair.getVol();

                            // generate the tone corresponding to the above frequency volume pair so long as the user
                            // hasn't pushed a button indicating they heard it or not
                            int index = 0;
                            while (iModel.waitingForClick()) {
                                if (! model.audioPlaying()) return;
                                float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
                                double angle = 2 * index / (period) * Math.PI;
                                short a = (short) (Math.sin(angle) * volume);
                                model.buf[0] = (byte) (a & 0xFF); //write lower 8bits (________WWWWWWWW) out of 16
                                model.buf[1] = (byte) (a >> 8); //write upper 8bits (WWWWWWWW________) out of 16
                                model.lineOut.write(model.buf, 0, 2);
                                index++;
                            }

                            double estimatedVolume = model.getEstimatedMinVolume(pair.getFreq(), subset);

                            //record the results
                            if (iModel.yesPushed) {
                                //Disable yes and no buttons to avoid confusing the user
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        iModel.disableYes();
                                        iModel.disableNo();
                                    }
                                });
                                model.confidenceTestResults.add(
                                        new ConfidenceSingleTestResult(pair, estimatedVolume, true, subset));
                                try {
                                    Thread.sleep((long) (500));//introduce a delay between playing the subsequent tone
                                } catch (InterruptedException e) {
                                    Log.e("ConfidenceController", "Interrupted exception in confidence test");
                                    e.printStackTrace();
                                }
                            } else if (iModel.noPushed) {
                                //Disable yes and no buttons to avoid confusing the user
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        iModel.disableYes();
                                        iModel.disableNo();
                                    }
                                });
                                model.confidenceTestResults.add(
                                        new ConfidenceSingleTestResult(pair, estimatedVolume, false, subset));
                                try {
                                    Thread.sleep((long) (500));//introduce a delay between playing the subsequent tone
                                } catch (InterruptedException e) {
                                    Log.e("ConfidenceController", "Interrupted exception in confidence test");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } finally {
                    model.audioTrackCleanup();
                }

                //after completing the confidence test, make it so the user can no longer click the yes or no button
                //they can now click the save results button
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        iModel.disableYes();
                        iModel.disableNo();
                        iModel.enableSave();
                        iModel.setResultsSaved(false);
                    }
                });
            }
        });
        this.testThread = thread;
    }

    /**
     * Update the iModel to indicate that the user heard the tone (as shown by clicking the yes button)
     */
    public void handleYesClick() {
        iModel.yesBtnClicked();
    }

    /**
     * Update the iModel to indicate that the user did not hear the tone (as shown by clicking the no button)
     */
    public void handleNoClick() {
        iModel.noBtnClicked();
    }

    public void handleExitClick() {

    }

}

