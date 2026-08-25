package de.unixkiwi.hinoteconverter.models;

public enum PenType {
    BRUSH(13),
    PEN(2),
    FELT_TIP(12),
    FOUNTAIN(1),
    PENCIL_HB(3),
    PENCIL_2B(11),
    MARKER(4),
    HIGHLIGHTER(5),
    OTHER(-1);

    public final int value;

    PenType(int value) {
        this.value = value;
    }

    public static PenType fromValue(int value) {
        for (PenType penType : PenType.values()) {
            if (penType.value == value) {
                return penType;
            }
        }
        return OTHER;
    }
}
