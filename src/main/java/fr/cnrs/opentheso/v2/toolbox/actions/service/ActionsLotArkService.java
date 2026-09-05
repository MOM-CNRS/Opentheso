package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotArkCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActionsLotArkService {

    public static final String TEMPLATE = """
            localId,arkId
            152645,26678/crtcg26jeN4R9
            """;

    private final WorkshopBulkImportPersistence persistence;
    private final ThesaurusMaintenanceService thesaurusMaintenanceService;

    public ActionsLotImportValidationResult<ActionsLotArkCandidate> validate(
            byte[] content,
            int choiceDelimiter,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<NodeIdValue> rows;
        try {
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileArk(bodyReader)) {
                    return ActionsLotImportValidationResult.failure(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes (localId, arkId)."
                    );
                }
            }
            rows = reader.getNodeIdValues();
        } catch (Exception ex) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.READ_ERROR_PREFIX + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotImportValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, arkId)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotArkCandidate> valid = new ArrayList<>();
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (NodeIdValue row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getId())) {
                localIds.add(row.getId().trim());
            }
        }
        Set<String> existing = persistence.findExistingIdSet(localIds, thesaurusId);

        for (NodeIdValue row : rows) {
            line++;
            collectArkRow(row, line, existing, errors, valid);
        }

        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    private static void collectArkRow(
            NodeIdValue row,
            int line,
            Set<String> existing,
            List<ActionsLotLineError> errors,
            List<ActionsLotArkCandidate> valid
    ) {
        if (row == null) {
            return;
        }
        String localId = StringUtils.trimToEmpty(row.getId());
        if (StringUtils.isBlank(localId)) {
            errors.add(new ActionsLotLineError(line, "— (vide)", "localId", "Identifiant obligatoire manquant"));
            return;
        }
        if (!existing.contains(localId)) {
            errors.add(new ActionsLotLineError(
                    line, localId, "localId", "Identifiant introuvable dans le thésaurus"
            ));
            return;
        }
        String arkId = StringUtils.trimToEmpty(row.getValue());
        if (StringUtils.isBlank(arkId)) {
            errors.add(new ActionsLotLineError(line, localId, "arkId", "Identifiant ARK obligatoire manquant"));
            return;
        }
        valid.add(new ActionsLotArkCandidate(line, localId, localId, arkId));
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotArkCandidate> candidates,
            String thesaurusId,
            boolean clearBefore
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_VALID_LINE);
        }

        int applied = 0;
        int rejected = 0;
        var concepts = persistence.findConceptsByIds(
                candidates.stream().map(ActionsLotArkCandidate::conceptId).toList(),
                thesaurusId
        );

        for (ActionsLotArkCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.arkId())) {
                rejected++;
                continue;
            }
            var concept = concepts.get(candidate.conceptId());
            if (concept == null) {
                rejected++;
                continue;
            }
            if (!clearBefore && StringUtils.isNotEmpty(concept.getIdArk())) {
                rejected++;
                continue;
            }
            if (persistence.updateArkIdOfConcept(candidate.conceptId(), thesaurusId, candidate.arkId())) {
                applied++;
            } else {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + ActionsLotMessages.ARK_APPLIED_SUFFIX,
                candidates.size(),
                applied,
                rejected
        );
    }

    public byte[] templateBytes() {
        return TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    public LocalArkSettings loadLocalArkSettings(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return new LocalArkSettings("", "", 0);
        }
        return thesaurusMaintenanceService.loadLocalArkSettings(thesaurusId);
    }

    public ActionsLotApplyResult generateFromConceptId(
            String thesaurusId,
            String prefix,
            String naan,
            boolean overwrite
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (StringUtils.isBlank(naan)) {
            return ActionsLotApplyResult.failure("Le NAAN est obligatoire.");
        }
        int count = thesaurusMaintenanceService.generateArkFromConceptId(thesaurusId, prefix, naan, overwrite);
        return new ActionsLotApplyResult(
                true,
                "Génération terminée : " + count + ActionsLotMessages.ARK_APPLIED_SUFFIX,
                0,
                count,
                0
        );
    }

    public ActionsLotApplyResult generateLocal(String thesaurusId, boolean overwrite) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        int count = thesaurusMaintenanceService.generateLocalArk(thesaurusId, overwrite);
        return new ActionsLotApplyResult(
                true,
                "Génération terminée : " + count + ActionsLotMessages.ARK_APPLIED_SUFFIX,
                0,
                count,
                0
        );
    }
}
