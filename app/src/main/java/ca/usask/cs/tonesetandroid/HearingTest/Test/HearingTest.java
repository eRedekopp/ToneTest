package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.HearingTestController;
import ca.usask.cs.tonesetandroid.HearingTest.Container.Click;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;
import ca.usask.cs.tonesetandroid.Control.FileIOController;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.Control.HearingTestInteractionModel;
import ca.usask.cs.tonesetandroid.HearingTestView;
import ca.usask.cs.tonesetandroid.Control.Model;

public abstract class HearingTest<T extends Tone> {

    // constants
    public static final float[] CALIB_FREQS = {200, 500, 1000, 2000, 4000};

    // mvc elements
    protected static Model model;
    protected static HearingTestView view;
    protected static Context context; // todo memory leak?
    protected static HearingTestInteractionModel iModel;
    protected static FileIOController fileController;  // todo memory leak in filecontroller?
    protected static HearingTestController controller;

    // Identifiers for individual tests
    protected BackgroundNoiseType backgroundNoiseType;
    protected String testTypeName;  // a unique string identifying the type of test, to be used in save files and logs.
                                    // all ramp tests must contain the word "ramp", reduce tests must contain the
                                    // word "reduce", confidence tests must contain the word "confidence"
    protected String testInfo; // info about this test to be displayed for user

    // elements representing current state
    protected SingleTrialResult currentTrial;
    protected ArrayList<SingleTrialResult> completedTrials;

    // Integers representing the possible answers in a hearing test
    public static final int ANSWER_NULL = 0;
    public static final int ANSWER_UP = 1;
    public static final int ANSWER_DOWN = 2;
    public static final int ANSWER_FLAT = 3;
    public static final int ANSWER_HEARD = 4;


    public static void setModel(Model theModel) {  // todo add this to init
        model = theModel;
    }

    public static void setView(HearingTestView theView) {  // todo add this to init
        view = theView;
    }

    public static void setContext(Context theContext) {  // todo add this to init
        context = theContext;
    }

    public static void setIModel(HearingTestInteractionModel theIModel) {  // todo add this to init
        iModel = theIModel;
    }

    public static void setFileController(FileIOController theFileController) {  // todo add this to init
        fileController = theFileController;
    }

    public static void setController(HearingTestController theController) {
        controller = theController;
    }

    /**
     * Begin or resume this hearing test on a new thread
     */
    protected abstract void run();


    /**
     * @return true if all trials in this hearing test have been completed, else false
     */
    abstract boolean isComplete();

    /**
     * @return A list of all possible answer values for this particular test (ie. HearingTest.ANSWER_*)
     */
    public abstract int[] getRequiredButtons();

    /**
     * Return the information to be written after the header in the save file for the given trial
     *
     * @param result The individual trial result to be saved
     * @return A string with the information to write after the header in the line
     */
    protected abstract String getLineEnd(SingleTrialResult result);

    public HearingTest(BackgroundNoiseType backgroundNoiseType) {  // todo test the backgroundnoisecontroller
        this.completedTrials = new ArrayList<>();
        this.backgroundNoiseType = backgroundNoiseType;
    }

    /**
     * Resume the test if necessary, else do nothing
     */
    public void checkForHearingTestResume() {
        if (    ! iModel.testThreadActive() &&
                iModel.testing() &&
                ! this.isComplete() &&
                ! iModel.testPaused())
            this.run();
    }

    /**
     * Set currentTrial to a new SingleTrialResult with the given tone, and if the current trial is not null, add the
     * current one to the list of results
     */
    protected void newCurrentTrial(T tone) {
        if (this.currentTrial != null) this.completedTrials.add(this.currentTrial);
        this.currentTrial = new SingleTrialResult(tone);
    }

    /**
     * Play a single sine wave via the model
     */
    protected void playSine(float freq, double vol, int durationMs) {
        try {
            byte[] buf = new byte[Model.MIN_AUDIO_BUF_SIZE];
            model.enforceMaxVolume();
            model.startAudio();
            float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
            for (int i = 0; i < durationMs * (float) Model.OUTPUT_SAMPLE_RATE / 1000; ) {
                for (int j = 0; j < Model.MIN_AUDIO_BUF_SIZE; j++, i++) {
                    for (int k = 0; k < 2 && j+k < Model.MIN_AUDIO_BUF_SIZE; k++) {
                        double angle = 2 * i / (period) * Math.PI;
                        short a = (short) (Math.sin(angle) * vol);
                        buf[j+k] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
                        buf[j+k] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
                    }
                }
                model.lineOut.write(buf, 0, Model.MIN_AUDIO_BUF_SIZE);
            }
        } finally {
            model.pauseAudio();
        }
    }

