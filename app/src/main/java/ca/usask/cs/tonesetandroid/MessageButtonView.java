package ca.usask.cs.tonesetandroid;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A view that contains a button that starts some Runnable, and a message accompanying it
 */
public class MessageButtonView extends LinearLayout {

    private Button button;
    private TextView textView;

    public MessageButtonView(Context context) {
        super(context);
        this.createViewObjects();

        this.setOrientation(LinearLayout.VERTICAL);

        LayoutParams LLParams =
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        this.setPadding(18, 0, 18, 0);
        this.setWeightSum(6f);
        this.setLayoutParams(LLParams);

        this.addView(this.textView);
        this.addView(this.button);
    }

    public void setMessageText(@Nullable String text) {
        this.textView.setText(text);
    }

    public void setButtonText(@Nullable String text) {
        this.button.setText(text);
    }

    /**
     * Add an actionlistener to the button that causes r to be run in a new thread, or remove the button's
     * actionlistener if r == null
     */
    public void setButtonAction(@Nullable final Runnable r) {
        if (r != null)
            this.button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new Thread(r).start();
                }
            });
        else
            this.button.setOnClickListener(null);
    }

    private void createViewObjects() {
        this.button = new Button(this.getContext());
        this.textView = new TextView(this.getContext());
        this.textView.setTextColor(Color.BLACK);
        this.textView.setTextSize(16);
    }
}
