package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMetric;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.TextOrientation;
import com.androidplot.ui.widget.TextLabelWidget;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

public class GraphActivity extends Activity {

    private XYPlot plot;

    private static FreqVolPair[] data;

    /**
     * Set the data to be displayed by the chart. Set this from the caller activity before
     * beginning this activity - to get around having to work with Serializables and stuff
     *
     * @param someData FreqVolPairs representing the spectrogram data to be displayed
     */
    public static void setData(FreqVolPair[] someData) {
        data = someData;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_view);

        this.plot = findViewById(R.id.plot);
        this.setupPlot();

        this.plot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });
    }

    public void setupPlot() {
        if (data == null) throw new IllegalStateException("data not initialized");

        // separate x/y data from FreqVolPairs
        Number[]  xData = new Float[data.length];   // use wrapper classes for XYSeries constructor
        Number[] yData = new Double[data.length];
        for (int i = 0; i < data.length; i++) {
            xData[i] = data[i].getFreq();
            yData[i] = data[i].getVol();
        }

        // create series and formatter
        XYSeries dataSeries = new SimpleXYSeries(
                Arrays.asList(xData),
                Arrays.asList(yData),
                ""
        );
        LineAndPointFormatter lpFormatter = new LineAndPointFormatter(
                Color.BLACK,
                Color.BLACK,
                Color.TRANSPARENT,
                null
        );

        // format labels
        this.plot.setTitle("Current Noise");
//        this.plot.setRangeLabel("Amplitude (dB)");
//        this.plot.setDomainLabel("Frequency (Hz)");
        this.plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append(i);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

        // add data to plot
        this.plot.addSeries(dataSeries, lpFormatter);

    }

    private void goBack() {
        this.finish();
    }


}
