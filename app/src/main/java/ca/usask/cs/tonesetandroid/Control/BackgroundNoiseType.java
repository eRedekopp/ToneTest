package ca.usask.cs.tonesetandroid.Control;

import android.support.annotation.NonNull;

/**
 * A class containing information about the background noise (or lack thereof) played during a test
 */
public class BackgroundNoiseType {

    // Array indices correspond to ID int (eg. "No Noise" == NOISE_TYPE_STRINGS[NOISE_TYPE_NONE])

    /**
     * Noise type identifier strings to be displayed to user
     *
     * Array indices correspond to ID int (eg. "No Noise" == NOISE_TYPE_STRINGS[NOISE_TYPE_NONE])
     */
    public static final String[] NOISE_TYPE_STRINGS = {"No Noise", "White Noise", "Crowd Noise"};   // for UI

    /**
     * Noise Type identifier strings to be used in File IO
     *
     * Array indices correspond to ID int (eg. "None" == NOISE_TYPE_STRINGS_F[NOISE_TYPE_NONE])
     */
    public static final String[] NOISE_TYPE_STRINGS_F = {"None", "White", "Crowd"};                 // for file IO

    /**
     * Integer value representing "crowd noise"
     */
    public static final int NOISE_TYPE_CROWD = 2;

    /**
     * Integer value representing "white noise"
     */
    public static final int NOISE_TYPE_WHITE = 1;

    /**
     * Integer value representing "no noise"
     */ 
    public static final int NOISE_TYPE_NONE = 0;

    /**
     * The identifier for the type of this noise
     */
    public final int noiseTypeID;

    /**
     * The volume of this background noise from 0 to 100
     */
    public final int volume;

    /**
     * @param noiseType The integer identifier representing the type of noise (ie. NOISE_TYPE_*)
     * @param volume The volume from 0 to 100 of the background noise
     */
    public BackgroundNoiseType(int noiseType, int volume) throws IllegalArgumentException {
        if (volume < 0 || volume > 100) throw new IllegalArgumentException("Volume out of range");

        this.noiseTypeID = noiseType;
        this.volume = volume;
    }

    /**
     * @param noiseType The String identifier (from NOISE_TYPE_STRINGS_F)
     * @param volume The volume from 0 to 100 of the background noise
     */
    public BackgroundNoiseType(String noiseType, int volume) throws IllegalArgumentException {
        if (volume < 0 || volume > 100) throw new IllegalArgumentException("Volume out of range");

        // parse string, set noiseTypeID accordingly
        int typeID = -1;
        for (int i = 0; i < NOISE_TYPE_STRINGS_F.length; i++) {
            if (NOISE_TYPE_STRINGS_F[i].equals(noiseType)) {
                typeID = i;
                break;
            }
        }
        if (typeID == -1) throw new IllegalArgumentException("Unable to parse noise type string");
        else this.noiseTypeID = typeID;

        // set volume
        this.volume = volume;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        try {
            builder.append(NOISE_TYPE_STRINGS_F[noiseTypeID]);
        } catch (ArrayIndexOutOfBoundsException e) {
            builder.append("unknown");
        }
        builder.append(' ');
        builder.append(volume);
        return builder.toString();
    }

}
