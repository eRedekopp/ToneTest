package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.ArrayList;

/**
 * Contains information about the current/most recent test as well as an interface for generating
 * sine wave audio
 *
 * @author redekopp, alexscott
 */
public class Model {

    public enum TestType {PureTone, Ramp}       // enum for types of hearing tests

    private ArrayList<ModelListener> subscribers;

    // Vars for audio
    AudioTrack line;
    public static final int SAMPLE_RATE = 44100; // sample at 44.1 kHz always
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, 8000}; // From British Society of Audiology
    public int duration_ms; // how long to play each tone in a test
    double volume;          // amplitude multiplier
    byte[] buf;

    // Vars for storing results
    ArrayList<FreqVolPair> hearingTestResults;  // The "just audible" volume for each frequency tested in the most 
                                                // recent pure/ramp test (or loaded from file)
    private int subjectId = -1;     // -1 indicates not set
    private TestType lastTestType;


    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        hearingTestResults = new ArrayList<>();
        this.setUpLine();
    }

    /**
     * Clear any results saved in the model
     */
    public void clearResults() {
        hearingTestResults = new ArrayList<>();
    }

    /**
     * @return True if this model has results saved to it, else false
     */
    public boolean hasResults() {
        return hearingTestResults.size() != 0;
    }

    /**
     * Configure the audio in preparation for a PureTone test
     */
    public void configureAudio() {
        this.duration_ms = 1500;
    }

    /**
     * Perform first time setup of the audio track
     */
    private void setUpLine() {
        if (line == null) {
            int minBufferSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat format =
                    new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                            .setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            line = new AudioTrack(audioAttributes, format, minBufferSize,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            line.setVolume(1.5f);
        }
    }

    public void addSubscriber(ModelListener newSub) {
        subscribers.add(newSub);
    }

    public void setSubjectId(int id) {
        this.subjectId = id;
    }

    public int getSubjectId() {
        return this.subjectId;
    }

    public void setLastTestType(TestType type) {
        this.lastTestType = type;
    }

    /**
     * Print the contents of hearingTestResults to the console (for testing)
     */
    public void printResultsToConsole() {
        StringBuilder builder = new StringBuilder();
        builder.append("### Test Results ###\n");
        builder.append(String.format("Test Subject ID: %d | Test Type: %s\n", this.subjectId, this.lastTestType));
        if (hearingTestResults.isEmpty())
            System.out.println("No results stored in model");
        else for (FreqVolPair fvp : hearingTestResults) {
            builder.append(fvp.toString());
            builder.append('\n');
        }
        Log.i("Model Results", builder.toString());
    }

}
