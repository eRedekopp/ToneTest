package ca.usask.cs.tonesetandroid.Control;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResultsCollection;
import ca.usask.cs.tonesetandroid.HearingTest.Container.PredictorResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
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
     * The string that is written in a test result file to indicate the beginning of a test
     */
    private static final String START_TEST_STRING = "START-TEST";

    /**
     * The string that is written in a test result file to indicate the end of a test
     */
    private static final String END_TEST_STRING = "END-TEST";

    /**
     * The current file to which saveString() will write
     */
    private File currentFile;

    /**
     * The writer we use to output to the current file. To be changed every time the currentFile changes
     */
    private BufferedWriter writer;

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
        // eg. START-TEST 2020-05-18_12:30:59 sine-interval-conf white 10
        //      indicator        date             test name     noise type
        saveString(String.format("%s %s %s %s%n",
                START_TEST_STRING,
                FORMAT.format(System.currentTimeMillis()),
                test.getTestTypeName(),
                test.getBackgroundNoiseType().toString()));
    }

    /**
     * Write a line to the current file indicating that a test has completed
     */
    public void saveEndTest() {
        saveString(String.format("%s%n", END_TEST_STRING));
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
                writer.write(string);
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
        // make sure the file exists, create if requested
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
        // set the file if all went well up to now
        this.currentFile = file;
        // set up the writer
        try {
            if (writer != null) {
                writer.close();
            }
            this.writer = new BufferedWriter(new FileWriter(this.currentFile, true));
        } catch (IOException e) {
            e.printStackTrace();
            this.writer = null;
        }
    }

    /**
     * Set the current file to the given file. The file must exist
     *
     * @param file A file that exists
     * @throws FileNotFoundException If the file does not exist
     */
    public void setCurrentFile(@Nullable File file) throws FileNotFoundException {
        if (file != null) setCurrentFile(file, false);
        else this.currentFile = null;
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
     * @throws InputMismatchException if the file wasn't properly formatted
     */
    public Participant loadParticipantData(int partID)
            throws FileNotFoundException, UnfinishedTestException, InputMismatchException {
        // TODO

        // get the file with the user's calibration results
        File calibFile = new File(getConfDirName(partID), getCalibFileName(partID));

        // make sure it exists
        if (! calibFile.exists()) {
            throw new FileNotFoundException("Calibration file for participant " + partID + " does not exist");
        }

        // read the file, looking for test results
        HearingTestResultsCollection resultsCollection = new HearingTestResultsCollection();
        Scanner scanner = new Scanner(calibFile);
        while (scanner.hasNext()) {
            // when execution gets here, we should always be at the start of a test's results
            BackgroundNoiseType noiseType;
            String testName;
            long startTime;

            // get test info, or throw exception if the test header isn't found
            if (scanner.next().equals(START_TEST_STRING)) {
                // get the start time of the test
                try {
                    startTime = FORMAT.parse(scanner.next()).getTime();
                } catch (ParseException e) {
                    throw new InputMismatchException();
                }
                // get the name of the test
                testName = scanner.next();
                // get the noise type of the test
                String noiseName = scanner.next();
                int noiseVol = scanner.nextInt();
                noiseType = new BackgroundNoiseType(noiseName, noiseVol);
            } else {
                throw new InputMismatchException();
            }

            // get the right type of test results
            PredictorResults testResults;
            if (Pattern.matches("-?calibration-?", testName)) {
                // calibration test
                testResults = new CalibrationTestResults(noiseType, testName);

                // todo

            } else if (Pattern.matches("-?ramp-?", testName)) {
                // ramp test
                testResults = new RampTestResults(noiseType, testName);

                // todo

            } else if (Pattern.matches("-?reduce-?", testName)) {
                // reduce test

                // todo
            }

        }

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
