package de.unixkiwi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractionHelper {

    /**
     * Unzips a ZIP file to a specified directory.
     *
     * @return The list of files which were extracted from the archive.
     */
    public static ArrayList<File> unzip(String inputFile, String outputDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile));
        ZipEntry zipEntry;
        ArrayList<File> extractedFiles = new ArrayList<>();

        while ((zipEntry = inputStream.getNextEntry()) != null) {
            File newFile = new File(outputDir, zipEntry.getName());
            extractedFiles.add(newFile);

            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                FileOutputStream outputStream = new FileOutputStream(newFile);
                int size;
                while ((size = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, size);
                }
                outputStream.close();
            }
        }

        inputStream.closeEntry();
        inputStream.close();

        return extractedFiles;
    }

    public static void gunzip(String inputFile, String outputFile) throws IOException {
        byte[] buffer = new byte[1024];
        GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(inputFile));

        FileOutputStream outputStream = new FileOutputStream(outputFile);

        int size;
        while ((size = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, size);
        }

        outputStream.close();
        inputStream.close();
    }
}
