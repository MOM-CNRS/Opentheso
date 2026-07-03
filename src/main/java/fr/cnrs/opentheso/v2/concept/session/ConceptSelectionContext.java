package fr.cnrs.opentheso.v2.concept.session;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * État de sélection concept partagé entre l'écran de consultation et les beans satellites.
 * <p>
 * Synchronisé par {@code ThesaurusBrowseBean} à chaque ouverture ou effacement de concept.
 */
@Getter
@SessionScoped
@Named("v2ConceptSelectionContext")
public class ConceptSelectionContext implements Serializable {

    private String thesaurusId;
    private String conceptId;
    private ConceptSummary summary;

    public void update(String thesaurusId, ConceptDetail detail) {
        if (detail == null || detail.summary() == null) {
            clear();
            return;
        }
        update(thesaurusId, detail.summary().conceptId(), detail.summary());
    }

    public void update(String thesaurusId, String conceptId, ConceptSummary summary) {
        this.thesaurusId = thesaurusId;
        this.conceptId = conceptId;
        this.summary = summary;
    }

    public void clear() {
        thesaurusId = null;
        conceptId = null;
        summary = null;
    }

    public boolean hasSelection() {
        return StringUtils.isNotBlank(conceptId) && summary != null;
    }
}
