package ca.usask.cs.tonesetandroid.HearingTest.Container;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.usask.cs.tonesetandroid.Control.BackgroundNoiseType;

/**
 * A class to store the results of any HearingTest
 */
public abstract class HearingTestResults {

    /**
     * The background noise type played during the test
     */
    private BackgroundNoiseType noiseType;

    /**
     * The time at which this test started
     */
    private long startTime = -1;

    /**
     * The string identifier for the test type (ie. HearingTest.testTypeName)
     */
    private String testTypeName;

    public HearingTestResults(BackgroundNoiseType noiseType, String testTypeName) {
        this.noiseType = noiseType;
        this.testTypeName = testTypeName;
    }

    /**
     * Are there any results stored in this container?
     */
    public abstract boolean isEmpty();

    public BackgroundNoiseType getNoiseType() {
        return noiseType;
    }

    /**
     * @return The time at which the test started in milliseconds since the beginning of the epoch, or -1 if not yet
     * set
     */
    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getTestTypeName() {
        return this.testTypeName;
    }

    /**
     * @return startTime formatted as yyyy-MM-dd-hh:mm
     */
    public String getFormattedStartTime() {
        if (this.startTime == -1) return "Start_time_not_set";
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
        Date date = new Date(this.startTime);
        return format.format(date);
    }
}
