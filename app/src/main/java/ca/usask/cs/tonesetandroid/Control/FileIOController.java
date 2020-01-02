package ca.usask.cs.tonesetandroid.Control;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import ca.usask.cs.tonesetandroid.HearingTest.Test.HearingTest;
import ca.usask.cs.tonesetandroid.Participant;

/**
 * A class for saving and reading results 
 */
public class FileIOController {

    /**
     * The parent directory containing all individual participant directories
     */
    private static final File PARENT = getResultsDir();

    /**
     * A DateFormat object for printing to test result files (to avoid recreating identical objects every time we
     * save a result)
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    /**
     * The current file to which saveString() will write
     */
    private File currentFile;

    /**
     * Return the name (not path) of the directory containing a participant's confidence test files
     *
     * @param partID The participant ID whose confidence directory name is to be found
     * @return The name of the participant's confidence directory
     */
    public static String getConfDirName(int partID) {
        return String.format("subject%d", partID);
    }

    /**
     * Return the name (not path) of a new confidence test file for the given participant. The name includes the
     * current time, so only call this immediately before starting a new test
     *
     * @param partID The participant ID whose new confidence test file name is to be found
     * @return The name of a new confidence test file for the given participant
     */
    public static String getNewConfFileName(int partID) {
        return String.format("conf_%s_%d", FORMAT.format(System.currentTimeMillis()), partID);
    }

    /**
     * Return the name (not path) of the file containing a participant's calibration data
     * @param partID The participant ID whose calibration file name is to be found
     * @return The name of the participant's calibration file
     */
    public static String getCalibFileName(int partID) {
        return String.format("Calibration_%d", partID);
    }

    /**
     * Save a single test result to the end of the current file
     */
    public void saveLine(final long startTime, final float freq, final double vol, final String direction,
                         final boolean correct, final int numClicks, final String clickString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String formattedDateTime;

                try {
                    formattedDateTime = FORMAT.format(startTime);
                } catch (NullPointerException e) {
                    Log.e("FileIOController", "Nullpointerexception caused");
                    e.printStackTrace();
                    formattedDateTime = "TimeFetchError";
                }
                saveString(String.format("%s,%.2f,%.2f,%s,%b,%d,%s%n",
                                         formattedDateTime, freq, vol, direction, correct, numClicks, clickString));
            }
        }).start();
    }

    /**
     * Save a line containing test information to the current file (to be called immediately before starting a new test)
     *
     * @param test The hearing test to be begun immediately following this function call
     */
    public void saveTestHeader(HearingTest test) {
        // TODO
    }

    /**
     * Write a line to the current file indicating that a test has completed
     */
    public void saveEndTest() {
        saveString(String.format("END-TEST%n"));
    }

    /**
     * Save the given string to the current file, and also write it to Log.i
     */
    public synchronized void saveString(final String string) {
        if (currentFile == null) throw new IllegalStateException("File not properly configured");
        else if (! currentFile.exists()) throw new IllegalStateException("Target file does not exist");
        else
            try {
                Log.i("FileIOController", string);
                BufferedWriter out = new BufferedWriter(new FileWriter(currentFile, true));
                out.write(string);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("FileIOController",
                        "NullPointerException occurred when writing string to output file: string = " + string +
                        " currentFile = " + (currentFile == null ? "null" : currentFile.getAbsolutePath()));
            }
    }

    /**
     * Return the parent directory for all save files, and create it if necessary
     */
    private static File getResultsDir() {
        File extDir = Environment.getExternalStorageDirectory();
        File subDir = new File(extDir, "HearingTestResults");

        // make results directory if doesn't already exist
        if (!subDir.isDirectory())
            if (! subDir.mkdir())
                Log.e("getResultsDir", "Error creating HearingTestResults directory");
        return subDir;
    }

    /**
     * Create a directory containing an empty calibration file for a new participant.
     * Both the directory and file are guaranteed to exist if this method completes without errors
     *
     * @param partID The new participant's ID
     * @throws IllegalArgumentException If a participant with the given ID already exists
     */
    public void createNewPartFiles(int partID) throws IllegalArgumentException {
        File partDir = new File(PARENT, getConfDirName(partID));
        // create participant directory
        if (partDir.exists())
            throw new IllegalArgumentException("Participant already exists");
        else
            if (! partDir.mkdir())
                throw new RuntimeException("Error creating participant directory");

        // create calibration file
        File calibFile = new File(partDir, getCalibFileName(partID));
        try {
            if (! calibFile.createNewFile()) {
                throw new IOException("Unable to create participant calibration file (createNewFile())");
            }
        } catch (IOException e) {
            Log.e("FileIOController", "IOException occurred while creating participant calibration file");
            e.printStackTrace();
        }
    }

    /**
     * Set the current file to the given file. The file is guaranteed to exist if this method does not
     * throw a FileNotFoundException
     *
     * @param file The file to be set as the new current
     * @param create Should we create the file if it doesn't exist?
     * @throws FileNotFoundException If the file was not found or created
     */
    private void setCurrentFile(File file, boolean create) throws FileNotFoundException {
        if (! file.exists()) {
            if (create) {
                try {
                    if (!file.createNewFile()) {
                        throw new IOException("Unable to create new file");
                    }
                } catch (IOException e) {
                    Log.e("FileIOController", "IOException occurred creating new file in setCurrentFile");
                    e.printStackTrace();
                }
            } else {
                throw new FileNotFoundException();
            }
        }
        this.currentFile = file;
    }

    /**
     * Set the current file to the given file. The file must exist
     *
     * @param file A file that exists
     * @throws FileNotFoundException If the file does not exist
     */
    public void setCurrentFile(File file) throws FileNotFoundException {
        setCurrentFile(file, false);
    }

    /**
     * Set the current file to a new confidence file for the given participant
     *
     * @param p The participant whose results are being saved in the new file
     */
    public void setCurrentConf(Participant p) {
        File file = new File(p.getConfDir(), getNewConfFileName(p.getId()));
        try {
            setCurrentFile(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the current file to the participant's calibration file
     *
     * @param p The participant whose calibration file is to be set as current
     */
    public void setCurrentCalib(Participant p) {
        try {
            setCurrentFile(p.getCalibFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load a participant's calibration results, save all the results into a new Participant object and return it
     *
     * @param partID The integer ID of the participant whose data is to be loaded
     * @return A new Participant with the data loaded from the appropriate file
     * @throws FileNotFoundException if the participant with the given ID does not have any files saved
     * @throws UnfinishedTestException if the file contains a half-finished test
     */
    public Participant loadParticipantData(int partID) throws FileNotFoundException, UnfinishedTestException {
        // TODO
        return null;
    }

    /**
     * @param partID A participant ID
     * @return True if there are files on disk for a participant with the given ID, else False
     */
    public boolean participantExists(int partID) {
        File file = new File(PARENT, getConfDirName(partID));
        return file.exists();
    }

    /**
     * An exception to be thrown when the file IO controller detects an unfinished test in a save file
     */
    public class UnfinishedTestException extends RuntimeException {
        public UnfinishedTestException(String reason) {
            super(reason);
        }
    }
}
