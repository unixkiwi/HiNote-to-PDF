package de.unixkiwi.hinoteconverter.models;

public record ImageElement(byte[] data, String mimeType, Double x, Double y, Double width, Double height,
                           Double angle) {
}