package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Post-traitement commun après mutation d'un concept :
 * date de modification, contributeur technique ({@code concept.contributor}),
 * et métadonnées Dublin Core ({@code concept_dcterms}) comme en legacy.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConceptWritePostMutationRepository {

    private final ConceptDcTermRepository conceptDcTermRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void touchConcept(String thesaurusId, String conceptId, int contributorUserId) {
        entityManager.createNativeQuery("""
                        UPDATE concept
                        SET modified = CURRENT_TIMESTAMP,
                            contributor = :contributor
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("contributor", contributorUserId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .executeUpdate();
    }

    /**
     * Enregistre le contributeur Dublin Core (comme {@code DcTermsService} / beans legacy).
     * La PK de {@code concept_dcterms} est (id_concept, id_thesaurus, name, value) :
     * un même contributeur déjà présent est ignoré.
     */
    @Transactional
    public void saveContributorDcTerm(String thesaurusId, String conceptId, String contributorName) {
        saveDcTermIfAbsent(thesaurusId, conceptId, DCMIResource.CONTRIBUTOR, contributorName);
    }

    @Transactional
    public void saveCreatorDcTerm(String thesaurusId, String conceptId, String creatorName) {
        saveDcTermIfAbsent(thesaurusId, conceptId, DCMIResource.CREATOR, creatorName);
    }

    private void saveDcTermIfAbsent(String thesaurusId, String conceptId, String name, String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, name, trimmed)) {
            return;
        }
        try {
            // Même approche que le legacy : ConceptDcTermRepository.save(...)
            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .idConcept(conceptId)
                    .idThesaurus(thesaurusId)
                    .name(name)
                    .value(trimmed)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            // Contributeur / créateur déjà enregistré pour ce concept (PK composite).
            log.debug("DC term déjà présent: {} / {} / {} / {}", thesaurusId, conceptId, name, trimmed);
        }
    }
}
