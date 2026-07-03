package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptFullAssembler;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptFullReadService {

    public static final int FULL_CONCEPT_STEP = 40;

    private final ConceptFullAssembler conceptFullAssembler;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ApplicationUriService applicationUriService;

    @Transactional(readOnly = true)
    public Optional<ConceptFullSnapshot> loadFullConcept(
            String thesaurusId,
            String conceptId,
            String lang,
            int offset,
            boolean authenticated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Optional.empty();
        }
        var preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, lang);
        String applicationBaseUrl = applicationUriService.resolveApplicationBaseUrl();
        return conceptFullAssembler.assemble(
                thesaurusId,
                conceptId,
                lang,
                offset,
                FULL_CONCEPT_STEP + 1,
                authenticated,
                preferences,
                applicationBaseUrl
        );
    }

    public boolean hasMoreNarrowers(ConceptFullSnapshot fullConcept) {
        return fullConcept != null
                && CollectionUtils.isNotEmpty(fullConcept.getNarrowers())
                && fullConcept.getNarrowers().size() > FULL_CONCEPT_STEP;
    }

    public int nextNarrowerOffset(int currentOffset) {
        return currentOffset + FULL_CONCEPT_STEP + 1;
    }

    @Transactional(readOnly = true)
    public List<ConceptHierarchicalRelation> loadMoreNarrowers(
            String thesaurusId,
            String conceptId,
            String lang,
            int offset,
            boolean authenticated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Collections.emptyList();
        }
        var preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, lang);
        String applicationBaseUrl = applicationUriService.resolveApplicationBaseUrl();
        return conceptFullAssembler.assembleNarrowerRelations(
                thesaurusId,
                conceptId,
                lang,
                offset,
                FULL_CONCEPT_STEP + 1,
                authenticated,
                preferences,
                applicationBaseUrl
        );
    }

    public void appendNarrowers(ConceptFullSnapshot fullConcept, List<ConceptHierarchicalRelation> additional) {
        if (fullConcept == null || CollectionUtils.isEmpty(additional)) {
            return;
        }
        if (fullConcept.getNarrowers() == null) {
            fullConcept.setNarrowers(new ArrayList<>(additional));
            return;
        }
        for (ConceptHierarchicalRelation relation : additional) {
            if (!fullConcept.getNarrowers().contains(relation)) {
                fullConcept.getNarrowers().add(relation);
            }
        }
    }
}
