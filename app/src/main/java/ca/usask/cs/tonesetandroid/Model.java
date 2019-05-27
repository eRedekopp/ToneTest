package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.ArrayList;

public class Model {

    public enum TestType {PureTone, Ramp}       // enum for types of hearing tests

    private ArrayList<ModelListener> subscribers;

    // Vars for audio
    AudioTrack line;
    public static final int SAMPLE_RATE = 44100; // sample at 44.1 kHz always
    public static final float[] FREQUENCIES = {200, 500, 1000, 2000, 4000, 8000}; // From British Society of Audiology
    public int duration_ms;
    double volume;
    byte[] buf;
    private int minBufferSize;
    
    // Vars for storing results
    ArrayList<FreqVolPair> hearingTestResults;  // The "just audible" volume for each frequency tested in the most 
                                                // recent pure/ramp test (or loaded from file)
    
    public Model() {
        buf = new byte[2];
        subscribers = new ArrayList<>();
        hearingTestResults = new ArrayList<>();
        this.setUpLine();
    }

    public void clearResults() {
        hearingTestResults = new ArrayList<>();
    }

    public boolean hasResults() {
        return hearingTestResults.size() != 0;
    }

    public void configureAudio() {
        this.duration_ms = 1500;
    }

    private void setUpLine() {
        if (line == null) {
            minBufferSize = AudioTrack.getMinBufferSize(44100,
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

}
