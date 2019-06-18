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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

// todo un-break this (broken parts temporarily commented out)


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

    Model model;

    private static final File RESULTS_DIR = getResultsDir();

    public void setModel(Model model) {
        this.model = model;
    }

    public void handleSaveCalibClick(Context context) throws IllegalStateException {
        if (! this.model.hasResults()) throw new IllegalStateException("No results stored in model");
        
        if (!directoryExistsForSubject(model.getSubjectId())) createDirForSubject(model.getSubjectId());

//        Log.d("saveCalib", "subject directory exists: " + directoryExistsForSubject(model.getSubjectId()));
//        Log.d("saveCalib", "parent directory exists: " + resultsDirExists());
//        Log.d("saveCalib", "target file exists: " + getDestinationFileCalib().exists());

        BufferedWriter out = null;
        File fout = null;
        try {
            fout = getDestinationFileCalib();
            if (! fout.createNewFile())
                throw new RuntimeException("Unable to create output file");
            out = new BufferedWriter(new FileWriter(fout));
            out.write("Frequency(Hz)\t\tVolume\tTimesHeard\tTimesNotHeard");
            out.newLine();
            HearingTestResultsContainer results = model.getHearingTestResults();
            for (float freq : results.getFreqs()) {
                HashMap<Double, Integer> timesHeardPerVol = results.getTimesHeardPerVolForFreq(freq);
                HashMap<Double, Integer> timesNotHeardPerVol = results.getTimesNotHeardPerVolForFreq(freq);
                List<Double> volumes = results.getTestedVolumesForFreq(freq);
                for (Double vol : volumes) {
                    out.write(Float.toString(freq));
                    out.write('\t');
                    out.write(String.format("%.4f", vol));
                    out.write('\t');
                    try {
                        out.write(timesHeardPerVol.get(vol).toString());
                    } catch (NullPointerException e) {
                        out.write('0');
                    }
                    out.write('\t');
                    try {
                        out.write(timesNotHeardPerVol.get(vol).toString());
                    } catch (NullPointerException e) {
                        out.write('0');
                    }
                    out.newLine();
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
     * Get the file where results are to be saved. All parent directories of the file are
     * guaranteed to exist if this method did not throw errors
     *
     * @return The file where the results are to be saved for the current model state
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
     * This method is for saving the results of the confidence hearing test
     * A separate method is used for saving the results of the ramp up hearing test
     *
     * When the user clicks on the get save button, display the results of the
     * confidence hearing test (list the frequencies of the tones and the amplitude they
     * were heard at) Also create a csv file containing the data
     */
    public void handleConfSaveClick(Context context) {

        BufferedWriter out = null;

//        try {
//            File fout = getDestinationFileConf();
//
//            if (! fout.createNewFile()) Log.e("HandleConfSaveClick", "Unable to write to confidence file");
//
//            // make the scanner aware of the new file
//            MediaScannerConnection.scanFile(
//                    context,
//                    new String[]{fout.getAbsolutePath()},
//                    new String[]{"text/csv"},
//                    null);
//
//            out = new BufferedWriter(new FileWriter(fout));
//
//            // write information about test
//            out.write("Calibration: " + model.confidenceTestResults.get(0).getCalibPairs().toString());
//            out.newLine();
//            out.write("Frequency(Hz)" + "\t" + "Vol_Ratio" +"\t" + "Expected" + "\t" + "Actual");
//            out.newLine();
//
//            // Keep track of performance at each volume ratio, frequency, and both ratio and frequency
//            int totalResults = 0, totalCorrect = 0;
//            // results = how many correct for each ratio, totals = how many total for each ratio
//            // "low" = tests with volume ratio < 1, "high" = tests with volume ratio >= 1
//            HashMap<Double, Integer> ratioResults = new HashMap<>(), ratioTotals = new HashMap<>(),
//                    freqResults = new HashMap<>(), freqTotals = new HashMap<>(),
//                    lowResults = new HashMap<>(), lowTotals = new HashMap<>(),
//                    highResults = new HashMap<>(), highTotals = new HashMap<>();
//
//            for (ConfidenceSingleTestResult result : model.confidenceTestResults) {
//                String line = String.format("%f\t%f\t%b\t\t%b\n", result.getFrequency(), result.getVolRatio(),
//                        result.getExpectedResult(), result.getActualResult());
//                out.write(line);
//
//                // update HashMaps for final tally
//                boolean wasCorrect = result.getExpectedResult() == result.getActualResult();
//                totalResults++;
//                if (!ratioResults.containsKey(result.getVolRatio())) { // set up ratio maps
//                    ratioResults.put(result.getVolRatio(), 0);
//                    ratioTotals.put(result.getVolRatio(), 1);
//                } else {
//                    incrMap(ratioTotals, result.getVolRatio());
//                }
//                if (wasCorrect) {
//                    totalCorrect++;
//                    if (ratioResults.containsKey(result.getVolRatio())) {
//                        incrMap(ratioResults, result.getVolRatio());
//                    }
//                }
//                if (!freqResults.containsKey(result.getFrequency())) { // set up frequency maps
//                    freqResults.put(result.getFrequency(), 0);
//                    freqTotals.put(result.getFrequency(), 1);
//                } else {
//                    incrMap(freqTotals, result.getFrequency());
//                }
//                if (wasCorrect) {
//                    incrMap(freqResults, result.getFrequency());
//                }
//                if (result.getVolRatio() < 1) { // set up low maps
//                    if (!lowResults.containsKey(result.getFrequency())) {
//                        lowResults.put(result.getFrequency(), 0);
//                        lowTotals.put(result.getFrequency(), 1);
//                    } else {
//                        incrMap(lowTotals, result.getFrequency());
//                    }
//                    if (wasCorrect) {
//                        incrMap(lowResults, result.getFrequency());
//                    }
//                } else {                       // set up high maps
//                    if (!highResults.containsKey(result.getFrequency())) {
//                        highResults.put(result.getFrequency(), 0);
//                        highTotals.put(result.getFrequency(), 1);
//                    } else {
//                        incrMap(highTotals, result.getFrequency());
//                    }
//                    if (wasCorrect) {
//                        incrMap(highResults, result.getFrequency());
//                    }
//                }
//            }
//
//            // Sort data for nicer output
//            ArrayList<Double> ratioList = new ArrayList<>(ratioTotals.keySet()),
//                    freqList  = new ArrayList<>(freqTotals.keySet());
//            Collections.sort(ratioList);
//            Collections.sort(freqList);
//
//            // write totals for each ratio
//            out.write("------------------ ratios -----------------\n");
//            for (Double ratio : ratioList) {
//                out.write(
//                        String.format("Volume ratio: %f, Total tests performed: %d, Total correct: %d, Accuracy: %f\n",
//                                ratio, ratioTotals.get(ratio), ratioResults.get(ratio),
//                                ((double)ratioResults.get(ratio))/ratioTotals.get(ratio)));
//            }
//            // write totals for each frequency
//            out.write("--------------- frequencies ---------------\n");
//            for (Double freq : freqList) {
//                out.write(String.format("Frequency: %f, Total tests performed: %d, Total correct: %d, Accuracy: %f\n",
//                        freq, freqTotals.get(freq), freqResults.get(freq),
//                        ((double)freqResults.get(freq))/freqTotals.get(freq)));
//                out.write(String.format("\tQuiet tests performed: %d, Total correct: %d, Accuracy: %f\n",
//                        lowTotals.get(freq), lowResults.get(freq), ((double)lowResults.get(freq))/lowTotals.get(freq)));
//                out.write(String.format("\tLoud tests performed: %d, Total correct: %d, Accuracy: %f\n",
//                        highTotals.get(freq), highResults.get(freq),
//                        ((double)highResults.get(freq))/highTotals.get(freq)));
//            }
//            out.write("------------------- total -------------------\n");
//            out.write(String.format("Total tests performed: %d, Total correct: %d, Accuracy: %f",
//                    totalResults, totalCorrect, ((double)totalCorrect)/totalResults));
//
//        } catch (FileNotFoundException e) {
//            // File was not found
//            e.printStackTrace();
//        } catch (IOException e) {
//            // Problem when writing to the file
//            e.printStackTrace();
//        } finally {
//            try {
//                if (out != null) out.close();
//            } catch (IOException e) {
//                System.out.println("Error closing confidence test result file");
//                e.printStackTrace();
//            }
//        }
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
                System.out.println("Error creating HearingTestResults directory");
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
    
    public static boolean resultsDirExists() {
        return RESULTS_DIR.exists();
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
        scanner.useDelimiter("\\s");

        scanner.nextLine(); // skip header
        HearingTestResultsContainer results = new HearingTestResultsContainer();

        try {
            while (scanner.hasNext()) {
                double nextFreq = scanner.nextDouble(), nextVol = scanner.nextDouble();
                int nextHeard = scanner.nextInt(), nextNotHeard = scanner.nextInt();
                for (int i = 0; i < nextHeard; i++) results.addResult((float) nextFreq, nextVol, true);
                for (int i = 0; i < nextNotHeard; i++) results.addResult((float) nextFreq, nextVol, false);
                if (scanner.hasNextLine()) scanner.nextLine();
            }
            model.hearingTestResults = results;
        } catch (NoSuchElementException e) {
            System.out.println("Error reading file: EOF reached before input finished");
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.out.println("Unknown error while reading file");
            e.printStackTrace();
        } finally { scanner.close(); }

        model.printResultsToConsole();
    }

    /**
     * Increases the integer value associated with the given key by 1 in the given map
     *
     * @param map The map to be edited
     * @param key The key in the map whose associated value is to be incremented
     */
    private static void incrMap(HashMap<Double, Integer> map, Double key) {
        int old = map.get(key);
        map.remove(key);
        map.put(key, ++old);
    }
}
