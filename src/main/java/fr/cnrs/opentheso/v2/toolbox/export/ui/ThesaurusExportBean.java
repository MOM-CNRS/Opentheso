package fr.cnrs.opentheso.v2.toolbox.export.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.toolbox.export.service.ThesaurusSkosExportService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;

import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named("v2ThesaurusExportBean")
@RequiredArgsConstructor
public class ThesaurusExportBean implements Serializable {

    private final ThesaurusSkosExportService thesaurusSkosExportService;

    private String thesaurusId;
    private String thesaurusTitle;
    private String formatCode = "rdf";

    public void init(String thesaurusId, String thesaurusTitle) {
        this.thesaurusId = thesaurusId;
        this.thesaurusTitle = thesaurusTitle;
        this.formatCode = "rdf";
    }

    /**
     * Mémorise le thésaurus avant le téléchargement (requête ressource PrimeFaces sans ViewRoot).
     */
    public void prepare(String thesaurusId, String thesaurusTitle) {
        this.thesaurusId = thesaurusId;
        this.thesaurusTitle = thesaurusTitle;
    }

    public StreamedContent downloadSkos() {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        try {
            return thesaurusSkosExportService.exportThesaurus(thesaurusId, thesaurusTitle, formatCode);
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Export SKOS impossible");
            return null;
        }
    }
}
