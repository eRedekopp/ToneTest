package ca.usask.cs.tonesetandroid;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Random;

public class BackgroundNoiseController {

    // todo this doesn't actually produce any noise (?)

    private Model model;

    private AudioTrack lineOut = null;

    private int volume;

    public static final int MAX_VOL = 32767;  // same max vol as ramp up test

    public BackgroundNoiseController() {
        this.setupLineOut();
    }

    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * Plays the given noise until model.testing() is no longer true
     *
     * @param noise The noise to be played
     * @throws IllegalStateException if the background noise is of an unknown type
     */
    public void playNoise(BackgroundNoiseType noise) throws IllegalStateException {
        this.volume = convertVolToInternal(noise.volume);

        switch (noise.noiseType) {
            case BackgroundNoiseType.NOISE_TYPE_NONE: break;
            case BackgroundNoiseType.NOISE_TYPE_WHITE: this.playWhiteNoise(); break;
            case BackgroundNoiseType.NOISE_TYPE_CROWD: this.playCrowdNoise(); break;
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
     * Play crowd noise on a new thread until the model is no longer in test mode. Does nothing if the model is not
     * testing when this method is called.
     */
    private void playWhiteNoise() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[2];
                Random random = new Random();
                while (model.testing()) {
                    buffer[0] = (short) (random.nextGaussian() * volume * Short.MAX_VALUE / 10 / 100);
                    buffer[1] = (short) (random.nextGaussian() * volume * Short.MAX_VALUE / 10 / 100);
                    lineOut.write(buffer, 0, 2);
                }
            }
        }).start();
    }

    /**
     * Play crowd noise until the model is no longer in test mode
     */
    private void playCrowdNoise() {

    }

    /**
     * Convert an external volume (from 0 to 100 selected by the user) to an internal volume (from 0 to MAX_VOL)
     *
     * @param externalVol The external vol 0 <= externalVol <= 100
     * @return The volume externalVol % of the way from 0 to MAX_VOL
     */
    private static int convertVolToInternal(int externalVol) {
        return MAX_VOL * (externalVol / 100);
    }

}
