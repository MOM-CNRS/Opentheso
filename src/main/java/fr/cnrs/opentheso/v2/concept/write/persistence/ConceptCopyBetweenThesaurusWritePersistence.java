package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.CopyBranchBetweenThesaurusCommand;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Copie SKOS d'une branche entre thésaurus — équivalent V2 de {@code CopyAndPasteBetweenThesoService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptCopyBetweenThesaurusWritePersistence {

    private static final String NO_SELECTION = "Aucune sélection !";

    private final ConceptSkosExportPersistence conceptSkosExportPersistence;
    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    private final BranchConceptSupport branchConceptSupport;
    private final ConceptCreationWriteRepository conceptCreationWriteRepository;
    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final PreferencesRepository preferencesRepository;

    @Transactional(readOnly = true)
    public MutationResult validateIdsAvailable(String targetThesaurusId, List<String> conceptIds) {
        return doValidateIdsAvailable(targetThesaurusId, conceptIds);
    }

    private MutationResult doValidateIdsAvailable(String targetThesaurusId, List<String> conceptIds) {
        if (StringUtils.isBlank(targetThesaurusId) || conceptIds == null || conceptIds.isEmpty()) {
            return MutationResult.validationError(NO_SELECTION);
        }
        for (String conceptId : conceptIds) {
            if (conceptCreationWriteRepository.existsConcept(targetThesaurusId, conceptId)) {
                return MutationResult.validationError(
                        "L'identifiant " + conceptId + " existe déjà dans le thésaurus cible, c'est interdit !!! ");
            }
        }
        return MutationResult.ok("OK");
    }

    private static MutationResult validateCopyCommand(CopyBranchBetweenThesaurusCommand command) {
        if (command == null
                || StringUtils.isAnyBlank(command.sourceThesaurusId(), command.sourceConceptId(),
                command.targetThesaurusId())) {
            return MutationResult.validationError(NO_SELECTION);
        }
        if (command.sourceThesaurusId().equalsIgnoreCase(command.targetThesaurusId())) {
            return MutationResult.validationError("Action non permise !!!");
        }
        if (!command.dropToRoot() && StringUtils.isBlank(command.targetParentConceptId())) {
            return MutationResult.validationError(NO_SELECTION);
        }
        return null;
    }

    @Transactional
    public MutationResult copyBranch(CopyBranchBetweenThesaurusCommand command) {
        MutationResult invalid = validateCopyCommand(command);
        if (invalid != null) {
            return invalid;
        }

        List<String> branchIds = branchConceptSupport.collectBranchConceptIds(
                command.sourceThesaurusId(), command.sourceConceptId());
        MutationResult clash = doValidateIdsAvailable(command.targetThesaurusId(), branchIds);
        if (!clash.success()) {
            return clash;
        }

        Preferences targetPrefs = preferencesRepository.findByIdThesaurus(command.targetThesaurusId())
                .orElse(null);
        if (targetPrefs == null) {
            return MutationResult.failure("Préférences du thésaurus cible introuvables");
        }

        SKOSXmlDocument document;
        try {
            document = buildBranchDocument(command.sourceThesaurusId(), command.sourceConceptId(), branchIds);
        } catch (Exception ex) {
            log.error("Export SKOS branche {} / {}", command.sourceThesaurusId(), command.sourceConceptId(), ex);
            return MutationResult.failure("Erreur lors de l'export de la branche");
        }
        if (document == null || document.getConceptList() == null || document.getConceptList().isEmpty()) {
            return MutationResult.failure("Branche vide ou non exportable");
        }

        String identifierType = StringUtils.defaultIfBlank(command.identifierType(), "sans");
        try {
            importBranch(document, command.targetThesaurusId(), command.userId(), targetPrefs, identifierType);
        } catch (Exception ex) {
            log.error("Import SKOS branche vers {}", command.targetThesaurusId(), ex);
            return MutationResult.failure("Erreur lors de l'import de la branche");
        }

        if ("ark".equalsIgnoreCase(identifierType)) {
            clearSourceArks(command.sourceThesaurusId(), document.getConceptList());
        }

        String headId = command.sourceConceptId();
        List<String> bts = new ArrayList<>(
                conceptRelationWriteRepository.listBroaderParentConceptIds(headId, command.targetThesaurusId()));
        for (String bt : bts) {
            conceptRelationWriteRepository.deleteBroaderRelation(
                    headId, bt, command.targetThesaurusId(), command.userId());
        }

        if (command.dropToRoot()) {
            if (!conceptLifecycleWriteRepository.setTopConcept(command.targetThesaurusId(), headId, true)) {
                return MutationResult.failure(
                        "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
            }
        } else {
            conceptRelationWriteRepository.addBroaderRelation(
                    headId, command.targetParentConceptId(), command.targetThesaurusId(), command.userId());
            if (conceptLifecycleWriteRepository.isTopConcept(command.targetThesaurusId(), headId)) {
                conceptLifecycleWriteRepository.setTopConcept(command.targetThesaurusId(), headId, false);
            }
        }

        return MutationResult.ok("Branche copiée avec succès");
    }

    private SKOSXmlDocument buildBranchDocument(
            String sourceThesaurusId,
            String sourceConceptId,
            List<String> branchIds
    ) throws Exception {
        Preferences sourcePrefs = conceptSkosExportPersistence.findThesaurusPreferences(sourceThesaurusId)
                .orElse(null);
        if (sourcePrefs == null) {
            return null;
        }
        SKOSXmlDocument document = new SKOSXmlDocument();
        document.setConceptScheme(conceptSkosExportPersistence.exportConceptScheme(sourceThesaurusId, sourcePrefs));
        for (String conceptId : branchIds) {
            document.addconcept(conceptSkosExportPersistence.exportConcept(sourceThesaurusId, conceptId));
        }
        // garder la tête en tête de liste pour clarté
        if (!branchIds.isEmpty() && !sourceConceptId.equals(branchIds.get(0))) {
            // no reorder needed — rewire uses sourceConceptId
        }
        return document;
    }

    private void importBranch(
            SKOSXmlDocument document,
            String targetThesaurusId,
            int userId,
            Preferences preferences,
            String identifierType
    ) throws Exception {
        thesaurusEditionSkosImportEngine.setInfos("yyyy-MM-dd", userId, -1, "");
        thesaurusEditionSkosImportEngine.setSelectedIdentifier(identifierType);
        thesaurusEditionSkosImportEngine.setPrefixHandle("");
        thesaurusEditionSkosImportEngine.setNodePreference(preferences);
        thesaurusEditionSkosImportEngine.setRdf4jThesaurus(document);
        for (SKOSResource resource : document.getConceptList()) {
            if (resource.getLabelsList() != null && !resource.getLabelsList().isEmpty()) {
                thesaurusEditionSkosImportEngine.addConceptV2(resource, targetThesaurusId);
            }
        }
    }

    private void clearSourceArks(String sourceThesaurusId, List<SKOSResource> resources) {
        for (SKOSResource resource : resources) {
            if (StringUtils.isNotBlank(resource.getArkId()) && StringUtils.isNotBlank(resource.getIdentifier())) {
                try {
                    // Via repository concept — clear ark on source after transfer of ark id
                    conceptLifecycleWriteRepository.clearArkId(sourceThesaurusId, resource.getIdentifier());
                } catch (RuntimeException ex) {
                    log.warn("Impossible de vider l'ARK source de {}", resource.getIdentifier(), ex);
                }
            }
        }
    }
}
