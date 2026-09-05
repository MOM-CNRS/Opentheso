package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentImport;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAlignmentCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotValidationResult;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.workshop.io.WorkshopCsvReader;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;

/**
 * Actions par lot — Alignements (import / suppression / export CSV).
 * Réutilise {@link WorkshopCsvReader} et {@link WorkshopBulkImportPersistence} (logique V2 / legacy).
 */
@Service
@RequiredArgsConstructor
public class ActionsLotAlignmentService {

    private static final String LOCAL_ID = "localId";

    public static final String TEMPLATE_IMPORT = """
            localId,source
            26678/crtcg26jeN4R9,https://www.wikidata.org/wiki/Q65955##1
            """;

    public static final String TEMPLATE_DELETE = """
            localId,URI
            26678/crtcg26jeN4R9,https://www.wikidata.org/wiki/Q65955
            """;

    private final WorkshopBulkImportPersistence persistence;
    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public ActionsLotValidationResult validateImport(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<NodeAlignmentImport> imports;
        try {
            ArrayList<String> headers;
            try (Reader headerReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                headers = reader.readHeadersFileAlignment(headerReader);
            }
            if (headers == null || headers.isEmpty()) {
                return ActionsLotValidationResult.failure(StringUtils.defaultIfBlank(
                        reader.getMessage(),
                        "En-têtes CSV introuvables. Attendu : localId + colonnes de sources."
                ));
            }
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileAlignment(bodyReader, headers)) {
                    return ActionsLotValidationResult.failure(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Lecture CSV impossible. Vérifiez le séparateur."
                    ));
                }
            }
            imports = reader.getNodeAlignmentImports();
        } catch (Exception ex) {
            return ActionsLotValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (imports == null || imports.isEmpty()) {
            return ActionsLotValidationResult.failure(StringUtils.defaultIfBlank(
                    reader.getMessage(),
                    "Aucune ligne d'alignement lue. Vérifiez le séparateur et les en-têtes."
            ));
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotAlignmentCandidate> valid = new ArrayList<>();
        int ignored = 0;
        int line = 1; // header

        Set<String> localIds = new HashSet<>();
        for (NodeAlignmentImport row : imports) {
            if (row != null && StringUtils.isNotBlank(row.getLocalId())) {
                localIds.add(row.getLocalId().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        for (NodeAlignmentImport row : imports) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.trimToEmpty(row.getLocalId());
            if (StringUtils.isBlank(localId)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", LOCAL_ID, "Identifiant obligatoire manquant"));
                continue;
            }
            String conceptId = resolved.get(localId);
            if (StringUtils.isBlank(conceptId)) {
                errors.add(new ActionsLotLineError(
                        line,
                        localId,
                        LOCAL_ID,
                        "Identifiant introuvable dans le thésaurus"
                ));
                continue;
            }
            List<NodeAlignmentSmall> alignments = row.getNodeAlignmentSmalls();
            if (alignments == null || alignments.isEmpty()) {
                errors.add(new ActionsLotLineError(line, localId, "URI", "Aucune URI d'alignement sur cette ligne"));
                continue;
            }
            boolean anyUri = false;
            for (NodeAlignmentSmall alignment : alignments) {
                if (alignment == null || StringUtils.isBlank(alignment.getUri_target())) {
                    continue;
                }
                anyUri = true;
                valid.add(new ActionsLotAlignmentCandidate(
                        line,
                        localId,
                        conceptId,
                        alignment.getUri_target().trim(),
                        StringUtils.defaultString(alignment.getSource()),
                        alignment.getAlignement_id_type() > 0 ? alignment.getAlignement_id_type() : 1
                ));
            }
            if (!anyUri) {
                errors.add(new ActionsLotLineError(line, localId, "URI", "URI cible vide"));
            }
        }

        return new ActionsLotValidationResult(
                true,
                null,
                imports.size(),
                valid.size(),
                errors.size(),
                ignored,
                errors,
                valid
        );
    }

    public ActionsLotValidationResult validateDelete(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<ThesaurusCsvConceptObject> rows;
        try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            if (!reader.readFileAlignmentToDelete(bodyReader)) {
                return ActionsLotValidationResult.failure(StringUtils.defaultIfBlank(
                        reader.getMessage(),
                        "Lecture CSV impossible. Vérifiez le séparateur (colonnes localId, URI)."
                ));
            }
            rows = reader.getConceptObjects();
        } catch (Exception ex) {
            return ActionsLotValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotValidationResult.failure("Aucune ligne lue. Vérifiez le séparateur et les en-têtes localId / URI.");
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotAlignmentCandidate> valid = new ArrayList<>();
        int ignored = 0;
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (ThesaurusCsvConceptObject row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getLocalId())) {
                localIds.add(row.getLocalId().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        for (ThesaurusCsvConceptObject row : rows) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.trimToEmpty(row.getLocalId());
            if (StringUtils.isBlank(localId)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", LOCAL_ID, "Identifiant obligatoire manquant"));
                continue;
            }
            String conceptId = resolved.get(localId);
            if (StringUtils.isBlank(conceptId)) {
                // règle legacy / maquette : ligne ignorée si l'id n'existe pas
                ignored++;
                continue;
            }
            List<NodeIdValue> alignments = row.getAlignments();
            if (alignments == null || alignments.isEmpty()) {
                errors.add(new ActionsLotLineError(line, localId, "URI", "URI manquante"));
                continue;
            }
            boolean anyUri = false;
            for (NodeIdValue alignment : alignments) {
                if (alignment == null || StringUtils.isBlank(alignment.getValue())) {
                    continue;
                }
                anyUri = true;
                valid.add(new ActionsLotAlignmentCandidate(
                        line,
                        localId,
                        conceptId,
                        alignment.getValue().trim(),
                        "",
                        0
                ));
            }
            if (!anyUri) {
                errors.add(new ActionsLotLineError(line, localId, "URI", "URI manquante"));
            }
        }

        return new ActionsLotValidationResult(
                true,
                null,
                rows.size(),
                valid.size(),
                errors.size(),
                ignored,
                errors,
                valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotAlignmentCandidate> candidates,
            String thesaurusId,
            int userId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_VALID_LINE);
        }

        int applied = 0;
        int rejected = 0;
        NodeAlignment nodeAlignment = new NodeAlignment();
        for (ActionsLotAlignmentCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.uri())) {
                rejected++;
                continue;
            }
            nodeAlignment.setId_author(userId);
            nodeAlignment.setConcept_target("");
            nodeAlignment.setThesaurus_target(candidate.source());
            nodeAlignment.setInternal_id_concept(candidate.conceptId());
            nodeAlignment.setInternal_id_thesaurus(thesaurusId);
            nodeAlignment.setAlignement_id_type(candidate.alignmentTypeId() > 0 ? candidate.alignmentTypeId() : 1);
            nodeAlignment.setUri_target(candidate.uri());
            if (persistence.addNewAlignment(nodeAlignment)) {
                applied++;
            } else {
                // déjà présent = non modifié (comportement legacy)
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " alignement(s) ajouté(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    @Transactional
    public ActionsLotApplyResult applyDelete(
            List<ActionsLotAlignmentCandidate> candidates,
            String thesaurusId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à supprimer.");
        }

        int applied = 0;
        int rejected = 0;
        for (ActionsLotAlignmentCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.uri())) {
                rejected++;
                continue;
            }
            if (persistence.deleteAlignmentByUri(candidate.uri().trim(), candidate.conceptId(), thesaurusId)) {
                applied++;
            } else {
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Suppression terminée : " + applied + " alignement(s) supprimé(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public byte[] exportAlignments(String thesaurusId, String alignmentSource, String branchConceptId) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalArgumentException(ActionsLotMessages.NO_THESAURUS);
        }
        if (StringUtils.isBlank(alignmentSource)) {
            throw new IllegalArgumentException("Le nom de la source est obligatoire.");
        }

        ArrayList<NodeIdValue> listAlignments = collectExportAlignments(
                thesaurusId, resolveExportBranchIds(thesaurusId, branchConceptId));

        byte[] csv = thesaurusCsvWriter.writeCsvForAlignment(listAlignments, alignmentSource.trim());
        if (csv == null) {
            throw new IllegalStateException("Échec de la génération du CSV.");
        }
        return csv;
    }

    private List<String> resolveExportBranchIds(String thesaurusId, String branchConceptId) {
        if (StringUtils.isBlank(branchConceptId)) {
            return persistence.getAllIdConceptOfThesaurus(thesaurusId);
        }
        if (!persistence.isIdExiste(branchConceptId.trim(), thesaurusId)) {
            throw new IllegalArgumentException("L'identifiant de concept n'existe pas.");
        }
        return persistence.getIdsOfBranch(branchConceptId.trim(), thesaurusId);
    }

    private ArrayList<NodeIdValue> collectExportAlignments(String thesaurusId, List<String> branchIds) {
        ArrayList<NodeIdValue> listAlignments = new ArrayList<>();
        if (branchIds == null) {
            return listAlignments;
        }
        for (String idConcept : branchIds) {
            List<NodeAlignmentSmall> alignments = persistence.getAllAlignmentsOfConcept(idConcept, thesaurusId);
            if (alignments == null || alignments.isEmpty()) {
                continue;
            }
            for (NodeAlignmentSmall alignment : alignments) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                nodeIdValue.setId(idConcept);
                nodeIdValue.setValue(alignment.getUri_target());
                listAlignments.add(nodeIdValue);
            }
        }
        return listAlignments;
    }

    public byte[] importTemplateBytes() {
        return TEMPLATE_IMPORT.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] deleteTemplateBytes() {
        return TEMPLATE_DELETE.getBytes(StandardCharsets.UTF_8);
    }
}
