package de.unixkiwi.hinoteconverter;

import de.unixkiwi.hinoteconverter.models.*;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final byte[] PENKIT_MAGIC = "PENKITINFENG".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PENCIL_ENGINE = "PENCILENGINE".getBytes(StandardCharsets.UTF_8);
    private static final long POINT_STRIDE = 36;
    // CONSTANTS FOR DRAWING
    private static final Stroke.ColorValue GRID_COLOR = new Stroke.ColorValue(221, 221, 221);
    private static final double GRID_LINE_WIDTH = 0.8;
    private static final double DOT_RADIUS = 1.3;

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

    static boolean isHighlighter(Stroke stroke) {
        return stroke.penType() == 5 || stroke.opacity() < 1.0;
    }

    static String fmt(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    static double strokeWidth(Stroke stroke, double pressure) {
        if (stroke.penType() == 5) {
            return Math.max(0.1, stroke.baseWidth() * 0.8);
        }
        return Math.max(0.1, pressure * stroke.baseWidth() * 0.8);
    }

    static String colorHex(List<Integer> color) {
        return "#" + color.stream()
                .map(channel -> String.format("%02x", channel))
                .collect(Collectors.joining());
    }

    static List<Point> strokeOutline(Stroke stroke) {
        List<PointPressure> samples = new ArrayList<>();

        for (int i = 0; i < stroke.points().size(); i++) {
            samples.add(new PointPressure(
                    stroke.points().get(i),
                    stroke.pressures().get(i)
            ));
        }

        int n = samples.size();
        if (n < 2) {
            return List.of();
        }

        List<Point> tangents = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double dx;
            double dy;

            if (i == 0) {
                dx = samples.get(1).point().x()
                        - samples.get(0).point().x();
                dy = samples.get(1).point().y()
                        - samples.get(0).point().y();
            } else if (i == n - 1) {
                dx = samples.get(n - 1).point().x()
                        - samples.get(n - 2).point().x();
                dy = samples.get(n - 1).point().y()
                        - samples.get(n - 2).point().y();
            } else {
                dx = samples.get(i + 1).point().x()
                        - samples.get(i - 1).point().x();
                dy = samples.get(i + 1).point().y()
                        - samples.get(i - 1).point().y();
            }

            double length = Math.hypot(dx, dy);

            tangents.add(length != 0
                    ? new Point(dx / length, dy / length)
                    : new Point(1.0, 0.0));
        }

        List<Point> left = new ArrayList<>();
        List<Point> right = new ArrayList<>();
        List<Stroke.Point> centers = new ArrayList<>();
        List<Double> radii = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            Point tangent = tangents.get(i);

            double nx = -tangent.y();
            double ny = tangent.x();

            double radius = strokeWidth(
                    stroke,
                    samples.get(i).pressure()
            ) / 2.0;

            Stroke.Point point = samples.get(i).point();

            left.add(new Point(
                    point.x() + nx * radius,
                    point.y() + ny * radius
            ));

            right.add(new Point(
                    point.x() - nx * radius,
                    point.y() - ny * radius
            ));

            centers.add(point);
            radii.add(radius);
        }

        double maxRadius = radii.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);

        double capFloor = maxRadius * 0.45;

        for (int index : new int[]{0, n - 1}) {
            if (radii.get(index) < capFloor) {
                radii.set(index, capFloor);

                Point tangent = tangents.get(index);
                double nx = -tangent.y();
                double ny = tangent.x();

                Stroke.Point point = samples.get(index).point();

                left.set(index, new Point(
                        point.x() + nx * capFloor,
                        point.y() + ny * capFloor
                ));

                right.set(index, new Point(
                        point.x() - nx * capFloor,
                        point.y() - ny * capFloor
                ));
            }
        }

        List<Point> outline = new ArrayList<>(left);

        int steps = 24;

        Stroke.Point endCenter = centers.get(n - 1);
        Point endLeft = left.get(n - 1);

        double a0 = Math.atan2(
                endLeft.y() - endCenter.y(),
                endLeft.x() - endCenter.x()
        );

        for (int step = 1; step <= steps; step++) {
            double angle = a0 - Math.PI * step / steps;

            outline.add(new Point(
                    endCenter.x()
                            + radii.get(n - 1) * Math.cos(angle),
                    endCenter.y()
                            + radii.get(n - 1) * Math.sin(angle)
            ));
        }

        List<Point> reversedRight = new ArrayList<>(right);
        Collections.reverse(reversedRight);
        outline.addAll(reversedRight);

        Stroke.Point startCenter = centers.getFirst();
        Point startRight = right.getFirst();

        a0 = Math.atan2(
                startRight.y() - startCenter.y(),
                startRight.x() - startCenter.x()
        );

        for (int step = 1; step <= steps; step++) {
            double angle = a0 - Math.PI * step / steps;

            outline.add(new Point(
                    startCenter.x()
                            + radii.getFirst() * Math.cos(angle),
                    startCenter.y()
                            + radii.getFirst() * Math.sin(angle)
            ));
        }

        return chaikinSmooth(outline);
    }

    static List<Point> chaikinSmooth(List<Point> points) {
        int m = points.size();

        if (m < 3) {
            return points;
        }

        List<Point> out = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            Point p0 = points.get(i);
            Point p1 = points.get((i + 1) % m);

            out.add(new Point(
                    0.75 * p0.x() + 0.25 * p1.x(),
                    0.75 * p0.y() + 0.25 * p1.y()
            ));

            out.add(new Point(
                    0.25 * p0.x() + 0.75 * p1.x(),
                    0.25 * p0.y() + 0.75 * p1.y()
            ));
        }

        return out;
    }

    static List<String> gridSvg(
            Map<String, Object> spec,
            double width,
            double height
    ) {
        String color = colorHex(List.of(GRID_COLOR.r(), GRID_COLOR.g(), GRID_COLOR.b()));
        List<String> elements = new ArrayList<>();

        String kind = (String) spec.get("type");
        double spacing = ((Number) spec.get("spacing")).doubleValue();
        double x0 = ((Number) spec.getOrDefault("x0", 0)).doubleValue();
        double y0 = ((Number) spec.getOrDefault("y0", 0)).doubleValue();

        if (kind.equals("hlines") || kind.equals("grid")) {
            double y = y0;

            while (y < height) {
                elements.add(
                        "<line x1=\"0\" y1=\"" +
                                fmt(y) +
                                "\" x2=\"" +
                                fmt(width) +
                                "\" y2=\"" +
                                fmt(y) +
                                "\" stroke=\"" +
                                color +
                                "\" stroke-width=\"" +
                                fmt(GRID_LINE_WIDTH) +
                                "\"/>"
                );

                y += spacing;
            }
        }

        if (kind.equals("grid")) {
            double x = x0;

            while (x < width) {
                elements.add(
                        "<line x1=\"" +
                                fmt(x) +
                                "\" y1=\"0\" x2=\"" +
                                fmt(x) +
                                "\" y2=\"" +
                                fmt(height) +
                                "\" stroke=\"" +
                                color +
                                "\" stroke-width=\"" +
                                fmt(GRID_LINE_WIDTH) +
                                "\"/>"
                );

                x += spacing;
            }
        } else if (kind.equals("dots")) {
            double y = y0;

            while (y < height) {
                double x = x0;

                while (x < width) {
                    elements.add(
                            "<circle cx=\"" +
                                    fmt(x) +
                                    "\" cy=\"" +
                                    fmt(y) +
                                    "\" r=\"" +
                                    fmt(DOT_RADIUS) +
                                    "\" fill=\"" +
                                    color +
                                    "\"/>"
                    );

                    x += spacing;
                }

                y += spacing;
            }
        }

        return elements;
    }

    static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    static String buildSvg(List<Stroke> strokes, double width, double height) {
        List<String> paths = new ArrayList<>();

        // Draw highlighters first so opaque strokes appear on top.
        List<Stroke> ordered = strokes.stream()
                .sorted(Comparator.comparingInt(
                        stroke -> isHighlighter(stroke) ? 0 : 1
                ))
                .toList();

        for (Stroke stroke : ordered) {
            List<Point> outline = strokeOutline(stroke);

            if (outline.isEmpty()) {
                continue;
            }

            StringBuilder d = new StringBuilder();

            d.append("M ")
                    .append(fmt(outline.get(0).x()))
                    .append(" ")
                    .append(fmt(outline.get(0).y()));

            for (Point point : outline.subList(1, outline.size())) {
                d.append(" L ")
                        .append(fmt(point.x()))
                        .append(" ")
                        .append(fmt(point.y()));
            }

            d.append(" Z");

            paths.add(
                    "<path d=\"" + d +
                            "\" fill=\"" + colorHex(List.of(stroke.color().r(), stroke.color().g(), stroke.color().b())) +
                            "\" fill-opacity=\"" + fmt(stroke.opacity()) +
                            "\"/>"
            );
        }

        return String.join("\n", List.of(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                        "version=\"1.1\" " +
                        "viewBox=\"0 0 " +
                        fmt(width) + " " +
                        fmt(height) + "\">",

                // White background only.
                "<rect width=\"" + fmt(width) +
                        "\" height=\"" + fmt(height) +
                        "\" fill=\"white\"/>",

                // Strokes only.
                String.join("\n", paths),

                "</svg>"
        ));
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

        if (pageData.pageNumber() == 3) {
            for (Stroke stroke : strokes) {
                System.out.println("Processing stroke for page 3: " + stroke + "\n");
            }
        }

        return new Page(
                "page-" + pageData.pageNumber(),
                width.floatValue(),
                height.floatValue(),
                strokes
        );
    }

    static Stroke.ColorValue getStrokeColor(int value) {
        if (value == 0 || value == 0xFFFFFFFF) {
            return new Stroke.ColorValue(0, 0, 0);
        }

        int r = (value >> 16) & 0xFF;
        int g = (value >> 8) & 0xFF;
        int b = value & 0xFF;

        return new Stroke.ColorValue(r, g, b);
    }

    static StrokeStyle getStrokeStyle(byte[] data, int styleOffset, long colorValue, long penType, float softness) {
        if (colorValue != 0 && colorValue != 0xFFFFFFFFL) {
            return new StrokeStyle(getStrokeColor((int) colorValue), 1.0f);
        }

        float c1 = ExtractionHelper.readFloat(data, styleOffset + 20);
        float c2 = ExtractionHelper.readFloat(data, styleOffset + 24);
        float c3 = ExtractionHelper.readFloat(data, styleOffset + 28);

        boolean allValid = Float.isFinite(c1) && c1 >= 0 && c1 <= 1
                && Float.isFinite(c2) && c2 >= 0 && c2 <= 1
                && Float.isFinite(c3) && c3 >= 0 && c3 <= 1;

        boolean anyNonZero = (c1 != 0 || c2 != 0 || c3 != 0);

        if (allValid && anyNonZero) {
            long flag = ExtractionHelper.readUint(data, styleOffset + 4);
            boolean isNew = (flag == 0x01000000L || flag == 0x01010000L);

            // bgr and rgb for new and old
            float rComp, gComp, bComp;
            if (penType == 5 || !isNew) {
                rComp = c3; // BGR
                gComp = c2;
                bComp = c1;
            } else {
                rComp = c1; // RGB
                gComp = c2;
                bComp = c3;
            }

            Stroke.ColorValue rgb = new Stroke.ColorValue(
                    Math.round(rComp * 255),
                    Math.round(gComp * 255),
                    Math.round(bComp * 255)
            );

            boolean isSoftnessValid = Float.isFinite(softness) && softness > 0 && softness <= 1;

            if (penType == 5) {
                return new StrokeStyle(rgb, isSoftnessValid ? softness : 0.35f);
            }
            if (penType == 3 && isSoftnessValid) {
                return new StrokeStyle(rgb, softness);
            }

            return new StrokeStyle(rgb, 1.0f);
        }

        return new StrokeStyle(new Stroke.ColorValue(0, 0, 0), 1.0f);
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

            List<Stroke.Point> points = new ArrayList<>();
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

                points.add(new Stroke.Point(x, y));
                pressures.add((Float.isFinite(pressure) && pressure > 0) ? pressure : 0.0f);
            }

            if (points.size() < 2) continue;

            // check if table is header inside metadata
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            for (Stroke.Point p : points) {
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

    record Point(double x, double y) {
    }

    record PointPressure(Stroke.Point point, double pressure) {
    }

    record TextLine(
            String text,
            int r,
            int g,
            int b,
            double fontSize
    ) {
    }

    record JhinoteRoot(JhinoteRootContent customNoteContent) {
        record JhinoteRootContent(List<JhinoteAttachment> attachment) {
            record JhinoteAttachment(String id, String filePath) {
            }
        }
    }
}
