package de.unixkiwi.hinoteconverter.models;

public enum PageBackground {
    BLANK("base1", 0.0f),
    /**
     * Dot radius is ~1.3-2.6 page units
     * <br>
     * Dot color is something like (221, 221, 221) or (228, 228, 255)
     */
    GRID_DOT("base6", 33f),
    /**
     * Grid line width is 0.8 page units
     * <br>
     * Grid color is something like (221, 221, 221) or (228, 228, 255)
     */
    GRID_MEDIUM("base2", 101f),
    /**
     * Grid line width is 0.8 page units
     * <br>
     * Grid color is something like (221, 221, 221) or (228, 228, 255)
     */
    GRID_SMALL("base3", 58.3f),
    /**
     * Line color is something like (221, 221, 221) or (228, 228, 255)
     */
    RULED_NARROW("base5", 47f),
    /**
     * Line color is something like (221, 221, 221) or (228, 228, 255)
     */
    RULED_WIDE("base4", 75f);

    private String value;
    private float spacing; // in page units

    PageBackground(String value, float spacing) {
        this.value = value;
        this.spacing = spacing;
    }

    public String getValue() {
        return value;
    }

    public float getSpacing() {
        return spacing;
    }
}
