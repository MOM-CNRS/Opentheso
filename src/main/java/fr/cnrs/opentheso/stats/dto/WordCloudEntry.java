package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
public class WordCloudEntry {

    private final String label;
    private final long totalVues;
    private final int fontSizePx;

    public WordCloudEntry(String label, long totalVues, int fontSizePx) {
        this.label = label;
        this.totalVues = totalVues;
        this.fontSizePx = fontSizePx;
    }

}