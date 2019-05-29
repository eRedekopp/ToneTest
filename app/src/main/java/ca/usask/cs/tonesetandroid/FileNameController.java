package ca.usask.cs.tonesetandroid;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

    private final File resultsDir;

    public FileNameController(Context context) {
        this.context = context;
        this.resultsDir = this.getResultsDir();
    }

    public void handleSaveCalibClick() {

    }

    /**
     * Return true if a folder named "subject##" found in the results directory (where ## is the
     * subject's ID number)
     *
     * @param subjectId The id number of the subject being searched
     * @return True if the subject's folder was found, else false
     */
    public boolean directoryExistsForSubject(int subjectId) {
        List<String> subjectDirectoryNames = Arrays.asList(resultsDir.list());
        return subjectDirectoryNames.contains("subject"+subjectId);
    }

    /**
     * Return the directory which stores the results, and create it if it does not exist
     */
    private File getResultsDir() {
        File extDir = Environment.getExternalStorageDirectory();
        File subDir = new File(extDir, "HearingTestResults");

        // make results directory if doesn't already exist
        if (!subDir.isDirectory())
            if (! subDir.mkdir())
                System.out.println("Error creating HearingTestResults directory");
        return subDir;
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
