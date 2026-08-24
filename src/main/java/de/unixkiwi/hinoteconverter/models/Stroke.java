package de.unixkiwi.hinoteconverter.models;

import java.awt.*;
import java.util.List;

public record Stroke(List<Point> points, List<Float> pressures, float baseWidth, Color color,
                     float opacity, long penType) {
}
