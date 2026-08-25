package de.unixkiwi.hinoteconverter.models;

import java.util.List;

public record JhinotePage(JhinotePageCustomPageContent customNotePageContent) {
    public record JhinotePageCustomPageContent(Integer pageNumber, Double pageRatio, int pageOrientation,
                                               Integer bkgAttachmentIndex, String background, Integer pageColor,
                                               String bkgAttachmentId, List<Attachment> attachment) {
        public record Attachment(String filePath) {
        }
    }
}