package de.unixkiwi.hinoteconverter.models;

import java.util.List;

public record Stroke(List<Point> points, List<Float> pressures, float baseWidth, ColorValue color,
                     float opacity, long penType) {
    public record Point(Float x, Float y) {
    }

    public record ColorValue(Integer r, Integer g, Integer b) {
    }
}
