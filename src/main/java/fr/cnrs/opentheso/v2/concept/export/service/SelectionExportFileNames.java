package fr.cnrs.opentheso.v2.concept.export.service;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Locale;

final class SelectionExportFileNames {

    private SelectionExportFileNames() {
    }

    static String build(String thesaurusTitle, String thesaurusId, boolean wholeThesaurus, String extension) {
        String base = sanitize(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId));
        if (base.isBlank()) {
            base = "opentheso";
        }
        String scope = wholeThesaurus ? "thesaurus" : "selection";
        String ext = extension == null || extension.isBlank()
                ? ".bin"
                : (extension.startsWith(".") ? extension : "." + extension);
        return base + "_" + scope + "_" + LocalDate.now() + ext;
    }

    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", "_")
                .replaceAll("[^\\p{Alnum}._-]", "");
        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60);
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }
}
