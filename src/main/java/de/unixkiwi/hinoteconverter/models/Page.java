package de.unixkiwi.hinoteconverter.models;

import java.util.List;

public record Page(
        String name,
        float width,
        float height,
        List<Stroke> strokes
) {
}