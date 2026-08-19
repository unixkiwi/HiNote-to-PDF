package de.unixkiwi.hinoteconverter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    static void main() {
        String inputFiles = "src/main/resources/TestNote.hinote";
        String tmpDir = "src/main/resources/TestNote";

        try {
            System.out.println("Extracting: " + inputFiles + " to " + tmpDir);
            List<File> extractedFiles = ExtractionHelper.unzip(inputFiles, tmpDir);
            if (extractedFiles.isEmpty()) {
                System.err.println("No files extracted.");
                return;
            }

            List<File> jhinoteFiles = extractedFiles.stream().filter(file -> file.exists() && file.isFile() && file.getName().endsWith(".jhinote")).toList();

            List<File> rootJhinoteFiles = jhinoteFiles.stream().filter(file -> file.getParent().equals(tmpDir)).toList();
            if (rootJhinoteFiles.size() != 2) {
                System.err.println("Expected 2 jhinote files, but found " + rootJhinoteFiles.size());
                return;
            }

            for (File f : jhinoteFiles) {
                String targetDir = f.getPath().replace("jhinote", "json");
                System.out.println("Extracting: " + f.getPath() + " to " + targetDir);
                ExtractionHelper.gunzip(f.getPath(), targetDir);
                if (!f.delete()) {
                    System.err.println("Could not delete: " + f.getPath());
                }
            }
        } catch (IOException e) {
            System.err.println("Something went wrong: ");
            e.printStackTrace();
        }
    }
}
