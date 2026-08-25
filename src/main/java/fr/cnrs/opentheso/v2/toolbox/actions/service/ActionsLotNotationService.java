package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNotationCandidate;
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

@Service
@RequiredArgsConstructor
public class ActionsLotNotationService {

    public static final String TEMPLATE = """
            localId,skos:notation
            26678/crtcg26jeN4R9,CBL1
            """;

    private final WorkshopBulkImportPersistence persistence;

    public ActionsLotImportValidationResult<ActionsLotNotationCandidate> validate(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotImportValidationResult.failure("Aucun fichier à valider.");
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotImportValidationResult.failure("Aucun thésaurus sélectionné.");
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<NodeIdValue> rows;
        try {
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileNotation(bodyReader)) {
                    return ActionsLotImportValidationResult.failure(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes (localId, skos:notation)."
                    );
                }
            }
            rows = reader.getNodeIdValues();
        } catch (Exception ex) {
            return ActionsLotImportValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotImportValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, skos:notation)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotNotationCandidate> valid = new ArrayList<>();
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (NodeIdValue row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getId())) {
                localIds.add(row.getId().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        for (NodeIdValue row : rows) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.trimToEmpty(row.getId());
            if (StringUtils.isBlank(localId)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", "localId", "Identifiant obligatoire manquant"));
                continue;
            }
            String conceptId = resolved.get(localId);
            if (StringUtils.isBlank(conceptId)) {
                errors.add(new ActionsLotLineError(
                        line, localId, "localId", "Identifiant introuvable dans le thésaurus"
                ));
                continue;
            }
            String notation = StringUtils.trimToEmpty(row.getValue());
            if (StringUtils.isBlank(notation)) {
                errors.add(new ActionsLotLineError(line, localId, "skos:notation", "Notation obligatoire manquante"));
                continue;
            }
            valid.add(new ActionsLotNotationCandidate(line, localId, conceptId, notation));
        }

        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotNotationCandidate> candidates,
            String thesaurusId,
            boolean clearBefore
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à importer.");
        }

        int applied = 0;
        int rejected = 0;
        var concepts = persistence.findConceptsByIds(
                candidates.stream().map(ActionsLotNotationCandidate::conceptId).toList(),
                thesaurusId
        );

        for (ActionsLotNotationCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.notation())) {
                rejected++;
                continue;
            }
            var concept = concepts.get(candidate.conceptId());
            if (concept == null) {
                rejected++;
                continue;
            }
            if (!clearBefore && StringUtils.isNotEmpty(concept.getNotation())) {
                rejected++;
                continue;
            }
            if (persistence.updateNotation(candidate.conceptId(), thesaurusId, candidate.notation())) {
                applied++;
            } else {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " notation(s) appliquée(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public byte[] templateBytes() {
        return TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }
}
