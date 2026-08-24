package de.unixkiwi.hinoteconverter.models;

public enum PageOrientation {
    PORTRAIT(0),
    LANDSCAPE(1);

    private final int value;

    PageOrientation(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value == 1 ? "LANDSCAPE" : "PORTRAIT";
    }
}
