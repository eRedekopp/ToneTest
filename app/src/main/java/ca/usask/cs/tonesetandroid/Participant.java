package ca.usask.cs.tonesetandroid;

import android.provider.Telephony;

import java.io.File;

import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResultsCollection;

/**
 * A class to store information about a particular participant
 *
 * Each Calibration/Ramp test performed by this participant is saved into a single file, and those results are loaded
 * from the file to populate `results` on login. Confidence tests are saved one-file-per-test in a directory for the
 * participant.
 */
public class Participant {

    /**
     * All calibration tests performed by this participant
     */
    private final HearingTestResultsCollection results;

    /**
     * The file containing all of this Participant's calibration test results
     */
    private final File calibFile;

    /**
     * The path to the directory where this Participant's confidence test results are to be saved
     */
    private final File confDir;

    /**
     * The participant's integer ID
     */
    private final int id;

    /**
     * Initialize a new participant with existing calibration data
     *
     * @param id The participant's integer ID
     * @param calib The file which contains this Participant's calibration data
     * @param confDir The directory which contains this Participant's confidence data files
     * @param results A HearingTestResultsCollection containing all existing calibration data
     */
    public Participant(int id, File calib, File confDir, HearingTestResultsCollection results) {
        this.id = id;
        this.calibFile = calib;
        this.confDir = confDir;
        this.results = results;
    }

    /**
     * Initialize a new participant with no existing calibration data
     *
     * @param id The participant's integer ID
     * @param calib The file which contains this Participant's calibration data
     * @param confDir The directory which contains this Participant's confidence data files
     */
    public Participant(int id, File calib, File confDir) {
        this(id, calib, confDir, new HearingTestResultsCollection());
    }

    public HearingTestResultsCollection getResults() {
        return this.results;
    }

    public File getCalibFile() {
        return this.calibFile;
    }

    public File getConfDir() {
        return this.confDir;
    }

    public int getId() {
        return this.id;
    }
}