package fr.cnrs.opentheso.v2.concept.export.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named("v2ConceptSkosExportBean")
@RequiredArgsConstructor
public class ConceptSkosExportBean implements Serializable {

    private final transient ConceptSkosExportService conceptSkosExportService;

    private String thesaurusId;
    private String conceptId;

    /**
     * Mémorise le concept sélectionné avant le téléchargement.
     * <p>
     * {@code p:fileDownload} déclenche une requête ressource distincte, sans ViewRoot ;
     * un bean {@code @ViewScoped} ne peut donc pas produire le flux. On capture ici les
     * identifiants au clic (requête JSF normale) puis on streame depuis la session.
     */
    public void prepare(String thesaurusId, String conceptId) {
        this.thesaurusId = thesaurusId;
        this.conceptId = conceptId;
    }

    public StreamedContent downloadConcept(String formatCode) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return null;
        }
        try {
            var result = conceptSkosExportService.exportConcept(thesaurusId, conceptId, formatCode);
            return DefaultStreamedContent.builder()
                    .contentType(result.contentType())
                    .name(result.filename())
                    .stream(() -> new ByteArrayInputStream(result.content()))
                    .build();
        } catch (IllegalStateException ex) {
            MessageUtils.showErrorMessage(ex.getMessage());
            return null;
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Export SKOS impossible");
            return null;
        }
    }
}
