package ca.usask.cs.tonesetandroid.Control;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import ca.usask.cs.tonesetandroid.HearingTest.Container.CalibrationTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.HearingTestResultsCollection;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResults;
import ca.usask.cs.tonesetandroid.HearingTest.Container.RampTestResultsWithFloorInfo;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Ramp.RampTest;
import ca.usask.cs.tonesetandroid.HearingTest.Test.Reduce.ReduceTest;
import ca.usask.cs.tonesetandroid.HearingTest.Tone.FreqVolPair;

/**
 * A class for saving and reading results 
 */
public class FileIOController {

    private Model model;

    private Context context;

    /**
     * The parent directory for all save files
     */ 
    private static final File RESULTS_DIR = getResultsDir();

    /**
     * The current file to which saveString() will write
     */
    private File currentFile;

    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * Save the given string with a newline appended
     */
    public void saveLine(final String line) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveString(line + '\n');
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
     * Set the current trial to null
     */
    public void closeFile() {
        this.currentFile = null;
    }

    /**
     * Start a save file for a new HearingTest and set it as this.currentFile
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
     *  @return A new File object with a new path in the CalibrationTests directory. All parent directories
     *          are guaranteed to exist if this method did not throw errors, but does not create the new file
     */
    private File getDestinationFileCalib() {
        int subID = this.model.getSubjectId();

        if (! directoryExistsForSubject(subID)) {
            createDirForSubject(subID);
        }
        File subjectCalibDir = getSubjectCalibDir(subID);

        // get and format current date
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd_hh:mma");
        String formattedDate = dFormat.format(date);

        // eg. subject2/CalibrationTests/sub2_RAMP_2019-05-31_02:35PM.csv
        return new File(subjectCalibDir,
                "sub" + subID + '_' + formattedDate + ".csv"
        );
    }

