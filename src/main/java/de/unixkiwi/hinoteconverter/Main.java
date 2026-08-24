package de.unixkiwi.hinoteconverter;

import de.unixkiwi.hinoteconverter.models.*;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final byte[] PENKIT_MAGIC = "PENKITINFENG".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PENCIL_ENGINE = "PENCILENGINE".getBytes(StandardCharsets.UTF_8);
    private static final long POINT_STRIDE = 36;

    static void main() {
        String inputFiles = "src/main/resources/Sample.hinote";
        String tmpDir = "src/main/resources/Sample";
        String outPath = "src/main/resources/SampleOut";
        try {
            exportArchive(new File(inputFiles), outPath);
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

    public static boolean isPencilEngine(byte[] data) {
        if (data == null || data.length < PENCIL_ENGINE.length) {
            return false;
        }

        return Arrays.mismatch(data, 0, PENCIL_ENGINE.length, PENCIL_ENGINE, 0, PENCIL_ENGINE.length) == -1;
    }

    public static boolean isFiniteCoordinate(float value) {
        return Float.isFinite(value) && -100000 < value && value < 100000;
    }

    static void exportArchive(File archiveFile, String outDir) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        List<JhinotePage.JhinotePageCustomPageContent> pageData = new ArrayList<>();
        Map<String, String> backgroundMap = new HashMap<>();
        List<Page> pages = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            zipFile.entries().asIterator().forEachRemaining(entry -> {
                // extract zip file
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    byte[] bytes = inputStream.readAllBytes();
                    System.out.println("Adding " + entry.getName() + " with content " + bytes);
                    files.put(entry.getName(), bytes);
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

            pageData.sort(Comparator.comparing(JhinotePage.JhinotePageCustomPageContent::pageNumber));

            System.out.println("Page data count: " + pageData.size());

            for (JhinotePage.JhinotePageCustomPageContent data : pageData) {
                Page page = buildPage(data, files, backgroundMap);

                System.out.println("Built page: " + page.name() + " with strokes: " + page.strokes().size() + "\n");

                if (!page.strokes().isEmpty() /*|| !page.images().isEmpty() || gridSpec(page.getBackgroundTemplate())*/) {
                    try {
                        String title = archiveFile.getName() + ": " + page.name();
                        Path svgFile = Path.of(outDir).resolve(page.name() + ".svg");

                        System.out.println("Writing file to: " + svgFile.toAbsolutePath());

                        String svgContent = buildSvg(page.strokes(), page.width(), page.height());
                        Files.writeString(svgFile, svgContent, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        System.err.println("Error while writing SVG file: " + page.name());
                        System.err.println(e.getMessage());
                    }
                    pages.add(page);
                }
            }
        } catch (IOException e) {
            System.err.println("Error while processing zip archive: " + archiveFile.getName());
            System.err.println(e.getMessage());
        }
    }

    static Page buildPage(JhinotePage.JhinotePageCustomPageContent pageData, Map<String, byte[]> files, Map<String, String> backgroundMap) {
        Double ratio = pageData.pageRatio();
        if (ratio == null) ratio = 0.706;
        System.out.println("Page ratio: " + ratio);

        Double width = 1000D;
        Double height;
        if (pageData.pageOrientation() == 1) {
            height = 1000.0 * ratio;
        } else {
            height = 1000.0 / ratio;
        }

        List<ImageElement> images = new ArrayList<>();
        if (pageData.bkgAttachmentId() != null
                && !pageData.bkgAttachmentId().isBlank()
                && backgroundMap != null && !backgroundMap.isEmpty()
                && backgroundMap.containsKey(pageData.bkgAttachmentId())
        ) {
            String backgroundName = backgroundMap.get(pageData.bkgAttachmentId());
            byte[] backgroundData = files.get(backgroundName);

            if (backgroundData != null && backgroundData.length > 0) {
                System.out.println("Processing background data for attachment: " + backgroundName);
                // process background data such as image or pdf here
            }

            // process text or image elements here
        }

        List<Stroke> strokes = new ArrayList<>();
        for (JhinotePage.JhinotePageCustomPageContent.Attachment attachment : pageData.attachment()) {
            String result = "files/" + attachment.filePath().substring(attachment.filePath().lastIndexOf("/") + 1);

            System.out.println("Processing attachment: " + result);


            byte[] data = files.get(result);
            System.out.println(files.values().toArray()[0]);
            System.out.println("Retrieved this data for an attachment: " + data);

            if (data != null && data.length > 0 && isPencilEngine(data)) {
                System.out.println("Parsing pencil engine data for attachment: " + result);

                strokes.addAll(parsePencilEngine(data));
            }
        }

        return new Page(
                "page-" + pageData.pageNumber(),
                width.floatValue(),
                height.floatValue(),
                strokes
        );
    }

    static List<Stroke> parsePencilEngine(byte[] data) {
        if (!isPencilEngine(data)) return new ArrayList<>();

        List<Stroke> strokes = new ArrayList<>();

        for (int offset = 60; offset < data.length - 16; offset += 4) {
            long tablePrefix = ExtractionHelper.readUint(data, offset);
            long count = ExtractionHelper.readUint(data, offset + 4);
            // stride means how big one point is like size of x + size of y + size of pressure
            long stride = ExtractionHelper.readUint(data, offset + 8);
            long reserved = ExtractionHelper.readUint(data, offset + 12);
            int pointsStart = offset + 16;
            long pointsEnd = pointsStart + count * stride;

            if (!((tablePrefix == 0 || tablePrefix == 2) && (2 <= count && count <= 16384) && stride == POINT_STRIDE && reserved == 0)) {
                continue;
            }
            if (pointsEnd > data.length) continue;

            List<Point> points = new ArrayList<>();
            List<Float> pressures = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                long pointOffset = pointsStart + i * stride;
                float x = ExtractionHelper.readFloat(data, (int) pointOffset + 4);
                float y = ExtractionHelper.readFloat(data, (int) pointOffset + 8);
                float pressure = ExtractionHelper.readFloat(data, (int) pointOffset + 16);

                if (!isFiniteCoordinate(x) || !isFiniteCoordinate(y)) {
                    points = new ArrayList<>();
                    break;
                }

                points.add(new Point(x, y));
                pressures.add((Float.isFinite(pressure) && pressure > 0) ? pressure : 0.0f);
            }

            if (points.size() < 2) continue;

            // check if table is header inside metadata
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            for (Point p : points) {
                if (p.x() < minX) minX = p.x();
                if (p.x() > maxX) maxX = p.x();
                if (p.y() < minY) minY = p.y();
                if (p.y() > maxY) maxY = p.y();
            }

            float xSpan = maxX - minX;
            float ySpan = maxY - minY;

            if (xSpan == 0.0f && ySpan == 0.0f) {
                continue;
            }

            /*
                if any(pressures):
                    first_pressure = next(pressure for pressure in pressures if pressure > 0)
                    for index, pressure in enumerate(pressures):
                        if pressure == 0:
                            pressures[index] = first_pressure
                        else:
                            first_pressure = pressure
                else:
                    pressures = [0.2] * len(points)
             */

            int styleOffest = offset - 60;
            float baseWidth = ExtractionHelper.readFloat(data, styleOffest + 40);
            if (!Float.isFinite(baseWidth) || baseWidth <= 0 || baseWidth > 100) baseWidth = 4.0f;

            long penType = ExtractionHelper.readUint(data, styleOffest + 12);
            if (!(penType == 1 || penType == 2 || penType == 3 || penType == 5)) continue;

            float softness = ExtractionHelper.readFloat(data, styleOffest + 32);
            long colorValue = ExtractionHelper.readUint(data, styleOffest + 8);

            StrokeStyle strokeStyle = getStrokeStyle(data, styleOffest, colorValue, penType, softness);

            strokes.add(new Stroke(points, pressures, baseWidth, strokeStyle.color(), strokeStyle.opacity(), penType));
        }

        return strokes;
    }

    record JhinoteRoot(JhinoteRootContent customNoteContent) {
        record JhinoteRootContent(List<JhinoteAttachment> attachment) {
            record JhinoteAttachment(String id, String filePath) {
            }
        }
    }
}
