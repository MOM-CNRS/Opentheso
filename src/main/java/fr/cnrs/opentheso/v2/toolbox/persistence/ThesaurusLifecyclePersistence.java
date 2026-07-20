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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusLifecyclePersistence {

    /**
     * Bulk native deletes (one statement per table) — avoids Spring Data derived deletes
     * that load every entity then delete row-by-row.
     */
    private static final String[] THESAURUS_BULK_DELETE_SQL = {
            "DELETE FROM thesohomepage WHERE idtheso = :id",
            "DELETE FROM user_group_thesaurus WHERE id_thesaurus = :id",
            "DELETE FROM user_role_only_on WHERE id_theso = :id",
            "DELETE FROM thesaurus_alignement_source WHERE id_thesaurus = :id",
            "DELETE FROM thesaurus_array WHERE id_thesaurus = :id",
            "DELETE FROM node_label WHERE id_thesaurus = :id",
            "DELETE FROM thesaurus_dcterms WHERE id_thesaurus = :id",
            "DELETE FROM graph_view_exported_concept_branch WHERE top_concept_thesaurus_id = :id",
            "DELETE FROM routine_mail WHERE id_thesaurus = :id",
            // terms
            "DELETE FROM preferred_term WHERE id_thesaurus = :id",
            "DELETE FROM non_preferred_term WHERE id_thesaurus = :id",
            "DELETE FROM non_preferred_term_historique WHERE id_thesaurus = :id",
            "DELETE FROM term WHERE id_thesaurus = :id",
            "DELETE FROM term_historique WHERE id_thesaurus = :id",
            // groups
            "DELETE FROM concept_group_concept WHERE idthesaurus = :id",
            "DELETE FROM concept_group_label_historique WHERE idthesaurus = :id",
            "DELETE FROM concept_group_label WHERE idthesaurus = :id",
            "DELETE FROM concept_group_historique WHERE idthesaurus = :id",
            "DELETE FROM relation_group WHERE id_thesaurus = :id",
            "DELETE FROM concept_group WHERE idthesaurus = :id",
            // candidats
            "DELETE FROM candidat_vote WHERE id_thesaurus = :id",
            "DELETE FROM candidat_status WHERE id_thesaurus = :id",
            "DELETE FROM candidat_messages WHERE id_thesaurus = :id",
            "DELETE FROM concept_term_candidat WHERE id_thesaurus = :id",
            "DELETE FROM concept_candidat WHERE id_thesaurus = :id",
            "DELETE FROM term_candidat WHERE id_thesaurus = :id",
            // concept-related
            "DELETE FROM gps WHERE id_theso = :id",
            "DELETE FROM alignement WHERE internal_id_thesaurus = :id",
            "DELETE FROM alignement_preferences WHERE id_thesaurus = :id",
            "DELETE FROM proposition_modification_detail WHERE id_proposition IN "
                    + "(SELECT id FROM proposition_modification WHERE id_theso = :id)",
            "DELETE FROM proposition_modification WHERE id_theso = :id",
            "DELETE FROM proposition WHERE id_thesaurus = :id",
            "DELETE FROM hierarchical_relationship WHERE id_thesaurus = :id",
            "DELETE FROM hierarchical_relationship_historique WHERE id_thesaurus = :id",
            "DELETE FROM external_images WHERE id_thesaurus = :id",
            "DELETE FROM external_resources WHERE id_thesaurus = :id",
            "DELETE FROM note WHERE id_thesaurus = :id",
            "DELETE FROM note_historique WHERE id_thesaurus = :id",
            "DELETE FROM permuted WHERE id_thesaurus = :id",
            "DELETE FROM concept_replacedby WHERE id_thesaurus = :id",
            "DELETE FROM corpus_link WHERE id_theso = :id",
            "DELETE FROM concept_dcterms WHERE id_thesaurus = :id",
            "DELETE FROM concept_historique WHERE id_thesaurus = :id",
            "DELETE FROM concept_type WHERE id_theso = :id",
            "DELETE FROM concept_facet WHERE id_thesaurus = :id",
            "DELETE FROM concept WHERE id_thesaurus = :id",
            // thesaurus root
            "DELETE FROM thesaurus_label WHERE id_thesaurus = :id",
            "DELETE FROM preferences WHERE id_thesaurus = :id",
            "DELETE FROM thesaurus WHERE id_thesaurus = :id"
    };

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

    @PersistenceContext
    private EntityManager entityManager;

    public void deleteRights(String thesaurusId) {
        entityManager.createNativeQuery("DELETE FROM user_group_thesaurus WHERE id_thesaurus = :id")
                .setParameter("id", thesaurusId)
                .executeUpdate();
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
        long started = System.currentTimeMillis();
        for (String sql : THESAURUS_BULK_DELETE_SQL) {
            entityManager.createNativeQuery(sql)
                    .setParameter("id", thesaurusId)
                    .executeUpdate();
        }
        entityManager.flush();
        entityManager.clear();
        log.info("Thésaurus {} supprimé en {} ms (bulk SQL)", thesaurusId, System.currentTimeMillis() - started);
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
