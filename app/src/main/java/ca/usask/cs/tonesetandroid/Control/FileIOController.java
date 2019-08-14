package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

/**
 * A class for handling file IO. Methods for reading files are static; must be instantiated
 * to save files
 *
 * Note: Directory structure starts at resultsDir, then each subject gets a folder named
 * Subject##, which contains subdirectories CalibrationTestResults and ConfidenceTestResults
 *
 * @author redekopp, alexscott
 */
public class FileIOController {

    private Model model;

    private HearingTestInteractionModel iModel;

    private Context context;

    private static final File RESULTS_DIR = getResultsDir();

    private File currentFile;

    public void setModel(Model model) {
        this.model = model;
    }

    public void setiModel(HearingTestInteractionModel iModel) {
        this.iModel = iModel;
    }

    /**
     * Saves a line representing an individual trial to the current file
     *
     * @param lineEnd The end of the line, ie. information specific only to the trial being saved
     */
    public void saveLine(final String lineEnd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveString(String.format("%s %s%n", getLineBeginning(), lineEnd));
            }
        }).start();
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
                BufferedWriter out = new BufferedWriter(new FileWriter(currentFile, true)); // todo
                                                                            // nullpointerexception happened here
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
     * Close the current file and reset this.currentFile to null
     */
    public void closeFile() {
        this.currentFile = null;
    }

    /**
     * @return Given the state of model and iModel, return a String with subject ID, current date/time, test type
     * name, and background noise type/volume
     */
    private String getLineBeginning() {
        Date startTime = null;
        SimpleDateFormat dateFormat = null;
        try {
            try {
                startTime = this.iModel.getCurrentTest().getLastTrialStartTime();
            } catch (NullPointerException e) {
                startTime = Calendar.getInstance().getTime();
            }
            dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String formattedDateTime = dateFormat.format(startTime);  // todo NullPointerException occurred here
            return String.format("%s Subject %d, Test %s, Noise %s,",
                    formattedDateTime, model.getSubjectId(), iModel.getCurrentTest().getTestTypeName(),
                    iModel.getCurrentTest().getBackgroundNoiseType().toString());
        } catch (NullPointerException e) {
            Log.e("getLineBeginning", "Nullpointerexception caused - dateFormat = " +
                    (dateFormat == null ? "null" : dateFormat.toPattern()) + " Date = " +
                    (startTime == null ? "null" : startTime.toString()));
            e.printStackTrace();
            return "NULL_UNKNOWN";
        }
    }

    /**
     * Start a new save file for a new HearingTest and set it as this.currentFile, also configureTestTones this.out
     *
     * @param isCalib Is the new file for a calibration test?
     */
    public void startNewSaveFile(boolean isCalib) {
        try {
            if (isCalib) this.currentFile = this.getNewCalibSaveFile();
            else this.currentFile = this.getNewConfSaveFile();
            // make the scanner aware of the new file
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{this.currentFile.getAbsolutePath()},
                    new String[]{"text/csv"},
                    null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create and return a new file with an appropriate name for a new calibration test
     *
     * @return A new file with an appropriate name for new calibration test
     * @throws IOException If the file was unable to be created
     */
    private File getNewCalibSaveFile() throws IOException {
        File newFile = getDestinationFileCalib();
        if (! newFile.createNewFile()) throw new IOException("Unable to create new calibration save file");
        else return newFile;
    }

    /**
     * Create and return a new file with an appropriate name for a new confidence test
     *
     * @return A new file with an appropriate name for new confidence test
     * @throws IOException If the file was unable to be created
     */
    private File getNewConfSaveFile() throws IOException {
        File newFile = getDestinationFileConf();
        if (! newFile.createNewFile()) throw new IOException("Unable to create new confidence save file");
        else return newFile;
    }

    /**
     * Get the file where calibration results are to be saved. All parent directories of the file are
     * guaranteed to exist if this method did not throw errors. Does not create the file
     *
     * @return The file where the calibration results are to be saved for the current model state
     */
    private File getDestinationFileCalib() {
        int subID = this.model.getSubjectId();

        if (! directoryExistsForSubject(subID)) {
            createDirForSubject(subID);
        }
        File subjectCalibDir = getSubjectCalibDir(subID);

        // get and format current date
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd_hh:mma");
        String formattedDate = dFormat.format(date);

        // eg. subject2/CalibrationTests/sub2_RAMP_2019-05-31_02:35PM.csv
        return new File(subjectCalibDir,
                "sub" + subID + '_' + formattedDate + ".csv"
        );
    }

    /**
     * Get the file where confidence results are to be saved. All parent directories of the file are guaranteed to
     * exist if this method did not throw errors. Does not create the file
     *
     * @return The file where the confidence results are to be saved for the current model state
     */
    private File getDestinationFileConf() {
        int subID = this.model.getSubjectId();
        File subjectConfDir = getSubjectConfDir(subID);
        if (! subjectConfDir.isDirectory())
            throw new IllegalStateException("No directory exists for subject with ID " + subID);

        // get and format current date
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd_hh:mma");
        String formattedDate = dFormat.format(date);

        // eg. subject2/ConfidenceTests/sub2_conf_2019-05-31_02:35PM.csv
        return new File(subjectConfDir,
                "sub" + subID + "_conf_" + formattedDate + ".csv"
        );
    }

    /**
     * Return an array of all the saved calibration file names in the directory for the given subject
     *
     * @param id The id of the test subject whose directory is to be searched
     * @return An array of all the saved calibration file names in the subject's directory
     * @throws FileNotFoundException If the subject's calibration directory doesn't exist
     */
    public static String[] getFileNamesFromCalibDir(int id) throws FileNotFoundException {
        // check if directory exists, return a list of files within if it does
        File subjectCalibDir = getSubjectCalibDir(id);
        if (!subjectCalibDir.exists())
            throw new FileNotFoundException("Subject's calibration directory does not exist");
        if (!subjectCalibDir.isDirectory())
            throw new RuntimeException("Subject's calibration pathname found but is not a directory");

        return subjectCalibDir.list(); // return array of filenames
    }

    /**
     * Return the file of the given name from within the given subject's calibration directory
     *
     * @param id The ID of the subject whose calibration directory is to be searched
     * @param fileName The filename to be retrieved from the subject's calibration directory
     * @return The requested file from the subject's calibration directory
     * @throws FileNotFoundException If the file does not exist within the subject's calibration directory, or
     *                               the directory does not exist
     */
    public static File getCalibFileFromName(int id, String fileName) throws FileNotFoundException {
        File subjectCalibDir = getSubjectCalibDir(id);
        if (! subjectCalibDir.isDirectory())
            throw new FileNotFoundException("Subject's calibration directory not found");
        List<String> calibFileNames = Arrays.asList(subjectCalibDir.list());
        if (! calibFileNames.contains(fileName))
            throw new FileNotFoundException("Filename not found in subject's calibration directory");
        else
            return new File(subjectCalibDir, fileName);
    }

    /**
     * Return a new File with the abstract pathname for the given subject's directory
     *
     * Note: does not check whether the directory exists
     *
     * @param id The id number of the subject whose directory is to be found
     * @return A new File with the abstract pathname for the given subject's directory
     */
    private static File getSubjectParentDir(int id) {
        return new File(RESULTS_DIR, "subject"+id);
    }

    /**
     * Return a new File with the abstract pathname for the given subject's calibration
     * test subdirectory
     *
     * Note: does not check whether the directory existsgetNoiseType
     *
     * @param id The id number of the subject whose calibration directory is to be found
     * @return A new File with the abstract pathname for the given subject's calibration directory
     */
    private static File getSubjectCalibDir(int id) {
        return new File(getSubjectParentDir(id), "CalibrationTests");
    }

    /**
     * Return a new File with the abstract pathname for the given subject's confidence
     * test subdirectory
     *
     * Note: does not check whether the directory exists
     *
     * @param id The id number of the subject whose confidence directory is to be found
     * @return A new File with the abstract pathname for the given subject's confidence directory
     */
    private static File getSubjectConfDir(int id) {
        return new File(getSubjectParentDir(id), "ConfidenceTests");
    }

    /**
     * Return true if a folder named "subject##" found in the results directory (where ## is the
     * subject's ID number)
     *
     * @param id The id number of the subject being searched
     * @return True if the subject's folder was found, else false
     */
    public static boolean directoryExistsForSubject(int id) {
        List<String> subjectDirectoryNames = Arrays.asList(RESULTS_DIR.list());
        return subjectDirectoryNames.contains(getSubjectParentDir(id).getName());
    }

    /**
     * Return the directory which stores the results, and create it if it does not exist
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

    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Set up directory structure for a new test subject. Directory must not already exist.
     *
     * @param id The ID of the new test subject
     */
    public static void createDirForSubject(int id) {
        File newSubjectDir = getSubjectParentDir(id);
        if (newSubjectDir.exists())
            throw new IllegalArgumentException("Directory already exists for subject with ID " + id);
        boolean subDirWasCreated = newSubjectDir.mkdir();
        if (! subDirWasCreated)
            throw new RuntimeException("Error: directory " + newSubjectDir.getPath() + " not successfully created");
        File[] newDirs =  { new File(newSubjectDir.getPath() + "/CalibrationTests"),
                            new File(newSubjectDir.getPath() + "/ConfidenceTests"),
                            new File(newSubjectDir.getPath() + "/Graphs") };
        for (File dir : newDirs) {
            boolean dirWasCreated = dir.mkdir();
            if (! dirWasCreated)
                throw new RuntimeException("Error: directory " + dir.getPath() + " not successfully created");
        }
    }

    public static void initializeModelFromFile(Model model, File file) {
        // "%s Subject %d, Test %s, Noise %s,"              this.getLineBeginning()
        // "freq(Hz) %.1f, vol %.1f, %s, %d clicks: %s"     CalibrationTest.getLineEnd()

        Scanner scanner;
        CalibrationTestResults newResults = new CalibrationTestResults();
        try {
            scanner = new Scanner(file);
            scanner.useDelimiter(",");
        } catch (FileNotFoundException e) {
            Log.e("initializeModel", "File not found");
            e.printStackTrace();
            return;
        }

        int subjectID = -1;

        while (scanner.hasNext()) {
            if (subjectID == -1) {  // set subject id if necessary
                String nextToken = scanner.next();
                Scanner subScanner = new Scanner(nextToken);
                subScanner.useDelimiter(" ");
                subScanner.next();
                subScanner.next();
                subjectID = subScanner.nextInt();
                subScanner.close();
            } else {
                scanner.next();
            }

            String testName = scanner.next();  // skip test name
            // skip if trial is from a ramp or reduce test
            if (testName.contains("ramp") || testName.contains("reduce")) { scanner.nextLine(); continue; }
            scanner.next();     // skip noise type

            String freqToken = scanner.next();
            Scanner subScanner = new Scanner(freqToken);
            subScanner.useDelimiter(" ");
            subScanner.next();  // skip "freq" label
            float trialFreq = subScanner.nextFloat();

            String volToken = scanner.next();
            subScanner = new Scanner(volToken);
            subScanner.next();  // skip "vol" label
            double trialVol = subScanner.nextDouble();

            String heardString = scanner.next();
            boolean trialHeard;
            if (heardString.toLowerCase().matches("\\s*heard")) trialHeard = true;
            else if (heardString.toLowerCase().matches("\\s*notheard")) trialHeard = false;
            else throw new RuntimeException("Unknown 'heard' indicator in file: " + heardString);

            newResults.addResult(new FreqVolPair(trialFreq, trialVol), trialHeard);
            scanner.nextLine();
            subScanner.close();
        }

        model.setCalibrationTestResults(newResults);
    }
}
