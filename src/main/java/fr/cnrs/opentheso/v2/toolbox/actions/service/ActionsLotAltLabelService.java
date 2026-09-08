package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
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
import java.util.Optional;
import java.util.Set;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;

/**
 * Actions par lot — Formes alternatives (altLabels) : import / suppression CSV.
 */
@Service
@RequiredArgsConstructor
public class ActionsLotAltLabelService {

    private static final String LOCAL_ID = "localId";

    public static final String TEMPLATE = """
            localId,skos:altLabel@fr,skos:altLabel@en
            26678/crtcg26jeN4R9,synonyme1##synonyme2,altLabel1##altLabel2
            """;

    private final WorkshopBulkImportPersistence persistence;

    public ActionsLotAltLabelValidationResult validate(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId,
            boolean rejectMissingConcept
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotAltLabelValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotAltLabelValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        AltLabelParse parsed = readAltLabelRows(content, choiceDelimiter);
        if (parsed.failure() != null) {
            return parsed.failure();
        }
        return collectAltLabelCandidates(parsed.rows(), identifierType, thesaurusId, rejectMissingConcept);
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotAltLabelCandidate> candidates,
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
        var preferredTerms = persistence.findPreferredTermsByConceptIds(
                candidates.stream().map(ActionsLotAltLabelCandidate::conceptId).toList(),
                thesaurusId
        );

        for (ActionsLotAltLabelCandidate candidate : candidates) {
            if (applyImportCandidate(candidate, thesaurusId, userId, clearBefore, clearedConcepts, preferredTerms)) {
                applied++;
            } else {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " synonyme(s) ajouté(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    @Transactional
    public ActionsLotApplyResult applyDelete(
            List<ActionsLotAltLabelCandidate> candidates,
            String thesaurusId,
            int userId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à supprimer.");
        }

        int applied = 0;
        int rejected = 0;
        var preferredTerms = persistence.findPreferredTermsByConceptIds(
                candidates.stream().map(ActionsLotAltLabelCandidate::conceptId).toList(),
                thesaurusId
        );
        for (ActionsLotAltLabelCandidate candidate : candidates) {
            if (applyDeleteCandidate(candidate, thesaurusId, userId, preferredTerms)) {
                applied++;
            } else {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Suppression terminée : " + applied + " synonyme(s) traité(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public byte[] templateBytes() {
        return TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    private AltLabelParse readAltLabelRows(byte[] content, int choiceDelimiter) {
        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        try {
            try (Reader headerReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.setLangs(headerReader)) {
                    return AltLabelParse.fail(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Aucune colonne de langue détectée. Attendu : skos:altLabel@fr, skos:altLabel@en…"
                    ));
                }
            }
            reader.setConceptObjects(new ArrayList<>());
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileAltlabel(bodyReader)) {
                    return AltLabelParse.fail(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Lecture CSV impossible. Vérifiez le séparateur."
                    ));
                }
            }
            List<ThesaurusCsvConceptObject> rows = reader.getConceptObjects();
            if (rows == null || rows.isEmpty()) {
                return AltLabelParse.fail(
                        "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, skos:altLabel@xx)."
                );
            }
            return AltLabelParse.ok(rows);
        } catch (Exception ex) {
            return AltLabelParse.fail("Erreur de lecture : " + ex.getMessage());
        }
    }

    private ActionsLotAltLabelValidationResult collectAltLabelCandidates(
            List<ThesaurusCsvConceptObject> rows,
            String identifierType,
            String thesaurusId,
            boolean rejectMissingConcept
    ) {
        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotAltLabelCandidate> valid = new ArrayList<>();
        int ignored = 0;
        int line = 1;
        Set<String> localIds = new HashSet<>();
        for (ThesaurusCsvConceptObject row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getIdConcept())) {
                localIds.add(row.getIdConcept().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);
        Map<String, fr.cnrs.opentheso.entites.PreferredTerm> preferredTerms =
                persistence.findPreferredTermsByConceptIds(resolved.values(), thesaurusId);
        for (ThesaurusCsvConceptObject row : rows) {
            line++;
            ignored += collectAltLabelRow(row, line, resolved, preferredTerms, rejectMissingConcept, errors, valid);
        }
        return new ActionsLotAltLabelValidationResult(
                true, null, rows.size(), valid.size(), errors.size(), ignored, errors, valid
        );
    }

    private static int collectAltLabelRow(
            ThesaurusCsvConceptObject row,
            int line,
            Map<String, String> resolved,
            Map<String, fr.cnrs.opentheso.entites.PreferredTerm> preferredTerms,
            boolean rejectMissingConcept,
            List<ActionsLotLineError> errors,
            List<ActionsLotAltLabelCandidate> valid
    ) {
        if (row == null) {
            return 0;
        }
        String localId = StringUtils.trimToEmpty(row.getIdConcept());
        if (StringUtils.isBlank(localId)) {
            errors.add(new ActionsLotLineError(line, "— (vide)", LOCAL_ID, "Identifiant obligatoire manquant"));
            return 0;
        }
        String conceptId = resolved.get(localId);
        if (StringUtils.isBlank(conceptId)) {
            if (rejectMissingConcept) {
                errors.add(new ActionsLotLineError(
                        line, localId, LOCAL_ID, "Identifiant introuvable dans le thésaurus"
                ));
                return 0;
            }
            return 1;
        }
        if (!preferredTerms.containsKey(conceptId)) {
            errors.add(new ActionsLotLineError(
                    line, localId, LOCAL_ID, "Terme préférentiel introuvable pour ce concept"
            ));
            return 0;
        }
        List<ThesaurusCsvConceptLabel> altLabels = row.getAltLabels();
        if (altLabels == null || altLabels.isEmpty()) {
            errors.add(new ActionsLotLineError(line, localId, "skos:altLabel", "Aucun synonyme sur cette ligne"));
            return 0;
        }
        boolean any = appendAltLabels(altLabels, line, localId, conceptId, valid);
        if (!any) {
            errors.add(new ActionsLotLineError(line, localId, "skos:altLabel", "Aucun synonyme sur cette ligne"));
        }
        return 0;
    }

    private static boolean appendAltLabels(
            List<ThesaurusCsvConceptLabel> altLabels,
            int line,
            String localId,
            String conceptId,
            List<ActionsLotAltLabelCandidate> valid
    ) {
        boolean any = false;
        for (ThesaurusCsvConceptLabel altLabel : altLabels) {
            if (altLabel == null || StringUtils.isBlank(altLabel.getLabel())) {
                continue;
            }
            any = true;
            valid.add(new ActionsLotAltLabelCandidate(
                    line,
                    localId,
                    conceptId,
                    altLabel.getLabel().trim(),
                    StringUtils.defaultIfBlank(altLabel.getLang(), "fr")
            ));
        }
        return any;
    }

    private boolean applyImportCandidate(
            ActionsLotAltLabelCandidate candidate,
            String thesaurusId,
            int userId,
            boolean clearBefore,
            Set<String> clearedConcepts,
            Map<String, fr.cnrs.opentheso.entites.PreferredTerm> preferredTerms
    ) {
        if (candidate == null || StringUtils.isBlank(candidate.label())) {
            return false;
        }
        var preferredTerm = Optional.ofNullable(preferredTerms.get(candidate.conceptId()));
        if (preferredTerm.isEmpty()) {
            return false;
        }
        if (clearBefore && clearedConcepts.add(candidate.conceptId())) {
            persistence.deleteAllByConceptAndThesaurus(candidate.conceptId(), thesaurusId);
        }
        Term term = Term.builder()
                .idTerm(preferredTerm.get().getIdTerm())
                .lexicalValue(candidate.label())
                .lang(candidate.lang())
                .idThesaurus(thesaurusId)
                .source("import")
                .status("")
                .hidden(false)
                .build();
        return persistence.addNonPreferredTerm(term, userId);
    }

    private boolean applyDeleteCandidate(
            ActionsLotAltLabelCandidate candidate,
            String thesaurusId,
            int userId,
            Map<String, fr.cnrs.opentheso.entites.PreferredTerm> preferredTerms
    ) {
        if (candidate == null || StringUtils.isBlank(candidate.label())) {
            return false;
        }
        var preferredTerm = Optional.ofNullable(preferredTerms.get(candidate.conceptId()));
        if (preferredTerm.isEmpty()) {
            return false;
        }
        persistence.deleteNonPreferredTerm(
                preferredTerm.get().getIdTerm(),
                candidate.lang(),
                candidate.label(),
                thesaurusId,
                userId
        );
        return true;
    }

    private record AltLabelParse(List<ThesaurusCsvConceptObject> rows, ActionsLotAltLabelValidationResult failure) {
        private static AltLabelParse ok(List<ThesaurusCsvConceptObject> rows) {
            return new AltLabelParse(rows, null);
        }

        private static AltLabelParse fail(String message) {
            return new AltLabelParse(List.of(), ActionsLotAltLabelValidationResult.failure(message));
        }
    }
}
