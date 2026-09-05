package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptHistoryOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptHistoryReadService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@ViewScoped
@Named("v2ConceptHistoryBean")
@RequiredArgsConstructor
public class ConceptHistoryBean implements Serializable {

    private final transient ConceptHistoryReadService conceptHistoryReadService;

    private ConceptHistoryOverview overview;

    public void load(String thesaurusId, String conceptId, String preferredTermId) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, preferredTermId)) {
            overview = new ConceptHistoryOverview(java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                    java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return;
        }
        overview = conceptHistoryReadService.load(thesaurusId, conceptId, preferredTermId);
    }

    public void reset() {
        overview = null;
    }
}
