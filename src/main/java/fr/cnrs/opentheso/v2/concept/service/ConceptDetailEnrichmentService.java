package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptDetailEnrichmentService {

    private static final int FULL_CONCEPT_OFFSET = 0;

    private final ConceptFullReadService conceptFullReadService;
    private final ConceptCustomRelationReadService conceptCustomRelationReadService;
    private final ConceptCorpusSearchService conceptCorpusSearchService;

    @Transactional(readOnly = true)
    public Optional<ConceptFullSnapshot> loadFullConcept(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean authenticated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Optional.empty();
        }
        return conceptFullReadService.loadFullConcept(
                thesaurusId,
                conceptId,
                lang,
                FULL_CONCEPT_OFFSET,
                authenticated
        );
    }

    public List<ConceptImageItem> mapImages(ConceptFullSnapshot fullConcept) {
        if (fullConcept == null || CollectionUtils.isEmpty(fullConcept.getImages())) {
            return Collections.emptyList();
        }
        return List.copyOf(fullConcept.getImages());
    }

    public List<ConceptGpsPoint> mapGpsPoints(ConceptFullSnapshot fullConcept) {
        if (fullConcept == null || CollectionUtils.isEmpty(fullConcept.getGps())) {
            return Collections.emptyList();
        }
        return List.copyOf(fullConcept.getGps());
    }

    public List<ConceptExternalResourceItem> mapExternalResources(ConceptFullSnapshot fullConcept) {
        if (fullConcept == null || CollectionUtils.isEmpty(fullConcept.getExternalResources())) {
            return Collections.emptyList();
        }
        return List.copyOf(fullConcept.getExternalResources());
    }

    @Transactional(readOnly = true)
    public List<ConceptCustomRelationItem> loadCustomRelations(
            String thesaurusId,
            String conceptId,
            String lang
    ) {
        return conceptCustomRelationReadService.loadCustomRelations(thesaurusId, conceptId, lang);
    }

    public String mapContributors(ConceptFullSnapshot fullConcept) {
        if (fullConcept == null || CollectionUtils.isEmpty(fullConcept.getContributorName())) {
            return "";
        }
        return String.join("; ", fullConcept.getContributorName());
    }

    public ConceptExportIds mapExportIds(ConceptFullSnapshot fullConcept, ThesaurusPreferences preferences) {
        String permanentId = fullConcept != null ? StringUtils.defaultString(fullConcept.getPermanentId()) : "";
        if (preferences == null) {
            return new ConceptExportIds(permanentId, "", "");
        }
        return switch (preferences.exportUriType()) {
            case HANDLE -> new ConceptExportIds("", permanentId, "");
            case DOI -> new ConceptExportIds("", "", permanentId);
            default -> new ConceptExportIds(permanentId, "", "");
        };
    }

    @Transactional(readOnly = true)
    public boolean hasActiveCorpus(String thesaurusId) {
        return conceptCorpusSearchService.hasActiveCorpus(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem> loadCorpusLinks(
            String thesaurusId,
            CorpusSearchContext context
    ) {
        return conceptCorpusSearchService.loadCorpusLinks(thesaurusId, context);
    }

    public record ConceptExportIds(String arkId, String handleId, String doiId) {
    }
}
