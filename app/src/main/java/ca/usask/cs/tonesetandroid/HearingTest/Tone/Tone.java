package ca.usask.cs.tonesetandroid.HearingTest.Tone;

import android.support.annotation.NonNull;

/**
 * An interface for anything that can be played through the line out
 */
public interface Tone {
    @Override
    @NonNull
    String toString();
}
