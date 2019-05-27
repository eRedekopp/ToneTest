package ca.usask.cs.tonesetandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HearingTestController {

    Model model;
    HearingTestInteractionModel iModel;


    private static float VOLUME_MULTIPLIER = 1.33f;

    /**
     * Plays the specified tone (as denoted by frequency and amplitude) for
     * duration_ms milliseconds
     *
     * Play a tone loud enough that they should hear it Decrease by 10 dB until
     * they no longer hear it Increase by 5dB until they can hear it Go back to
     * the point they could not hear the tone at and increase again until they
     * get two matching
     */
    public void pureTone() {

        iModel.notHeard();
        model.clearResults();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                model.configureAudio();
                try {
                    model.line.play();
                    ArrayList<Double> justAudibleVol = new ArrayList<>();
                    for (float freq : Model.FREQUENCIES) {
                        iModel.notHeardTwice();
                        model.volume = 500;
                        boolean decreasePhase = false;
                        boolean increasePhase = false;
                        double notHeardVol = 1;//initially assign to 1;
                        justAudibleVol.clear();
                        while (!iModel.heardTwice) {
                            iModel.notHeard();
                            for (int i = 0; i < Model.DURATION_MS * (float) 44100 / 1000; i++) { //1000 ms in 1 second
                                if (iModel.heard) {
                                    break;
                                }
                                float period = (float) Model.SAMPLE_RATE / freq;
                                double angle = 2 * i / (period) * Math.PI;
                                short a = (short) (Math.sin(angle) * model.volume);
                                model.buf[0] = (byte) (a & 0xFF); //write lower 8bits (________WWWWWWWW) out of 16
                                model.buf[1] = (byte) (a >> 8); //write upper 8bits (WWWWWWWW________) out of 16
                                model.line.write(model.buf, 0, 2);
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
                    model.line.stop();
                } finally {
                    model.line.stop();
                    model.line.release();
                }

                //Allow the user to click the get results button
                //The user can no longer click the heard button since the test is over
                //The user can now start a new hearing test
                iModel.setTestMode(false);
                iModel.notifySubscribers();
            }
        });
        thread.start();
    }

    public void handlePureToneClick() {
        this.iModel.setTestMode(true);
        pureTone();
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


}
