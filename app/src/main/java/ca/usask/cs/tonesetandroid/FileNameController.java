package ca.usask.cs.tonesetandroid;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * A class for handling file IO.
 *
 * Note: Directory structure starts at resultsDir, then each subject gets a folder named
 * Subject##, which contains subdirectories HearingTestResults and ConfidenceTestResults
 *
 * @author redekopp
 */
public class FileNameController {

    private Context context;

    private static final File RESULTS_DIR = getResultsDir();

    public FileNameController(Context context) {
        this.context = context;
    }

    public void handleSaveCalibClick() {
        // todo
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
     * Note: does not check whether the directory exists
     *
     * @param id The id number of the subject whose calibration directory is to be found
     * @return A new File with the abstract pathname for the given subject's calibration directory
     */
    private static File getSubjectCalibDir(int id) {
        return new File(getSubjectParentDir(id), "CalibrationTests");
    }

    /**
     * Return true if a folder named "subject##" found in the results directory (where ## is the
     * subject's ID number)
     *
     * @param subjectId The id number of the subject being searched
     * @return True if the subject's folder was found, else false
     */
    public static boolean directoryExistsForSubject(int subjectId) {
        List<String> subjectDirectoryNames = Arrays.asList(RESULTS_DIR.list());
        return subjectDirectoryNames.contains("subject"+subjectId);
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
    public static  void createDirForSubjectID(int id) {
        File newSubjectDir = new File(RESULTS_DIR, "subject" + id);
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
     * Set model.hearingTestResults to the results of a previous test stored in the given file, and initialize
     * information about the last test
     *
     * @param filePath The absolute pathname of the file to be read
     * @param model The model to be initialized from the file
     */
    public static void initializeModelFromFileData(String filePath, Model model) throws FileNotFoundException {
        File file = new File(filePath);
        if (! file.exists()) throw new FileNotFoundException("File does not exist. Pathname: " + filePath);
        Scanner scanner;
        ArrayList<FreqVolPair> newList = new ArrayList<>();
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("Error: file not found");
            e.printStackTrace();
            return;
        }

        // parse test information
        scanner.useDelimiter("\\s");

        // bg noise settings : skipped in Android version but leaving here for now just in case
//        scanner.next(); // skip label
//        String lastNoiseType = scanner.next();
//        // set last noise type from file
//        switch (lastNoiseType) {
//            case "White":  model.setLastNoiseType(Model.NoiseType.White); break;
//            case "Crowd":  model.setLastNoiseType(Model.NoiseType.Crowd); break;
//            case "None" :  model.setLastNoiseType(Model.NoiseType.None);  break;
//            default: throw new RuntimeException("Unexpected noise type in file: " + lastNoiseType);
//        }
//        scanner.next(); // skip label
//        model.setLastBgNoiseVol(Double.parseDouble(scanner.next()));

        scanner.next(); // skip label
        String lastTestType = scanner.next();
        switch(lastTestType) {
            case "PureTone" : model.setLastTestType(Model.TestType.PureTone); break;
            case "Ramp"     : model.setLastTestType(Model.TestType.Ramp);     break;
            default: throw new RuntimeException("Unexpected test type: " + lastTestType);
        }

        scanner.nextLine(); scanner.nextLine(); // skip rest of line and header line
        try {
            while (scanner.hasNext()) {
                double nextFreq = scanner.nextDouble(), nextVol = scanner.nextDouble();
                newList.add(new FreqVolPair((float) nextFreq, nextVol));
                if (scanner.hasNextLine()) scanner.nextLine();
            }
            model.hearingTestResults = newList;
        } catch (NoSuchElementException e) {
            System.out.println("Error reading file: EOF reached before input finished");
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.out.println("Unknown error while reading file");
            e.printStackTrace();
        } finally { scanner.close(); }
    }


    /**
     * A sample method that writes a file called "foo_dir/bar.txt" to the external storage
     * directory and writes some sample text to it, then reads that text. Delete this after you
     * get the hang of it.
     */
    private void sampleWriteFile() {
        // get public directory
        File parentDir = Environment.getExternalStorageDirectory();
        if (parentDir.exists()) System.out.println(parentDir.getAbsolutePath());
        else System.out.println("getExternalStorageDirectory() returned nonexistent file");

        // make new directory in public directory
        File newDir = new File(parentDir, "foo_dir");
        if (newDir.mkdir()) System.out.println(newDir.getAbsolutePath());
        else System.out.println("newDir.mkdir failed");

        // make new file in new directory
        File newFile = new File(newDir, "bar.txt");
        try {
            if (newFile.createNewFile()) System.out.println(newFile.getAbsolutePath());
            else { System.out.println("newFile.createNewFile returned false"); }
        } catch (IOException e) { System.out.println("newFile.createNewFile failed"); }

        // grant permissions for new file
        if (newFile.setReadable(true)) System.out.println("Read permissions successfully granted");
        else System.out.println("Read permissions not granted");

        // make the scanner aware of the new files
        MediaScannerConnection.scanFile(
                this.context,
                new String[]{newFile.getAbsolutePath()},
                new String[]{"text/plain"},
                null);

        // write to new file
        try {
            String text = "Foo Bar Baz";
            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
            writer.write(text, 0, text.length());
            System.out.println("Successfully wrote to file");
            writer.close();
        } catch (IOException e) { System.out.println("Writing to file failed"); }

        // read the new file
        Scanner scanner = null;
        try {
            scanner = new Scanner(newFile);
            System.out.println("File read: " + scanner.nextLine());
        } catch (FileNotFoundException e) { System.out.println("Unknown error: file not found"); }
    }
}
