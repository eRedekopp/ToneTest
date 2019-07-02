package ca.usask.cs.tonesetandroid;

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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;


/**
 * A class for handling file IO. Methods for reading files are static; must be instantiated
 * to save files
 *
 * Note: Directory structure starts at resultsDir, then each subject gets a folder named
 * Subject##, which contains subdirectories HearingTestResults and ConfidenceTestResults
 *
 * @author redekopp, alexscott
 */
public class FileNameController {

    private Model model;

    private static final File RESULTS_DIR = getResultsDir();

    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * A method to write the results of the calibration test currently stored in the model to a file
     *
     * @param context The context of the calling thread (ie. MainActivity.this)
     * @throws IllegalStateException If there are no calibration test results stored in model
     */
    @SuppressWarnings("ConstantConditions")
    public void handleSaveCalibClick(Context context) throws IllegalStateException {
        if (! this.model.hasResults()) throw new IllegalStateException("No results stored in model");
        
        if (!directoryExistsForSubject(model.getSubjectId())) createDirForSubject(model.getSubjectId());

        BufferedWriter out = null;
        File fout = null;
        try {
            fout = getDestinationFileCalib();
            if (! fout.createNewFile()) throw new RuntimeException("Unable to create output file");
            out = new BufferedWriter(new FileWriter(fout));
            out.write("ParticipantID " + model.getSubjectId());
            out.write("Freq(Hz),Volume,nHeard,nNotHeard");
            out.newLine();
            HearingTestResultsContainer results = model.getHearingTestResults();
            for (float freq : results.getTestedFreqs()) {
                HashMap<Double, Integer> timesHeardPerVol = results.getTimesHeardPerVolForFreq(freq);
                HashMap<Double, Integer> timesNotHeardPerVol = results.getTimesNotHeardPerVolForFreq(freq);
                Collection<Double> volumes = results.getTestedVolumesForFreq(freq);
                for (Double vol : volumes) {
                    int nHeard, nNotHeard;
                    try {
                        nHeard = timesHeardPerVol.get(vol);
                    } catch (NullPointerException e) {
                        nHeard = 0;
                    }
                    try {
                        nNotHeard = timesNotHeardPerVol.get(vol);
                    } catch (NullPointerException e) {
                        nNotHeard = 0;
                    }
                    out.write(String.format("%.1f,%.2f,%d,%d,\n", freq, vol, nHeard, nNotHeard));
                }
            }
        } catch (FileNotFoundException e) {
            // File was not found
            Log.e("FileNameController", "Output file not found");
            e.printStackTrace();
        } catch (IOException e) {
            // Problem when writing to the file
            Log.e("FileNameController", "Unable to write to output file");
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                Log.e("FileNameController", "Error closing test result file");
                e.printStackTrace();
            }
        }

        // make the scanner aware of the new file
        MediaScannerConnection.scanFile(
                context,
                new String[]{fout.getAbsolutePath()},
                new String[]{"text/csv"},
                null);
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
     * A method for writing the results of the confidence test currently stored in the model to a file
     *
     * @param context The context of the calling thread (ie. MainActivity.this)
     */
    public void handleConfSaveClick(Context context) throws IllegalStateException {

        BufferedWriter out = null;

        try {
            File fout = getDestinationFileConf();

            if (! fout.createNewFile()) {
                Log.e("HandleConfSaveClick", "Unable to create confidence file");
                Log.d("HandleConfSaveClick", "fout.exists() = " + fout.exists());
                return;
            }

            out = new BufferedWriter(new FileWriter(fout));

            HearingTestResultsContainer results = model.getHearingTestResults();

            // write calibration results used for this confidence test
            out.write("Confidence test results:\n" + results.toString() + '\n');

            // test using different sample sizes
            for (int n : Model.CONF_SAMP_SIZES) {
                try {  // change hearing test results to new sample size
                    model.hearingTestResults = results.getSubsetResults(n);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                out.write("### Sample Size = " + n + " ###\n");

                // test using different subsets of calibration frequencies
                for (float[] subset : Model.CONF_SUBSETS) {

                    // set model.analysisResults for current subset
                    this.model.analyzeConfidenceResults(subset);

                    // write header/info for current subset
                    out.write("Calibration Freqs: " + Arrays.toString(subset));
                    out.newLine();
                    out.write("Frequency(Hz),Volume,confProb,modelProb,alpha,beta,critLow,critHigh,sigDifferent\n");

                    // write results for each freq-vol pair in subset
                    // model.analysisResults should contain all freq-vol pairs in subset if everything works correctly
                    for (ConfidenceTestResultsContainer.StatsAnalysisResultsContainer result : model.analysisResults) {
                        out.write(String.format(
                                "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%b,\n",
                                result.freq, result.vol, result.confProbEstimate, result.probEstimate,
                                result.alpha, result.beta, result.critLow, result.critHigh, result.estimatesSigDifferent
                        ));
                    }
                    out.newLine();
                }
            }

            model.hearingTestResults = results; // reset hearingTestResults

            // make the scanner aware of the new file
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{fout.getAbsolutePath()},
                    new String[]{"text/csv"},
                    null);

        } catch (FileNotFoundException e) {
            // File was not found
            Log.e("saveConfResults", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            // Problem when writing to the file
            Log.e("saveConfResults", "Error writing to file");
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                Log.e("saveConfResults", "Error closing confidence test result file");
                e.printStackTrace();
            }
        }
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
     * Note: does not check whether the directory exists
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

    /**
     * Initialize the model with the information contained in the file with pathname filePath
     *
     * @param filePath The absolute pathname of the file to be read
     * @param model The model to be initialized from the file
     */
    public static void initializeModelFromFileData(String filePath, Model model) throws FileNotFoundException {
        File file = new File(filePath);
        if (! file.exists()) throw new FileNotFoundException("File does not exist. Pathname: " + filePath);
        Scanner scanner;

        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            Log.e("initializeModel", "Error: file not found");
            e.printStackTrace();
            return;
        }

        // parse test information
        scanner.useDelimiter(",");

        scanner.nextLine(); // skip headers
        scanner.nextLine();
        HearingTestResultsContainer results = new HearingTestResultsContainer();

        try {
            while (scanner.hasNext()) {
                double nextFreq = scanner.nextDouble(), nextVol = scanner.nextDouble();
                int nextHeard = scanner.nextInt();
                int nextNotHeard = scanner.nextInt();
                for (int i = 0; i < nextHeard; i++) results.addResult((float) nextFreq, nextVol, true);
                for (int i = 0; i < nextNotHeard; i++) results.addResult((float) nextFreq, nextVol, false);
                if (scanner.hasNextLine()) scanner.nextLine();
            }
            model.hearingTestResults = results;
        } catch (NoSuchElementException e) {
            Log.e("InitializeModel", "Error reading file: EOF reached before input finished");
            e.printStackTrace();
        } catch (RuntimeException e) {
            Log.e("InitializeModel", "Unknown error while reading file");
            e.printStackTrace();
        } finally { scanner.close(); }
    }
}
