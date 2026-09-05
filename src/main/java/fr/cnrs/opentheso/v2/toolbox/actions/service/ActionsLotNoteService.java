package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNoteCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNoteValidationResult;
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
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;

/**
 * Actions par lot — Notes : import CSV (même moteur que l'atelier legacy).
 */
@Service
@RequiredArgsConstructor
public class ActionsLotNoteService {

    public static final String TEMPLATE = """
            localId,skos:definition@fr,skos:definition@en
            26678/crtcg26jeN4R9,test de définition##autre définition,testing of definition
            """;

    private final WorkshopBulkImportPersistence persistence;

    public ActionsLotNoteValidationResult validate(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotNoteValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotNoteValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<ThesaurusCsvConceptObject> rows;
        try {
            try (Reader headerReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.setLangs(headerReader)) {
                    return ActionsLotNoteValidationResult.failure(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Aucune colonne de langue détectée. Attendu : skos:definition@fr, skos:note@en…"
                    ));
                }
            }
            reader.setConceptObjects(new ArrayList<>());
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileNote(bodyReader)) {
                    return ActionsLotNoteValidationResult.failure(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Lecture CSV impossible. Vérifiez le séparateur."
                    ));
                }
            }
            rows = reader.getConceptObjects();
        } catch (Exception ex) {
            return ActionsLotNoteValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotNoteValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, skos:definition@xx)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotNoteCandidate> valid = new ArrayList<>();
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (ThesaurusCsvConceptObject row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getIdConcept())) {
                localIds.add(row.getIdConcept().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        for (ThesaurusCsvConceptObject row : rows) {
            line++;
            collectNoteRow(row, line, resolved, errors, valid);
        }

        return new ActionsLotNoteValidationResult(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    private void collectNoteRow(
            ThesaurusCsvConceptObject row,
            int line,
            Map<String, String> resolved,
            List<ActionsLotLineError> errors,
            List<ActionsLotNoteCandidate> valid
    ) {
        if (row == null) {
            return;
        }
        String localId = StringUtils.trimToEmpty(row.getIdConcept());
        if (StringUtils.isBlank(localId)) {
            errors.add(new ActionsLotLineError(line, "— (vide)", "localId", "Identifiant obligatoire manquant"));
            return;
        }
        String conceptId = resolved.get(localId);
        if (StringUtils.isBlank(conceptId)) {
            errors.add(new ActionsLotLineError(
                    line, localId, "localId", "Identifiant introuvable dans le thésaurus"
            ));
            return;
        }
        int before = valid.size();
        collect(row.getDefinitions(), "definition", line, localId, conceptId, valid);
        collect(row.getHistoryNotes(), "historyNote", line, localId, conceptId, valid);
        collect(row.getChangeNotes(), "changeNote", line, localId, conceptId, valid);
        collect(row.getEditorialNotes(), "editorialNote", line, localId, conceptId, valid);
        collect(row.getExamples(), "example", line, localId, conceptId, valid);
        collect(row.getNote(), "note", line, localId, conceptId, valid);
        collect(row.getScopeNotes(), "scopeNote", line, localId, conceptId, valid);
        if (valid.size() == before) {
            errors.add(new ActionsLotLineError(line, localId, "skos:note", "Aucune note sur cette ligne"));
        }
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotNoteCandidate> candidates,
            String thesaurusId,
            int userId,
            boolean clearBefore
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_VALID_LINE);
        }

        Set<String> clearedConcepts = new HashSet<>();
        int applied = 0;
        int rejected = 0;

        for (ActionsLotNoteCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.value()) || StringUtils.isBlank(candidate.typeCode())) {
                rejected++;
                continue;
            }
            if (clearBefore && clearedConcepts.add(candidate.conceptId())) {
                persistence.deleteNotes(candidate.conceptId(), thesaurusId);
            }
            if (!clearBefore && persistence.isNoteExist(
                    candidate.conceptId(),
                    thesaurusId,
                    candidate.lang(),
                    candidate.value(),
                    candidate.typeCode()
            )) {
                rejected++;
                continue;
            }
            try {
                persistence.addNote(
                        candidate.conceptId(),
                        candidate.lang(),
                        thesaurusId,
                        candidate.value(),
                        candidate.typeCode(),
                        "import",
                        userId
                );
                applied++;
            } catch (Exception ex) {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " note(s) ajoutée(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public byte[] templateBytes() {
        return TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    private void collect(
            List<ThesaurusCsvConceptLabel> labels,
            String typeCode,
            int line,
            String localId,
            String conceptId,
            List<ActionsLotNoteCandidate> valid
    ) {
        if (labels == null) {
            return;
        }
        for (ThesaurusCsvConceptLabel label : labels) {
            if (label == null || StringUtils.isBlank(label.getLabel())) {
                continue;
            }
            valid.add(new ActionsLotNoteCandidate(
                    line,
                    localId,
                    conceptId,
                    typeCode,
                    StringUtils.defaultIfBlank(label.getLang(), "fr"),
                    label.getLabel().trim()
            ));
        }
    }
}
