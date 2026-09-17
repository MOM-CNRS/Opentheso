package fr.cnrs.opentheso.v2.concept.ui;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Écran d'export d'un thésaurus depuis le picker V2.
 * L'UI réutilise le panneau SelectionExport (SKOS / CSV / PDF) côté JS.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusPickerExportBean")
public class ThesaurusPickerExportBean implements Serializable {

    private boolean exportMode;
    private String thesaurusId;
    private String thesaurusTitle;
    private long conceptCount;

    public void open(String thesaurusId, String thesaurusTitle, long conceptCount) {
        if (StringUtils.isBlank(thesaurusId)) {
            exportMode = false;
            return;
        }
        this.thesaurusId = thesaurusId.trim();
        this.thesaurusTitle = StringUtils.defaultIfBlank(thesaurusTitle, this.thesaurusId);
        this.conceptCount = Math.max(0L, conceptCount);
        this.exportMode = true;
    }

    public void cancel() {
        exportMode = false;
        thesaurusId = null;
        thesaurusTitle = null;
        conceptCount = 0L;
    }

    public String getConceptCountLabel() {
        if (conceptCount <= 0) {
            return "";
        }
        return String.format("%,d", conceptCount).replace(',', '\u202f');
    }
}
