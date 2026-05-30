package dev.xerohero.fixiy;

import java.io.File;

public class SearchResult {
    private final File file;
    private final int lineNumber;
    private final String lineContent;
    private final String relativePath;

    public SearchResult(File file, int lineNumber, String lineContent, String relativePath) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
        this.relativePath = relativePath;
    }

    public File getFile() { return file; }

    @Override
    public String toString() {
        return String.format("%s [Line %d]: %s", relativePath, lineNumber, lineContent.trim());
    }
}