package de.unixkiwi.hinoteconverter.models;

public enum PenType {
    PEN(1),
    PENCIL(2),
    BRUSH(3),
    HIGHLIGHTER(5),
    OTHER(-1);

    private int value;

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
