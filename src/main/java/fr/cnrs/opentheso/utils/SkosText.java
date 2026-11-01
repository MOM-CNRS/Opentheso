package fr.cnrs.opentheso.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Texte passé aux procédures d'import et aux légendes d'image.
 * {@code ##} et {@code @@} découpent les lots SQL ; {@code null} et les sources
 * d'alignement ({@code Wikidata_sparql}, {@code Wikidata_rest}) ne sont pas des légendes.
 */
public final class SkosText {

    private SkosText() {
    }

    public static String imageCaptionPart(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (!text.contains(" · ")) {
            return imageCaptionToken(text);
        }
        StringBuilder kept = new StringBuilder();
        for (String part : text.split(" · ")) {
            String clean = imageCaptionToken(part);
            if (clean.isEmpty()) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append(" · ");
            }
            kept.append(clean);
        }
        return kept.toString();
    }

    private static String imageCaptionToken(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (text.isEmpty() || "null".equalsIgnoreCase(text) || text.matches("(?i).+_(sparql|rest)")) {
            return "";
        }
        return text;
    }

    /**
     * Les séparateurs {@code ##} et {@code @@} découpent le lot passé à
     * {@code opentheso_add_notes}. Un texte qui les contient était ignoré.
     */
    public static String notePayloadPart(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return "";
        }
        return text.replace("##", "# #").replace("@@", "@ @");
    }

    public static String noteLanguage(String language) {
        String text = StringUtils.trimToEmpty(language);
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return "";
        }
        return text;
    }
}
