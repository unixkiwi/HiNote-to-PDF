package de.unixkiwi.hinoteconverter.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RootNote {

    @JsonProperty("customNoteContent")
    private CustomNoteContent noteContent;

    public CustomNoteContent getNoteContent() {
        return noteContent;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomNoteContent {
        @JsonProperty("noteTitle")
        private String title;
        private String background;
        private Long createTime;
        private int cloudSyncStatus;
        private String id;
        private String noteIcon;
        private int noteType;
        private int pageColor;
        private int pageOrientation;
        private Double pageRatio;
        private List<Attachment> attachment;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }

        public int getCloudSyncStatus() {
            return cloudSyncStatus;
        }

        public void setCloudSyncStatus(int cloudSyncStatus) {
            this.cloudSyncStatus = cloudSyncStatus;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNoteIcon() {
            return noteIcon;
        }

        public void setNoteIcon(String noteIcon) {
            this.noteIcon = noteIcon;
        }

        public int getNoteType() {
            return noteType;
        }

        public void setNoteType(int noteType) {
            this.noteType = noteType;
        }

        public int getPageColor() {
            return pageColor;
        }

        public void setPageColor(int pageColor) {
            this.pageColor = pageColor;
        }

        public int getPageOrientation() {
            return pageOrientation;
        }

        public void setPageOrientation(int pageOrientation) {
            this.pageOrientation = pageOrientation;
        }

        public Double getPageRatio() {
            return pageRatio;
        }

        public void setPageRatio(Double pageRatio) {
            this.pageRatio = pageRatio;
        }

        public List<Attachment> getAttachment() {
            return attachment;
        }

        public void setAttachment(List<Attachment> attachment) {
            this.attachment = attachment;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {
        private int attachType;
        private Long createTime;
        private String id;
        private int isDelete;
        private String notesId;

        public int getAttachType() {
            return attachType;
        }

        public void setAttachType(int attachType) {
            this.attachType = attachType;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getIsDelete() {
            return isDelete;
        }

        public void setIsDelete(int isDelete) {
            this.isDelete = isDelete;
        }

        public String getNotesId() {
            return notesId;
        }

        public void setNotesId(String notesId) {
            this.notesId = notesId;
        }
    }
}