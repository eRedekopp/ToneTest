package ca.usask.cs.tonesetandroid;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public class BackgroundNoiseType {

    // Array indices correspond to ID int (eg. NOISE_TYPE_NONE = 0 -> "No Noise" = NOISE_TYPE_STRINGS[0])
    public static final String[] NOISE_TYPE_STRINGS = {"No Noise", "White Noise"};  // for UI

    public static final String[] NOISE_TYPE_STRINGS_F = {"None", "White"};          // for file io

    public static final int NOISE_TYPE_CROWD = 2;

    public static final int NOISE_TYPE_WHITE = 1;

    public static final int NOISE_TYPE_NONE = 0;

    public final int noiseType;

    public final int volume;

    public BackgroundNoiseType(int noiseType, int volume) throws IllegalArgumentException {
        if (volume < 0 || volume > 100) throw new IllegalArgumentException("Volume out of range");

        this.noiseType = noiseType;
        this.volume = volume;
    }

    public BackgroundNoiseType(String noiseType, int volume) throws IllegalArgumentException {
        if (volume < 0 || volume > 100) throw new IllegalArgumentException("Volume out of range");

        // parse string, set noiseType accordingly
        int typeID = -1;
        for (int i = 0; i < NOISE_TYPE_STRINGS_F.length; i++) {
            if (NOISE_TYPE_STRINGS_F[i].equals(noiseType)) {
                typeID = i;
                break;
            }
        }
        if (typeID == -1) throw new IllegalArgumentException("Unable to parse noise type string");
        else this.noiseType = typeID;

        // set volume
        this.volume = volume;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        try {
            builder.append(NOISE_TYPE_STRINGS_F[noiseType]);
        } catch (ArrayIndexOutOfBoundsException e) {
            builder.append("unknown");
        }
        builder.append(' ');
        builder.append(volume);
        return builder.toString();
    }
}
