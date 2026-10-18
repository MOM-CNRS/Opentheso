package fr.cnrs.opentheso.concept.export.service;

import fr.cnrs.opentheso.shared.time.Dates;
import org.apache.commons.lang3.StringUtils;

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
        String ext;
        if (extension == null || extension.isBlank()) {
            ext = ".bin";
        } else if (extension.startsWith(".")) {
            ext = extension;
        } else {
            ext = "." + extension;
        }
        return base + "_" + scope + "_" + Dates.nowDate() + ext;
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
