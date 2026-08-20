package de.unixkiwi.hinoteconverter;

import de.unixkiwi.hinoteconverter.models.JhinotePage;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final byte[] PENKIT_MAGIC = "PENKITINFENG".getBytes(StandardCharsets.UTF_8);

    static void main() {
        String inputFiles = "src/main/resources/Sample.hinote";
        String tmpDir = "src/main/resources/Sample";
        try {
            exportArchive(new File(inputFiles));
        } catch (Exception e) {
            System.err.println("Error while exporting archive: " + e.getMessage());
        }

/*
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

            RootNote rootNote = MAPPER.readValue(new File(rootJhinoteFiles.stream().filter(file -> !file.getPath().contains("custom")).findFirst().orElseThrow().getPath().replace("jhinote", "json")), RootNote.class);

        } catch (IOException e) {
            System.err.println("Something went wrong: ");
            e.printStackTrace();
        }*/
    }

    public static boolean isPenkitBlock(byte[] data) {
        if (data == null || data.length < PENKIT_MAGIC.length) {
            return false;
        }

        return Arrays.mismatch(data, 0, PENKIT_MAGIC.length, PENKIT_MAGIC, 0, PENKIT_MAGIC.length) == -1;
    }

    static void exportArchive(File archiveFile) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        List<Map<String, Object>> pageData = new ArrayList<>();
        Map<String, String> backgroundMap = new HashMap<>();

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            zipFile.entries().asIterator().forEachRemaining(entry -> {
                // extract zip file
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    files.put(entry.getName(), inputStream.readAllBytes());
                } catch (IOException e) {
                    System.err.println("Error while reading entry: " + entry.getName());
                    System.err.println(e.getMessage());
                }

                if (entry.getName().startsWith("pages/") && entry.getName().endsWith(".jhinote")) {
                    try {
                        pageData.add(MAPPER.readValue(ExtractionHelper.decodeJhinoteData(files.get(entry.getName())), JhinotePage.class).customNotePageContent());
                    } catch (IOException e) {
                        System.err.println("Error while decoding '/pages' Jhinote data from entry: " + entry.getName());
                        System.err.println(e.getMessage());
                    }
                } else if (isPenkitBlock(files.get(entry.getName()))) {
                    System.err.println(entry.getName() + " is an unsupported PENKITINFENG infinite-canvas block");
                } else if (!entry.getName().contains("/") && entry.getName().endsWith(".jhinote") && !entry.getName().equals("custom_md.jhinote")) {
                    try {
                        JhinoteRoot note = MAPPER.readValue(ExtractionHelper.decodeJhinoteData(files.get(entry.getName())), JhinoteRoot.class);
                        for (JhinoteRoot.JhinoteRootContent.JhinoteAttachment attachment : note.customNoteContent().attachment()) {
                            if (attachment != null && attachment.id() != null) {
                                String filePath = attachment.filePath();
                                if (filePath != null && !filePath.isBlank()) {
                                    File backgroundFile = new File(filePath);
                                    backgroundMap.put(attachment.id(), backgroundFile.getName());
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error while decoding top-level Jhinote data from entry: " + entry.getName());
                        System.err.println(e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error while processing zip archive: " + archiveFile.getName());
            System.err.println(e.getMessage());
        }

        System.out.println("Loaded page data count: " + pageData.size());
        System.out.println("Loaded background files count: " + backgroundMap.size());
    }

    record JhinoteRoot(JhinoteRootContent customNoteContent) {
        record JhinoteRootContent(List<JhinoteAttachment> attachment) {
            record JhinoteAttachment(String id, String filePath) {
            }
        }
    }
}
