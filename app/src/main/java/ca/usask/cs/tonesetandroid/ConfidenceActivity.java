package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class ConfidenceActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init self
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confidence_view);

        Intent callerIntent = getIntent();
        Model model = (Model) callerIntent.getSerializableExtra("model");
        model.printResultsToConsole();
    }
}
