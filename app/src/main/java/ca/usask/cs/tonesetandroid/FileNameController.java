package ca.usask.cs.tonesetandroid;

import android.Manifest;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.view.View;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * A class for handling file IO
 *
 * @author redekopp
 */
public class FileNameController {

    private Context context;

    public FileNameController(Context context) {
        this.context = context;
    }

    public void handleSaveCalibClick() {

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

    private File getPublicDirectory() {
        return new File(
                Environment.getExternalStorageDirectory(),
                "test_file.txt"
                );
    }

    private void setupDirectoryStructure() {
        File resultsFolder = new File(Environment.getExternalStorageDirectory() +
                                      "/HearingTestResults");
        boolean isPresent = resultsFolder.exists();
        if (!resultsFolder.exists()) {
            isPresent = resultsFolder.mkdir();
        }
        if (isPresent) System.out.println("Successfully set up directory structure");
        else System.out.println("Error setting up directory structure");
    }


}
