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

/**
 * Lecture du concept complet avec pagination des termes spécifiques (NT),
 * alignée sur le legacy ({@code step = 40}, probe {@code step + 1}).
 */
@Service
@RequiredArgsConstructor
public class ConceptFullReadService {

    /** Taille de page des NT (legacy {@code ConceptView#step}). */
    public static final int NARROWER_PAGE_SIZE = 40;

    /** @deprecated préférer {@link #NARROWER_PAGE_SIZE} */
    @Deprecated
    public static final int FULL_CONCEPT_STEP = NARROWER_PAGE_SIZE;

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
        return doLoadFullConcept(thesaurusId, conceptId, lang, offset, authenticated, false);
    }

    @Transactional(readOnly = true)
    public Optional<ConceptFullSnapshot> loadFullConcept(
            String thesaurusId,
            String conceptId,
            String lang,
            int offset,
            boolean authenticated,
            boolean includeCandidates
    ) {
        return doLoadFullConcept(thesaurusId, conceptId, lang, offset, authenticated, includeCandidates);
    }

    private Optional<ConceptFullSnapshot> doLoadFullConcept(
            String thesaurusId,
            String conceptId,
            String lang,
            int offset,
            boolean authenticated,
            boolean includeCandidates
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
                pageFetchSize(),
                authenticated,
                preferences,
                applicationBaseUrl,
                includeCandidates
        );
    }

    /**
     * Après un chargement initial (offset 0), indique s'il reste des NT à charger.
     * Le fetch demande {@code pageSize + 1} éléments : s'il y en a plus que {@code pageSize},
     * une page suivante existe.
     */
    public boolean hasMoreNarrowers(ConceptFullSnapshot fullConcept) {
        return hasMoreFromBatch(fullConcept == null ? null : fullConcept.getNarrowers());
    }

    /** Même règle sur le dernier lot ramené par {@link #loadMoreNarrowers}. */
    public boolean hasMoreFromBatch(List<?> batch) {
        return CollectionUtils.isNotEmpty(batch) && batch.size() > NARROWER_PAGE_SIZE;
    }

    public int nextNarrowerOffset(int currentOffset) {
        return currentOffset + pageFetchSize();
    }

    public int pageFetchSize() {
        return NARROWER_PAGE_SIZE + 1;
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
                pageFetchSize(),
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
            boolean alreadyPresent = fullConcept.getNarrowers().stream()
                    .anyMatch(existing -> StringUtils.equals(existing.conceptId(), relation.conceptId()));
            if (!alreadyPresent) {
                fullConcept.getNarrowers().add(relation);
            }
        }
    }
}