    protected void playSine(FreqVolPair fvp, int durationMs) {
        playSine(fvp.freq(), fvp.vol(), durationMs);
    }

    /**
     * Exactly the same as playSine except it never calls model.startAudio(), model.pauseAudio() or
     * model.enforceMaxVolume()
     */
    protected void playSineRaw(FreqVolPair fvp, int durationMs) {
        playSineRaw(fvp.freq(), fvp.vol(), durationMs);
    }

    protected void playSineRaw(float freq, double vol, int durationMs) {
        byte[] buf = new byte[Model.MIN_AUDIO_BUF_SIZE];
        float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
        for (int i = 0; i < durationMs * (float) Model.OUTPUT_SAMPLE_RATE / 1000; ) {
            for (int j = 0; j < Model.MIN_AUDIO_BUF_SIZE; j++, i++) {
                for (int k = 0; k < 2 && j+k < Model.MIN_AUDIO_BUF_SIZE; k++) {
                    double angle = 2 * i / (period) * Math.PI;
                    short a = (short) (Math.sin(angle) * vol);
                    buf[j+k] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
                    buf[j+k] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
                }
            }
            model.lineOut.write(buf, 0, Model.MIN_AUDIO_BUF_SIZE);
        }
    }

    /**
     * Play the given Earcon via the model
     * @param earcon the earcon to be played
     */
    private void playEarcon(Earcon earcon) {
        try {
            byte[] buf = new byte[2];
            InputStream rawPCM = context.getResources().openRawResource(earcon.audioResourceID);
            model.enforceMaxVolume();
            model.startAudio();

            try {
                while (rawPCM.available() > 0) {
                    rawPCM.read(buf, 0, 2);       // read data from stream

                    byte b = buf[0];              // convert to big-endian todo get rid of this and swap below + test
                    buf[0] = buf[1];
                    buf[1] = b;

                    short sample = (short) (buf[0] << 8 | buf[1] & 0xFF);  // convert to short
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
            model.pauseAudio();
        }
    }

    /**
     * Save the current trial to the output file with the default line-end formatting via the FileIOController
     */
    protected void saveLine() {
        if (this.currentTrial != null) this.saveLine(this.getLineEnd(this.currentTrial));
    }

    /**
     * Save a line to the output file with the given string as the line-end via the FileIOController
     * @param lineEnd The String to be written after the header in the new line
     */
    protected void saveLine(String lineEnd) {

        // todo "heard" and "notHeard" not always correct
        // todo does this even work?

        if (this.backgroundNoiseType == null || this.testTypeName == null) // sanity check
            throw new IllegalStateException("Test not properly initialized: " +
                    (this.backgroundNoiseType == null ? "BackgroundNoiseType = null " : "") +
                    (this.testTypeName == null ? "TestTypeName = null" : ""));

        fileController.saveLine(lineEnd);
    }

    /**
     * Adds a new Click object to this test's results for the current earcon if a test is currently being performed
     * @param answer An int representing the answer associated with this click
     */
    public void handleAnswerClick(int answer) {
        Click newClick = new Click(answer);
        if (this.currentTrial == null) {
            Log.e("handleAnswerClick", "No current trial set");
            return;
        }
        this.currentTrial.addClick(newClick);
    }

    /**
     * Sleep the current thread for a random amount of time between minMs and maxMs milliseconds
     *
     * @param minMs The minimum amount of time to pause the thread
     * @param maxMs The maximum amount of time to pause the thread
     */
    public static void sleepThread(int minMs, int maxMs) {
        try {
            Thread.sleep((long) (minMs + Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    /**
     * Given an int representing a test response, return a string representing the meaning of that response (eg.
     * "Up", "Down")
     */
    public static String answerAsString(int answer) {
        switch (answer) {
            case ANSWER_UP:     return "Up";
            case ANSWER_DOWN:   return "Down";
            case ANSWER_FLAT:   return "Flat";
            case ANSWER_HEARD:  return "Heard";
            default:            return "Unknown";
        }
    }

    public String getTestInfo() {
        return this.testInfo;
    }

    public String getTestTypeName() {
        return this.testTypeName;
    }

    public Date getLastTrialStartTime() {
        return this.currentTrial.getStartTime();
    }

    public BackgroundNoiseType getBackgroundNoiseType() {
        return backgroundNoiseType;
    }

    public void setBackgroundNoiseType(BackgroundNoiseType backgroundNoiseType) {
        this.backgroundNoiseType = backgroundNoiseType;
    }
}
