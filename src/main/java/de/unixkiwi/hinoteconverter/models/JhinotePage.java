package de.unixkiwi.hinoteconverter.models;

public record JhinotePage(JhinotePageCustomPageContent customNotePageContent) {
    public record JhinotePageCustomPageContent(Integer pageNumber, Double pageRatio, Integer pageOrientation,
                                               Integer bkgAttachmentIndex, String background, Integer pageColor,
                                               String bkgAttachmentId) {

    }
}