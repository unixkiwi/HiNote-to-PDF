package de.unixkiwi.hinoteconverter;

import de.unixkiwi.hinoteconverter.models.*;
import de.unixkiwi.hinoteconverter.models.Point;
import de.unixkiwi.hinoteconverter.models.Stroke;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import tools.jackson.databind.ObjectMapper;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
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

                System.out.println("Built page: " + page.getName() + " with strokes: " + page.getStrokes().size() + "\n");

                if (!page.getStrokes().isEmpty() /*|| !page.getImages().isEmpty() || gridSpec(page.getBackgroundTemplate())*/) {
                    String title = archiveFile.getName().replace(" ", "_") + "-" + page.getName();

                    Renderer r = new Renderer(page);

                    SVGGraphics2D svgG2d = new SVGGraphics2D((int) page.getWidth(), (int) page.getHeight());
                    r.buildGraphic().render(svgG2d);
                    String xmlContent = svgG2d.getSVGElement();
                    try (FileWriter writer = new FileWriter(outDir + File.separatorChar + title + ".svg")) {
                        writer.write(xmlContent);
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

        double width = 1000D;
        double height;
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
                // process background data such as image or pdf here
            }

            // process text or image elements here
        }

        List<Stroke> strokes = new ArrayList<>();
        for (JhinotePage.JhinotePageCustomPageContent.Attachment attachment : pageData.attachment()) {
            String result = "files/" + attachment.filePath().substring(attachment.filePath().lastIndexOf("/") + 1);


            byte[] data = files.get(result);
            if (data != null && data.length > 0 && isPencilEngine(data)) {
                System.out.println("Parsing pencil engine data for attachment: " + result);

                strokes.addAll(parsePencilEngine(data));
            }
        }

        return new Page(
                "page-" + pageData.pageNumber(),
                (float) width,
                (float) height,
                strokes,
                PageOrientation.fromValue(pageData.pageOrientation()),
                PageBackground.fromValue(pageData.background())
        );
    }

    static Color getStrokeColor(int value) {
        if (value == 0 || value == 0xFFFFFFFF) return new Color(0, 0, 0);
        return new Color((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF);
    }

    static StrokeStyle getStrokeStyle(byte[] data, long styleOffset, int colorValue, PenType penType, float softness) {
        if (colorValue != 0 && colorValue != 0xFFFFFFFF) return new StrokeStyle(getStrokeColor(colorValue), 1.0f);

        float c1 = ExtractionHelper.readFloat(data, (int) (styleOffset + 20));
        float c2 = ExtractionHelper.readFloat(data, (int) (styleOffset + 24));
        float c3 = ExtractionHelper.readFloat(data, (int) (styleOffset + 28));

        boolean allValid = Float.isFinite(c1) && c1 >= 0 && c1 <= 1
                && Float.isFinite(c2) && c2 >= 0 && c2 <= 1
                && Float.isFinite(c3) && c3 >= 0 && c3 <= 1;

        boolean anyNonZero = (c1 != 0 || c2 != 0 || c3 != 0);

        if (allValid && anyNonZero) {
            int flag = Math.toIntExact(ExtractionHelper.readUint(data, (int) styleOffset + 4));
            boolean isNew = (flag == 0x01000000 || flag == 0x01010000);

            float r, g, b;
            if (penType == PenType.HIGHLIGHTER || !isNew) {
                r = c3;
                g = c2;
                b = c1;
            } else {
                r = c1;
                g = c2;
                b = c3;
            }

            Color color = new Color(
                    Math.round(r * 255),
                    Math.round(g * 255),
                    Math.round(b * 255)
            );

            boolean isSoftnessValid = Float.isFinite(softness) && softness > 0 && softness <= 1;

            if (penType == PenType.HIGHLIGHTER) {
                float finalSoftness = isSoftnessValid ? softness : 0.35f;
                return new StrokeStyle(color, finalSoftness);
            }
            if (penType == PenType.PENCIL_HB && isSoftnessValid) {
                return new StrokeStyle(color, softness);
            }
            return new StrokeStyle(color, 1.0f);
        }

        return new StrokeStyle(new Color(0, 0, 0), 1.0f);
    }

    static List<Stroke> parsePencilEngine(byte[] data) {
        if (!isPencilEngine(data)) return new ArrayList<>();
        //Set<Integer> penTypes = new HashSet<>();
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

            PenType penType = PenType.fromValue((int) ExtractionHelper.readUint(data, styleOffest + 12));
            if (penType == PenType.OTHER) continue;
//            if (penTypes.add((int) ExtractionHelper.readUint(data, styleOffest + 12)))
//                System.out.println("Pen Type: " + ExtractionHelper.readUint(data, styleOffest + 12));

            float softness = ExtractionHelper.readFloat(data, styleOffest + 32);
            long colorValue = ExtractionHelper.readUint(data, styleOffest + 8);

            StrokeStyle strokeStyle = getStrokeStyle(data, styleOffest, (int) colorValue, penType, softness);

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