    /**
     *  @return A new File object with a new path in the ConfidenceTests directory. All parent directories
     *          are guaranteed to exist if this method did not throw errors, but does not create the new file
     */
    private File getDestinationFileConf() {
        int subID = this.model.getSubjectId();
        File subjectConfDir = getSubjectConfDir(subID);
        if (! subjectConfDir.isDirectory())
            throw new IllegalStateException("No directory exists for subject with ID " + subID);

        // get and format current date
        Date date = new Date(System.currentTimeMillis());
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
     * Return a new File with the pathname for the given subject's directory. Does not check whether
     * the directory exists
     *
     * @param id The id number of the subject whose directory is to be found
     * @return A new File with the abstract pathname for the given subject's directory
     */
    private static File getSubjectParentDir(int id) {
        return new File(RESULTS_DIR, "subject"+id);
    }

    /**
     * Return a new File with the pathname for the given subject's calibration test subdirectory. Does not check
     * whether the directory exists
     *
     * @param id The id number of the subject whose calibration directory is to be found
     * @return A new File with the abstract pathname for the given subject's calibration directory
     */
    private static File getSubjectCalibDir(int id) {
        return new File(getSubjectParentDir(id), "CalibrationTests");
    }

    /**
     * Return a new File with the pathname for the given subject's calibration test subdirectory. Does not check
     * whether the directory exists
     *
     * @param id The id number of the subject whose confidence directory is to be found
     * @return A new File with the abstract pathname for the given subject's confidence directory
     */
    private static File getSubjectConfDir(int id) {
        return new File(getSubjectParentDir(id), "ConfidenceTests");
    }

    /**
     * Check whether there is a directory for the subject with the given ID 
     *
     * @param id The id number of the subject being searched
     * @return True if the subject's folder was found, else false
     */
    public static boolean directoryExistsForSubject(int id) {
        List<String> subjectDirectoryNames = Arrays.asList(RESULTS_DIR.list());
        return subjectDirectoryNames.contains(getSubjectParentDir(id).getName());
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

    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Create all required directories for a new test subject
     *
     * @throws IllegalArgumentException If directories already exist for the subject with the given ID
     */
    public static void createDirForSubject(int id) throws IllegalArgumentException {
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

    /**
     * Store all calibrations found for the subject with ID = model.getSubjectId()
     */
    public static void initializeWithAllCalibrations(Model model) {
        String[] allPaths;
        try {
            allPaths = getFileNamesFromCalibDir(model.getSubjectId());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        HearingTestResultsCollection resultsCollection = model.getHearingTestResults();

        for (String pathname : allPaths) {
            // search file for all types of results
            File curFile = new File(pathname);
            CalibrationTestResults calibResults = searchForCalibResults(curFile);
            RampTestResultsWithFloorInfo rampFloorResults = searchForRampFloorResults(curFile);
            RampTestResults rampRegResults;
            if (rampFloorResults == null) rampRegResults = searchForRegRampResults(curFile);
            else rampRegResults = rampFloorResults.getRegularRampResults();

            // add all results found to model.results
            if (calibResults != null) resultsCollection.addResults(calibResults);
            if (rampFloorResults != null) resultsCollection.addResults(rampFloorResults);
            if (rampRegResults != null) resultsCollection.addResults(rampRegResults);
        }
    }

    /**
     * Search the file for calibration results. Return a new CalibrationTestResults containing the results if found,
     * else return null
     */
    public static CalibrationTestResults searchForCalibResults(File file) {
        return null; //todo
    }

    /**
     * Search the file for the results of a ramp test and a reduce test, and return a RampTestResultsWithFloorInfo if
     * both types of results found, else return null
     */
    public static RampTestResultsWithFloorInfo searchForRampFloorResults(File file) {
        return null; //todo
    }

    /**
     * Search the file for the results of a ramp test, and return a RampTestResults if found, else return null
     */
    public static RampTestResults searchForRegRampResults(File file) {
        return null; //todo
    }

    /**
     * Given a model and a file, set the Model such that its stored results are identical to how they were
     * immediately following the CalibrationTest whose results are stored in the file
     * 
     * @param model A Model object whose results are to be initialized from the file
     * @param file A CalibrationTest save file
     * @throws InputMismatchException If the given file was not formatted correctly
     */
    public static void initializeModelFromFile(Model model, File file) throws InputMismatchException {
        // "%s Subject %d, Test %s, Noise %s,"              this.getLineBeginning()
        // "freq(Hz) %.1f, vol %.1f, %s, %d clicks: %s"     CalibrationTest.getLineEnd()

        throw new RuntimeException("Method not complete");



        /*
        Scanner scanner = null, subScanner = null;

        try {

            CalibrationTestResults newCalibResults = new CalibrationTestResults(new BackgroundNoiseType(0, 0));
            RampTestResultsWithFloorInfo newRampResults = new RampTestResultsWithFloorInfo(new BackgroundNoiseType(0, 0));
            ReduceTest.ResultsBuilder reduceResultsBuilder = new ReduceTest.ResultsBuilder();

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
                    subScanner = new Scanner(nextToken);
                    subScanner.useDelimiter(" ");
                    subScanner.next();
                    subScanner.next();
                    subjectID = subScanner.nextInt();
                    subScanner.close();
                } else {
                    scanner.next();
                }

                String testName = scanner.next();  // skip test name
                scanner.next();     // skip noise type

                // Line is from a ramp test
                if (testName.contains("ramp")) {
                    subScanner = new Scanner(scanner.nextLine());
                    subScanner.useDelimiter(" ");
                    String s;
                    subScanner.next();
                    subScanner.next();  // skip "freq" label
                    s = subScanner.next();
                    s = s.substring(0, s.length() - 2);  // remove comma
                    float freq = Float.parseFloat(s);
                    subScanner.next();  // skip "vol1" label
                    s = subScanner.next();
                    s = s.substring(0, s.length() - 2);  // remove comma
                    double vol1 = Double.parseDouble(s);
                    subScanner.next();  // skip "vol2" label
                    s = subScanner.next();
                    s = s.substring(0, s.length() - 2);  // remove comma
                    double vol2 = Double.parseDouble(s);
                    newRampResults.addResult(freq, vol1, vol2);
                    subScanner.close();
                    continue;
                }

                // Line is from a reduce test
                if (testName.contains("reduce")) {
                    subScanner = new Scanner(scanner.nextLine());
                    subScanner.useDelimiter(" ");
                    String s;
                    subScanner.next();
                    subScanner.next();  // skip "freq" label
                    s = subScanner.next();
                    s = s.substring(0, s.length() - 2);  // remove comma
                    float freq = Float.parseFloat(s);
                    subScanner.next();  // skip "vol" label
                    s = subScanner.next();
                    s = s.substring(0, s.length() - 2);  // remove comma
                    double vol = Double.parseDouble(s);
                    reduceResultsBuilder.addResult(freq, vol);
                    subScanner.close();
                    continue;
                }

                // line is from a confidence test
                String freqToken = scanner.next();
                subScanner = new Scanner(freqToken);
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

                newCalibResults.addResult(new FreqVolPair(trialFreq, trialVol), trialHeard);
                scanner.nextLine();
                subScanner.close();
            }

            model.setCalibrationTestResults(newCalibResults);
            newRampResults.setReduceResults(reduceResultsBuilder.build().getResults());
            model.setRampResults(newRampResults);

        } finally {
            if (subScanner != null) subScanner.close();
            if (scanner != null) scanner.close();
        } */
    }
}
