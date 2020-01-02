package ca.usask.cs.tonesetandroid.HearingTest.Test;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.HearingTestController;
import ca.usask.cs.tonesetandroid.Click;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResults;
import ca.usask.cs.tonesetandroid.Control.FileIOController;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;
import ca.usask.cs.tonesetandroid.HearingTest.Container.SingleTrialResult;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;
import ca.usask.cs.tonesetandroid.Control.HearingTestInteractionModel;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.WavTone;
import ca.usask.cs.tonesetandroid.HearingTestView;
import ca.usask.cs.tonesetandroid.Control.Model;

/**
 * Parent class for all hearing tests, in which tones are played via the model and the
 * user's answers are recorded as SingleTrialResults to be used later 
 *
 * @param <T> The type of tones to be played in this HearingTest
 */
public abstract class HearingTest<T extends Tone> {

    // defaults
    protected static final float[] DEFAULT_CALIBRATION_FREQUENCIES = {200, 500, 1000, 2000, 4000};
    protected static final float[] DEFAULT_CONFIDENCE_FREQUENCIES =  {220, 440, 880, 1760, 3520};
    protected static final int DEFAULT_TONE_DURATION_MS = 1500;

    // answer type identifiers
    public static final int ANSWER_NULL = -1;
    public static final int ANSWER_UP = Tone.DIRECTION_UP;
    public static final int ANSWER_DOWN = Tone.DIRECTION_DOWN;
    public static final int ANSWER_FLAT = Tone.DIRECTION_FLAT;
    public static final int ANSWER_HEARD = -2;

    // mvc elements
    protected static Model model;
    protected static HearingTestView view;
    protected static Context context;
    protected static HearingTestInteractionModel iModel;
    protected static FileIOController fileController;
    protected static HearingTestController controller;

    /**
     * The background noise to be played during this test
     */
    protected BackgroundNoiseType backgroundNoiseType;

    /**
     * Human-readable info about the format of this test
     */
    protected String testInfo;

    /**
     * The results of this test
     */
    protected HearingTestResults results;

    /**
     * The time in milliseconds since the start of the epoch at which setStartTime() was first called, or -1 if
     * setStartTime() has not yet been called. Used as an identifier for the test
     */
    protected long startTime = -1;

    /**
     * The current trial being performed in this test, or null if not applicable
     */
    protected SingleTrialResult currentTrial = null;

    /**
     * All trials that have been completed in this test so far
     */
    protected ArrayList<SingleTrialResult> completedTrials;

    /**
     * Begin or resume this hearing test on a new thread
     */
    protected abstract void run();

    /**
     * @return true if all trials in this hearing test have been completed, else return false
     */
    public abstract boolean isComplete();

    /**
     * @return A list of all possible answer values for this particular test (ie. HearingTest.ANSWER_*)
     */
    public abstract int[] getPossibleResponses();

    /**
     * Return a string identifying the type of test, to be used in save files and logs.
     * All ramp tests must contain the word "ramp", reduce tests must contain the
     * word "reduce", confidence tests must contain the word "confidence", and calibration
     * tests must contain the word "calibration"
     */
    public abstract String getTestTypeName();

    /**
     * Return the information to be written after the header in the save file for the given trial
     *
     * @param result The individual trial result to be saved
     * @return A string with information relating specifically to result to be written after the line's header
     */
    protected abstract String getLineEnd(SingleTrialResult result);

    public HearingTest(BackgroundNoiseType backgroundNoiseType) {
        this.completedTrials = new ArrayList<>();
        this.backgroundNoiseType = backgroundNoiseType;
    }

    /**
     * Resume the test if necessary, or do nothing
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
     * Play the audio from the WavTone's resource id via the model
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
                    model.lineOut.write(writeBuf, 0, 1000);    // write sample to line out
                }
            } catch (IOException e) {
                Log.e("playWav", "Error playing wav file");
                e.printStackTrace();
            }
        } finally {
            model.pauseAudio();
        }
    }

    protected void saveLine() {
        fileController.saveLine(this.currentTrial.getStartTime(), this.currentTrial.tone().freq(),
                                this.currentTrial.tone().vol(), this.currentTrial.tone().directionAsString(),
                                this.currentTrial.wasCorrect(), this.currentTrial.nClicks(),
                                this.currentTrial.getClicksAsString());
    }

    /**
     * If a test is currently being performed, add a new click to its current trial with the associated answer value
     *
     * @param answer An int representing the answer associated with this click
     * @param wasTouchInput Was this "click" from an onscreen button press?
     */
    public void handleAnswerClick(int answer, boolean wasTouchInput) {
        Click newClick = new Click(answer, wasTouchInput);
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
     * Given an int representing a test response (eg, ANSWER_UP, ANSWER_DOWN), return a string representing the meaning
     * of that response (eg. "Up", "Down")
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

    /**
     * @return The time in seconds since 1970 at which the current trial was started
     */
    public long getLastTrialStartTime() {
        return this.currentTrial.getStartTime();
    }

    /**
     * Set startTime to the current time. Only changes startTime on the first call - any subsequent calls will do
     * nothing
     */
    public void setStartTime() {
        if (this.startTime == -1) // only change startTime if setStartTime has not been called yet
            this.startTime = System.currentTimeMillis();
        this.results.setStartTime(this.startTime);
    }

    public BackgroundNoiseType getBackgroundNoiseType() {
        return backgroundNoiseType;
    }

    public void setBackgroundNoiseType(BackgroundNoiseType backgroundNoiseType) {
        this.backgroundNoiseType = backgroundNoiseType;
    }

    public HearingTestResults getResults() {
        return this.results;
    }

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
}
