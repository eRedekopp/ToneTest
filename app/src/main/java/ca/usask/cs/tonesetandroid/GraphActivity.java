package ca.usask.cs.tonesetandroid;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;


import java.util.Arrays;

/**
 * An activity that displays a graph of freq-vol pairs - data must be previously set by another activity using setData()
 */
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
        Number[] xData = new Float[data.length];   // use wrapper classes for XYSeries constructor
        Number[] yData = new Double[data.length];
        Double maxY = Double.MIN_VALUE;   // keep track of largest Y value for setting axis range
        for (int i = 0; i < data.length; i++) {
            xData[i] = data[i].getFreq();
            yData[i] = data[i].getVol();
            if (maxY < (Double) yData[i]) maxY = (Double) yData[i];
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

        // set boundaries
        this.plot.setDomainBoundaries(0, 8000, BoundaryMode.FIXED); // show 0Hz to 8KHz
        this.plot.setRangeBoundaries(0, maxY, BoundaryMode.FIXED);

        // set label steps
        this.plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 500);
        this.plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);

        // add data to plot
        this.plot.addSeries(dataSeries, lpFormatter);
    }

    private void goBack() {
        this.finish();
    }


}
