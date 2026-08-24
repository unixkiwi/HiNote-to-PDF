package de.unixkiwi.hinoteconverter.models;

import java.util.List;

public class Note {
    private String name;
    private List<Page> pages;


    public Note(List<Page> pages) {
        this.pages = pages;
    }

    public List<Page> getPages() {
        {
        }
        return pages;
    }
}
