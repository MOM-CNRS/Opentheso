package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.AlignementPreferencesRepository;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.repositories.CorpusLinkRepository;
import fr.cnrs.opentheso.repositories.ExternalImageRepository;
import fr.cnrs.opentheso.repositories.ExternalResourceRepository;
import fr.cnrs.opentheso.repositories.GraphViewExportedConceptBranchRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PermutedRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.RoutineMailRepository;
import fr.cnrs.opentheso.repositories.TermCandidatRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusAlignementSourceRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusHomePageRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserRoleOnlyOnRepository;
import fr.cnrs.opentheso.v2.concept.identifier.handle.ConceptHandleConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusLifecyclePersistence {

    private final ThesaurusRepository thesaurusRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final ThesaurusHomePageRepository thesaurusHomePageRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final UserRoleOnlyOnRepository userRoleOnlyOnRepository;
    private final ThesaurusAlignementSourceRepository thesaurusAlignementSourceRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final NodeLabelRepository nodeLabelRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final GraphViewExportedConceptBranchRepository graphViewExportedConceptBranchRepository;
    private final RoutineMailRepository routineMailRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;
    private final TermRepository termRepository;
    private final TermHistoriqueRepository termHistoriqueRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final RelationGroupRepository relationGroupRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final CandidatStatusRepository candidatStatusRepository;
    private final CandidatMessageRepository candidatMessageRepository;
    private final ConceptCandidatRepository conceptCandidatRepository;
    private final ConceptTermCandidatRepository conceptTermCandidatRepository;
    private final TermCandidatRepository termCandidatRepository;
    private final GpsRepository gpsRepository;
    private final AlignementRepository alignementRepository;
    private final AlignementPreferencesRepository alignementPreferencesRepository;
    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final PropositionModificationRepository propositionModificationRepository;
    private final PropositionRepository propositionRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ImagesRepository imagesRepository;
    private final ExternalResourceRepository externalResourceRepository;
    private final ExternalImageRepository externalImageRepository;
    private final NoteRepository noteRepository;
    private final NoteHistoriqueRepository noteHistoriqueRepository;
    private final PermutedRepository permutedRepository;
    private final ConceptReplacedByRepository conceptReplacedByRepository;
    private final CorpusLinkRepository corpusLinkRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final ConceptTypeRepository conceptTypeRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ConceptGroupHistoriqueRepository conceptGroupHistoriqueRepository;
    private final ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptRepository conceptRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ConceptHandleConnectionService conceptHandleConnectionService;

    public void deleteRights(String thesaurusId) {
        userGroupThesaurusRepository.deleteByIdThesaurus(thesaurusId);
    }

    public void deleteAllHandleIds(String thesaurusId, Preferences preferences) {
        if (preferences == null || !preferences.isUseHandle()) {
            return;
        }
        var handleIds = conceptRepository.findAllNonEmptyIdHandleByThesaurus(thesaurusId);
        if (preferences.isUseHandleWithCertificat()) {
            if (!conceptHandleConnectionService.deleteAllIdHandle(handleIds, preferences)) {
                log.error("Erreur pendant la suppression des handles : {}", conceptHandleConnectionService.getMessage());
            }
            return;
        }
        conceptHandleConnectionService.applyNodePreference(preferences);
        conceptHandleConnectionService.connectHandle();
        for (String handleId : handleIds) {
            try {
                conceptHandleConnectionService.deleteHandle(handleId);
            } catch (Exception ex) {
                log.error("Erreur pendant la suppression du handle {}", handleId, ex);
            }
        }
    }

    @Transactional
    public boolean deleteThesaurus(String thesaurusId) {
        if (!toolboxThesaurusPersistence.exists(thesaurusId)) {
            return false;
        }
        thesaurusId = fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurusId);
        thesaurusHomePageRepository.deleteAllByIdTheso(thesaurusId);
        userGroupThesaurusRepository.deleteByIdThesaurus(thesaurusId);
        userRoleOnlyOnRepository.deleteByThesaurusIdThesaurus(thesaurusId);
        thesaurusAlignementSourceRepository.deleteAllByIdThesaurus(thesaurusId);
        thesaurusArrayRepository.deleteAllByIdThesaurus(thesaurusId);
        nodeLabelRepository.deleteAllByIdThesaurus(thesaurusId);
        thesaurusDcTermRepository.deleteAllByIdThesaurus(thesaurusId);
        graphViewExportedConceptBranchRepository.deleteAllByTopConceptThesaurusId(thesaurusId);
        routineMailRepository.deleteAllByIdThesaurus(thesaurusId);
        deleteAllTerms(thesaurusId);
        deleteAllGroups(thesaurusId);
        deleteAllCandidats(thesaurusId);
        gpsRepository.deleteByIdTheso(thesaurusId);
        alignementRepository.deleteByThesaurus(thesaurusId);
        alignementPreferencesRepository.deleteByIdThesaurus(thesaurusId);
        propositionModificationDetailRepository.deleteByIdThesaurus(thesaurusId);
        propositionModificationRepository.deleteAllByIdTheso(thesaurusId);
        propositionRepository.deleteAllByIdThesaurus(thesaurusId);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurus(thesaurusId);
        hierarchicalRelationshipHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
        imagesRepository.deleteByIdThesaurus(thesaurusId);
        externalResourceRepository.deleteAllByIdThesaurus(thesaurusId);
        externalImageRepository.deleteAllByIdThesaurus(thesaurusId);
        noteRepository.deleteAllByIdThesaurus(thesaurusId);
        noteHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
        deleteAllConcepts(thesaurusId);
        thesaurusRepository.deleteById(thesaurusId);
        thesaurusLabelRepository.deleteByIdThesaurus(thesaurusId);
        toolboxPreferencePersistence.deletePreferences(thesaurusId);
        return true;
    }

    @Transactional
    public boolean changeThesaurusId(String oldIdThesaurus, String newIdThesaurus) {
        if (toolboxThesaurusPersistence.exists(newIdThesaurus)) {
            return false;
        }
        newIdThesaurus = fr.cnrs.opentheso.utils.StringUtils.convertString(newIdThesaurus);
        thesaurusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusLabelRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusHomePageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        userGroupThesaurusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        userRoleOnlyOnRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusAlignementSourceRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusArrayRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        nodeLabelRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        thesaurusDcTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        graphViewExportedConceptBranchRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        routineMailRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        updateTermsThesaurusId(newIdThesaurus, oldIdThesaurus);
        updateGroupsThesaurusId(newIdThesaurus, oldIdThesaurus);
        updateCandidatsThesaurusId(newIdThesaurus, oldIdThesaurus);
        toolboxPreferencePersistence.updateThesaurusId(oldIdThesaurus, newIdThesaurus);
        gpsRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        alignementRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        alignementPreferencesRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        propositionRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        propositionModificationRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        hierarchicalRelationshipRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        hierarchicalRelationshipHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        imagesRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        externalResourceRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        externalImageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        noteRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        noteHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        updateConceptsThesaurusId(newIdThesaurus, oldIdThesaurus);
        return true;
    }

    private void deleteAllTerms(String thesaurusId) {
        preferredTermRepository.deleteByIdThesaurus(thesaurusId);
        nonPreferredTermRepository.deleteByIdThesaurus(thesaurusId);
        nonPreferredTermHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
        termRepository.deleteByIdThesaurus(thesaurusId);
        termHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
    }

    private void deleteAllGroups(String thesaurusId) {
        conceptGroupRepository.deleteByIdThesaurus(thesaurusId);
        conceptGroupLabelRepository.deleteByIdThesaurus(thesaurusId);
        relationGroupRepository.deleteByIdThesaurus(thesaurusId);
    }

    private void deleteAllCandidats(String thesaurusId) {
        candidatVoteRepository.deleteAllByIdThesaurus(thesaurusId);
        candidatStatusRepository.deleteAllByIdThesaurus(thesaurusId);
        candidatMessageRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptCandidatRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptTermCandidatRepository.deleteAllByIdThesaurus(thesaurusId);
        termCandidatRepository.deleteAllByIdThesaurus(thesaurusId);
    }

    private void deleteAllConcepts(String thesaurusId) {
        permutedRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptReplacedByRepository.deleteAllByIdThesaurus(thesaurusId);
        corpusLinkRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptDcTermRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptTypeRepository.deleteAllByIdThesaurus(thesaurusId);
        conceptFacetRepository.deleteAllByIdThesaurus(thesaurusId);
        try {
            conceptGroupHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
            conceptGroupLabelHistoriqueRepository.deleteAllByIdThesaurus(thesaurusId);
            conceptGroupConceptRepository.deleteAllByIdThesaurus(thesaurusId);
            conceptRepository.deleteAllByIdThesaurus(thesaurusId);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private void updateTermsThesaurusId(String newIdThesaurus, String oldIdThesaurus) {
        preferredTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        nonPreferredTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        nonPreferredTermHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        termRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        termHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    private void updateGroupsThesaurusId(String newIdThesaurus, String oldIdThesaurus) {
        conceptGroupRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptGroupLabelRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptGroupLabelHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptGroupConceptRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        relationGroupRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptGroupHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    private void updateCandidatsThesaurusId(String newIdThesaurus, String oldIdThesaurus) {
        candidatVoteRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        candidatStatusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        candidatMessageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptTermCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        termCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    private void updateConceptsThesaurusId(String newIdThesaurus, String oldIdThesaurus) {
        permutedRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptReplacedByRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        corpusLinkRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptDcTermRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptTypeRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        conceptFacetRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }
}
