package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.ExternalImageRepository;
import fr.cnrs.opentheso.repositories.ExternalResourceRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConceptTransferWriteRepository {

    private final ConceptRepository conceptRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final TermRepository termRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NoteRepository noteRepository;
    private final NoteHistoriqueRepository noteHistoriqueRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final ConceptCandidatRepository conceptCandidatRepository;
    private final CandidatStatusRepository candidatStatusRepository;
    private final CandidatMessageRepository candidatMessageRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ConceptTermCandidatRepository conceptTermCandidatRepository;
    private final AlignementRepository alignementRepository;
    private final PropositionRepository propositionRepository;
    private final ConceptReplacedByRepository conceptReplacedByRepository;
    private final GpsRepository gpsRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ExternalResourceRepository externalResourceRepository;
    private final ExternalImageRepository externalImageRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;

    @Transactional
    public boolean moveConceptToAnotherThesaurus(String conceptId, String sourceThesaurusId, String targetThesaurusId) {
        try {
            conceptRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            thesaurusArrayRepository.updateThesaurusByParent(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptHistoriqueRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            termRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            nonPreferredTermRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            noteRepository.updateThesaurusByConcept(conceptId, sourceThesaurusId, targetThesaurusId);
            noteRepository.updateThesaurusByTerm(conceptId, sourceThesaurusId, targetThesaurusId);
            noteHistoriqueRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            preferredTermRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptCandidatRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            candidatStatusRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            candidatMessageRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            candidatVoteRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptGroupConceptRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            hierarchicalRelationshipRepository.updateThesaurusByConcept1(conceptId, sourceThesaurusId, targetThesaurusId);
            hierarchicalRelationshipRepository.updateThesaurusByConcept2(conceptId, sourceThesaurusId, targetThesaurusId);
            hierarchicalRelationshipHistoriqueRepository.updateThesaurusByConcept1(conceptId, sourceThesaurusId, targetThesaurusId);
            hierarchicalRelationshipHistoriqueRepository.updateThesaurusByConcept2(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptTermCandidatRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            alignementRepository.updateInternalThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            propositionRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptReplacedByRepository.updateThesaurusByConcept1(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptReplacedByRepository.updateThesaurusByConcept2(conceptId, sourceThesaurusId, targetThesaurusId);
            gpsRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptFacetRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            externalResourceRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            externalImageRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            conceptDcTermRepository.updateThesaurus(conceptId, sourceThesaurusId, targetThesaurusId);
            return true;
        } catch (Exception exception) {
            log.error("Error while moving concept {}", conceptId, exception);
            return false;
        }
    }
}
