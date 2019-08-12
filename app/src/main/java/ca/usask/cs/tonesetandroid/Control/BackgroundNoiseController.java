package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import java.util.Random;

import ca.usask.cs.tonesetandroid.R;


public class BackgroundNoiseController {


    private HearingTestInteractionModel iModel;

    private AudioTrack lineOut = null;

    private MediaPlayer mediaPlayer = null;

    private Context context;  // the context to use for new objects created

    public static final int MAX_VOL = Short.MAX_VALUE;

    public BackgroundNoiseController(Context context) {
        this.context = context;
        this.setupLineOut();
        this.setupMediaPlayer();
    }

    /**
     * Plays the given noise until model.testing() is no longer true
     *
     * @param noise The noise to be played
     * @throws IllegalStateException if the background noise is of an unknown type
     */
    public void playNoise(BackgroundNoiseType noise) throws IllegalStateException {
        switch (noise.noiseType) {
            case BackgroundNoiseType.NOISE_TYPE_NONE:
                break;
            case BackgroundNoiseType.NOISE_TYPE_WHITE:
                this.playWhiteNoise(convertVolToInternal(noise.volume)); break;
            case BackgroundNoiseType.NOISE_TYPE_CROWD:
                this.playCrowdNoise(convertVolToInternal(noise.volume)); break;
            default: throw new IllegalStateException("Unknown noise type identifier: " + noise.noiseType);
        }
    }

    private void setupLineOut() {
        if (lineOut == null || lineOut.getState() == AudioTrack.STATE_UNINITIALIZED) {
            int minBufferSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat format =
                    new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                            .setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build();
            lineOut = new AudioTrack(audioAttributes, format, minBufferSize,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            lineOut.setVolume(1.0f); // unity gain - no amplification
        }
    }

    /**
     * Sets up the MediaPlayer to be ready to play crowd noise
     */
    private void setupMediaPlayer() {
        if (mediaPlayer == null) // perform on separate thread because sometimes takes a few seconds to prepare
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AudioAttributes audioAttributes =
                            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                    mediaPlayer = MediaPlayer.create(context, R.raw.crowdnoise, audioAttributes,
                            AudioManager.AUDIO_SESSION_ID_GENERATE);
                    if (mediaPlayer == null)
                        throw new RuntimeException("Error creating MediaPlayer");
                    else mediaPlayer.setLooping(true);
                }
            }).start();
    }

    /**
     * Play crowd noise on a new thread until the model is no longer in test mode. Does nothing if the model is not
     * testing when this method is called.
     *
     * @param volume The volume at which the noise is to be played, 0 <= volume <= MAX_VOL
     */
    private void playWhiteNoise(final int volume) {
        if (volume > MAX_VOL) throw new IllegalArgumentException("Volume out of range : " + volume);
        new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[2];
                Random random = new Random();
                lineOut.play();
                while (iModel.testing()) {
                    buffer[0] = (short) (random.nextGaussian() * volume);
                    buffer[1] = (short) (random.nextGaussian() * volume);
                    lineOut.write(buffer, 0, 2);
                }
                lineOut.stop();
                lineOut.flush();
            }
        }).start();
    }

    /**
     * Play crowd noise until the model is no longer in test mode
     *
     * @param volume The volume at which the noise is to be played, 0 <= volume <= MAX_VOL
     */
    private void playCrowdNoise(final int volume) {
        // todo make this event-based
        if (volume > MAX_VOL) throw new IllegalArgumentException("Volume out of range : " + volume);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
                float floatVol = (float) (volume) / (float) MAX_VOL;
                mediaPlayer.setVolume(floatVol, floatVol);
                while (iModel.testing()) continue;  // Continue playing until iModel.testing() becomes false
                mediaPlayer.pause();
            }
        }).start();

    }

    /**
     * Convert an external volume (from 0 to 100 selected by the user) to an internal volume (from 0 to MAX_VOL)
     *
     * @param externalVol The external vol 0 <= externalVol <= 100
     * @return The volume externalVol % of the way from 0 to MAX_VOL
     */
    private static int convertVolToInternal(int externalVol) {
        return (int) Math.round((double) MAX_VOL * ((double) externalVol / 100.0));
    }

    public void setiModel(HearingTestInteractionModel iModel) {
        this.iModel = iModel;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
