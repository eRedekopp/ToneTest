package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.HearingTestController;
import ca.usask.cs.tonesetandroid.Click;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;
import ca.usask.cs.tonesetandroid.Control.FileIOController;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.Control.HearingTestInteractionModel;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;
import ca.usask.cs.tonesetandroid.HearingTestView;
import ca.usask.cs.tonesetandroid.Control.Model;

public abstract class HearingTest<T extends Tone> {

    // constants
    public static final float[] DEFAULT_CALIBRATION_FREQUENCIES = {200, 500, 1000, 2000, 4000};

    // mvc elements
    protected static Model model;
    protected static HearingTestView view;
    protected static Context context; // todo memory leak?
    protected static HearingTestInteractionModel iModel;
    protected static FileIOController fileController;
    protected static HearingTestController controller;

    // Identifiers for individual tests
    protected BackgroundNoiseType backgroundNoiseType;

    /**
     * A unique string identifying the type of test, to be used in save files and logs.
     * All ramp tests must contain the word "ramp", reduce tests must contain the
     * word "reduce", confidence tests must contain the word "confidence"
     */
    protected String testTypeName;

    /**
     * Human-readable info about the format of this test
     */
    protected String testInfo; // info about this test to be displayed for user

    /**
     * A container to store the results of this test
     */
    HearingTestResults results;

    /**
     * The current trial being performed in this test, or null if test not running
     */
    protected SingleTrialResult currentTrial = null;

    /**
     * All trials that have been completed in this test so far
     */
    protected ArrayList<SingleTrialResult> completedTrials;

    public static final int ANSWER_NULL = 0;
    public static final int ANSWER_UP = 1;
    public static final int ANSWER_DOWN = 2;
    public static final int ANSWER_FLAT = 3;
    public static final int ANSWER_HEARD = 4;


    public static void setModel(Model theModel) {
        model = theModel;
    }

    public static void setView(HearingTestView theView) {
        view = theView;
    }

    public static void setContext(Context theContext) {
        context = theContext;
    }

    public static void setIModel(HearingTestInteractionModel theIModel) {
        iModel = theIModel;
    }

    public static void setFileController(FileIOController theFileController) {
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
    public abstract boolean isComplete();

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

    public HearingTest(BackgroundNoiseType backgroundNoiseType) {
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
        model.enforceMaxVolume();
        model.startAudio();
        byte[] buf = Model.buf;
        for (int i = 0; i < durationMs * (float) 44100 / 1000; i++) {
            float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
            double angle = 2 * i / (period) * Math.PI;
            short a = (short) (Math.sin(angle) * vol);
            buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
            buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
            model.lineOut.write(buf, 0, 2);
        }
        model.pauseAudio();
    }

    protected void playSine(FreqVolPair fvp, int durationMs) {
        playSine(fvp.freq(), fvp.vol(), durationMs);
    }

    /**
     * Play the given Earcon via the model
     * @param earcon the earcon to be played
     */
    protected void playWav(WavTone tone) {

        try {
            byte[] buf = Model.buf;
            short[] writeBuf = new short[1000];
            InputStream rawPCM = context.getResources().openRawResource(tone.wavID());
            model.enforceMaxVolume();
            model.startAudio();

            try {
                Log.d("playWav", "Playing wav with freq: " + tone.freq() + ", vol: " + tone.vol());
                while (rawPCM.available() > 1000) {

                    for (int i = 0; i < 1000; i++) {
                        rawPCM.read(buf, 0, 2);       // read data from stream

                        short sample = (short) (buf[1] << 8 | buf[0] & 0xFF);  // convert to short
                        double amplitude = (double) sample / (double) Short.MIN_VALUE;
                        sample = (short) (amplitude * tone.vol());                   // convert to same vol scale as
                                                                                     // sines
                        writeBuf[i] = sample;
                    }
                    model.lineOut.write(writeBuf, 0, 1000);                 // write sample to line out
                }
            } catch (IOException e) {
                Log.e("playWav", "Error playing wav file");
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

    /**
     * @return The time in seconds since 1970 at which the current trial was started
     */
    public long getLastTrialStartTime() {
        return this.currentTrial.getStartTime();
    }

    public BackgroundNoiseType getBackgroundNoiseType() {
        return backgroundNoiseType;
    }

    public void setBackgroundNoiseType(BackgroundNoiseType backgroundNoiseType) {
        this.backgroundNoiseType = backgroundNoiseType;
    }
}
