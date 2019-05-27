package ca.usask.cs.tonesetandroid;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A class for handling file IO
 *
 * @author redekopp
 */
public class FileNameController {

    public void handleSaveCalibClick() {

        // Testing - not actually implemented yet

        System.out.println("Attempting file save");

        File outFile = new File(Environment.getExternalStorageDirectory() + "/HearingTestResults");
//        BufferedWriter out = null;
//
//        try {
//            out = new BufferedWriter(new FileWriter(outFile));
//            out.write("Test file 123");
//        }
//        catch (IOException e) {
//            System.out.println("Error writing file");
//            e.printStackTrace();
//        }
//        finally {
//            try {
//                if (out != null) out.close();
//            } catch (IOException e) { System.out.println("Error closing file"); }
//        }
//
//        System.out.println("File save attempt complete");


        System.out.println(outFile.exists());
        System.out.println(outFile.isDirectory());
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
        boolean isPresent = false;
        if (!resultsFolder.exists()) {
            isPresent = resultsFolder.mkdir();
        }
        if (isPresent) System.out.println("Successfully set up directory structure");
        else System.out.println("Error setting up directory structure");
    }


}
