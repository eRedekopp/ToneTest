package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import ca.usask.cs.tonesetandroid.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Click;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Earcon;
import ca.usask.cs.tonesetandroid.FileNameController;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.HearingTestInteractionModel;
import ca.usask.cs.tonesetandroid.HearingTestView;
import ca.usask.cs.tonesetandroid.Model;

public abstract class HearingTest {

    // constants
    public static final float[] CALIB_FREQS = {200, 500, 1000, 2000, 4000};

    // mvc elements
    protected static Model model;
    protected static HearingTestView view;
    protected static Context context; // todo memory leak?
    protected static HearingTestInteractionModel iModel;
    protected static FileNameController fileController;

    // Identifiers for individual tests
    protected BackgroundNoiseType backgroundNoiseType;
    protected String testTypeName;
    protected String testInfo; // info about this test to be displayed for user

    // elements representing current state
    protected SingleTrialResult currentTrial;
    protected ArrayList<SingleTrialResult> completedTrials;


    static void setModel(Model theModel) {  // todo add this to init
        model = theModel;
    }

    static void setView(HearingTestView theView) {  // todo add this to init
        view = theView;
    }

    static void setContext(Context theContext) {  // todo add this to init
        context = theContext;
    }

    static void setIModel(HearingTestInteractionModel theIModel) {  // todo add this to init
        iModel = theIModel;
    }

    static void setFileController(FileNameController theFileController) {  // todo add this to init
        fileController = theFileController;
    }

    /**
     * Resume the test if necessary, else do nothing
     */
    public void checkForHearingTestResume() { // todo edit this to make test phase internal to HearingTest
        if (    ! iModel.testThreadActive() &&
                model.getTestPhase() != Model.TEST_PHASE_NULL &&
                ! this.isComplete() &&
                ! iModel.testPaused())
            this.run();
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
     * Return the information to be written after the header in the save file for the given trial
     * @param result The individual trial result to be saved
     * @return A string with the information to write after the header in the line
     */
    protected abstract String getLineEnd(SingleTrialResult result);

    /**
     * Set currentTrial to a new SingleTrialResult with the given tone, and add the current one to the list of results
     */
    protected void newCurrentTrial(Tone tone) {
        this.completedTrials.add(this.currentTrial);
        this.currentTrial = new SingleTrialResult(tone);
    }

    /**
     * Play a single sine wave via the model
     */
    protected void playSine(float freq, double vol, int durationMs) {
        try {
            byte[] buf = new byte[2];
            model.enforceMaxVolume();
            model.startAudio();
            for (int i = 0; i < durationMs * (float) Model.OUTPUT_SAMPLE_RATE / 1000; i++) {
                float period = (float) Model.OUTPUT_SAMPLE_RATE / freq;
                double angle = 2 * i / (period) * Math.PI;
                short a = (short) (Math.sin(angle) * vol);
                buf[0] = (byte) (a & 0xFF); // write lower 8bits (________WWWWWWWW) out of 16
                buf[1] = (byte) (a >> 8);   // write upper 8bits (WWWWWWWW________) out of 16
                model.lineOut.write(buf, 0, 2);
            }
        } finally {
            model.stopAudio();
        }
    }

    protected void playSine(FreqVolPair fvp, int durationMs) {
        playSine(fvp.freq, fvp.vol, durationMs);
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
            model.stopAudio();
        }
    }

    /**
     * Save a test result as an individual line via the FileNameController
     *
     * @param lineEnd The end of the line to be saved, ie. information specific only to the trial being saved
     */
    protected void saveLine() {
        this.saveLine(this.getLineEnd(this.currentTrial));
    }

    protected void saveLine(String lineEnd) {
        if (this.backgroundNoiseType == null || this.testTypeName == null) // sanity check
            throw new IllegalStateException("Test not properly initialized: " +
                    (this.backgroundNoiseType == null ? "BackgroundNoiseType = null " : "") +
                    (this.testTypeName == null ? "TestTypeName = null" : ""));

        fileController.saveLine(lineEnd, this.testTypeName);
    }

    /**
     * Adds a new Click object to this test's results for the current earcon if a test is currently being performed
     * @param direction An int representing the direction that the user answered
     */
    public void handleAnswerClick(int direction) {
        Click newClick = new Click(direction);
        if (this.currentTrial == null) {
            Log.e("handleAnswerClick", "No current trial set");
            return;
        }
        this.currentTrial.addClick(newClick);
    }

    /**
     * Pause the current thread for a random amount of time between minMs and maxMs milliseconds
     * @param minMs The minimum amount of time to pause the thread
     * @param maxMs The maximum amount of time to pause the thread
     */
    public static void pauseThread(int minMs, int maxMs) {
        try {
            Thread.sleep((long) (minMs + Math.random() * (maxMs - minMs)));
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public BackgroundNoiseType getBackgroundNoiseType() {
        return backgroundNoiseType;
    }

    public void setBackgroundNoiseType(BackgroundNoiseType backgroundNoiseType) {
        this.backgroundNoiseType = backgroundNoiseType;
    }
}
