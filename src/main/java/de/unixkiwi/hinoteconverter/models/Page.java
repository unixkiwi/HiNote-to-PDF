package de.unixkiwi.hinoteconverter.models;

import java.util.List;

public class Page {
    private String name;
    private float width;
    private float height;
    private List<Stroke> strokes;
    private PageOrientation orientation;
    private PageBackground background;
    private String bgAttachmentId;

    public Page(String name, float width, float height, List<Stroke> strokes, PageOrientation orientation, PageBackground background) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.strokes = strokes;
        this.orientation = orientation;
        this.background = background;
        this.bgAttachmentId = null;
    }

    public Page(String name, float width, float height, List<Stroke> strokes, PageOrientation orientation, PageBackground background, String bgAttachmentId) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.strokes = strokes;
        this.orientation = orientation;
        this.background = background;
        this.bgAttachmentId = bgAttachmentId;
    }
}