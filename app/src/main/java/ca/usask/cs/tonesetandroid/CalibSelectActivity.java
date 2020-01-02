package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;
import ca.usask.cs.tonesetandroid.Control.HearingTestController;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.Tone;

public class CalibSelectActivity extends Activity {

     // todo test

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calib_select_view);

        // locate view elements
        final RadioGroup noiseGroup     = findViewById(R.id.NoiseRadioGroup);
        final EditText volumeEditText   = findViewById(R.id.VolumeEditTxt);
        final RadioGroup toneGroup      = findViewById(R.id.TimbreRadioGroup);
        final RadioGroup testTypeGroup  = findViewById(R.id.TestTypeRadioGroup);
        final Button goButton           = findViewById(R.id.GoButton);
        final Button cancelButton       = findViewById(R.id.CancelButton);

        // set up event listeners
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int noiseTypeID, noiseVol, toneTimbreID, testTypeID;

                // set noiseTypeID
                switch (noiseGroup.getCheckedRadioButtonId()) {
                    case R.id.NoNoiseRadio:
                        noiseTypeID = BackgroundNoiseType.NOISE_TYPE_NONE;
                        break;
                    case R.id.WhiteNoiseRadio:
                        noiseTypeID = BackgroundNoiseType.NOISE_TYPE_WHITE;
                        break;
                    case R.id.CrowdNoiseRadio:
                        noiseTypeID = BackgroundNoiseType.NOISE_TYPE_CROWD;
                        break;
                    default:
                        throw new RuntimeException("Unknown radio button ID: " + noiseGroup.getCheckedRadioButtonId());
                }

                // set noiseVol
                try {
                    noiseVol = Integer.parseInt(volumeEditText.getText().toString());
                } catch (NumberFormatException e) {
                    showErrorDialog("Unable to parse volume entry: " + volumeEditText.getText().toString());
                    return;
                }
                if (noiseVol < 0 || noiseVol > 100) {
                    showErrorDialog("Volume must be between 0 and 100 - you entered " + noiseVol);
                    return;
                }

                // set toneTimbreID
                switch (toneGroup.getCheckedRadioButtonId()) {
                    case R.id.SineToneRadio:
                        toneTimbreID = Tone.TIMBRE_SINE;
                        break;
                    case R.id.PianoToneRadio:
                        toneTimbreID = Tone.TIMBRE_PIANO;
                        break;
                    default:
                        throw new RuntimeException("Unknown radio button ID: " + toneGroup.getCheckedRadioButtonId());
                }

                // set testTypeID
                switch (testTypeGroup.getCheckedRadioButtonId()) {
                    case R.id.FullTestRadio:
                        testTypeID = HearingTestController.TEST_SUITE_FULL;
                        break;
                    case R.id.RampTestRadio:
                        testTypeID = HearingTestController.TEST_SUITE_RAMP;
                        break;
                    case R.id.RRTestRadio:
                        testTypeID = HearingTestController.TEST_SUITE_RR;
                        break;
                    default:
                        throw new RuntimeException("Unknown radio button ID: "
                                                   + testTypeGroup.getCheckedRadioButtonId());
                }

                // return to caller with entered values
                returnToCaller(noiseTypeID, noiseVol, toneTimbreID, testTypeID);
            }
        });

        // return unsuccessfully if cancelled
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToCaller();
            }
        });

        // force volume = 0 if "no noise" selected
        noiseGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.NoNoiseRadio) {
                    volumeEditText.setText("0");
                    volumeEditText.setInputType(InputType.TYPE_NULL);
                    volumeEditText.setTextIsSelectable(false);
                } else {
                    volumeEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    volumeEditText.setTextIsSelectable(true);
                }
            }
        });
    }

    /**
     * Return successfully with the entered values
     */
    private void returnToCaller(int noiseTypeID, int noiseVol, int toneTimbreID,int testTypeID) {

        // create intent and pass args
        Intent goBackIntent = new Intent();
        goBackIntent.putExtra("noiseTypeID", noiseTypeID);
        goBackIntent.putExtra("noiseVol", noiseVol);
        goBackIntent.putExtra("toneTimbreID", toneTimbreID);
        goBackIntent.putExtra("testTypeID", testTypeID);
        this.setResult(RESULT_OK, goBackIntent);

        // exit this activity
        this.finish();
    }

    /**
     * Return unsuccessfully
     */
    private void returnToCaller() {
        Intent goBackIntent = new Intent();
        this.setResult(RESULT_CANCELED);
        this.finish();
    }

    /**
     * Show a dialog with title "Error" and the given message
     * @param message The message to be displayed
     */
    private void showErrorDialog(String message) {
        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(this);
        warningBuilder.setTitle("Error");
        warningBuilder.setMessage(message);
        warningBuilder.show();
    }
}
