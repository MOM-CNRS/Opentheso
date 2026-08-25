package fr.cnrs.opentheso.v2.toolbox.actions.service;

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

/**
 * Actions par lot — Formes alternatives (altLabels) : import / suppression CSV.
 */
@Service
@RequiredArgsConstructor
public class ActionsLotAltLabelService {

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
            return ActionsLotAltLabelValidationResult.failure("Aucun fichier à valider.");
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotAltLabelValidationResult.failure("Aucun thésaurus sélectionné.");
        }

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<WorkshopCsvReader.ConceptObject> rows;
        try {
            try (Reader headerReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.setLangs(headerReader)) {
                    return ActionsLotAltLabelValidationResult.failure(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Aucune colonne de langue détectée. Attendu : skos:altLabel@fr, skos:altLabel@en…"
                    ));
                }
            }
            reader.setConceptObjects(new ArrayList<>());
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileAltlabel(bodyReader)) {
                    return ActionsLotAltLabelValidationResult.failure(StringUtils.defaultIfBlank(
                            reader.getMessage(),
                            "Lecture CSV impossible. Vérifiez le séparateur."
                    ));
                }
            }
            rows = reader.getConceptObjects();
        } catch (Exception ex) {
            return ActionsLotAltLabelValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotAltLabelValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, skos:altLabel@xx)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotAltLabelCandidate> valid = new ArrayList<>();
        int ignored = 0;
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (WorkshopCsvReader.ConceptObject row : rows) {
            if (row != null && StringUtils.isNotBlank(row.getIdConcept())) {
                localIds.add(row.getIdConcept().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);
        Map<String, fr.cnrs.opentheso.entites.PreferredTerm> preferredTerms =
                persistence.findPreferredTermsByConceptIds(resolved.values(), thesaurusId);

        for (WorkshopCsvReader.ConceptObject row : rows) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.trimToEmpty(row.getIdConcept());
            if (StringUtils.isBlank(localId)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", "localId", "Identifiant obligatoire manquant"));
                continue;
            }
            String conceptId = resolved.get(localId);
            if (StringUtils.isBlank(conceptId)) {
                if (rejectMissingConcept) {
                    errors.add(new ActionsLotLineError(
                            line, localId, "localId", "Identifiant introuvable dans le thésaurus"
                    ));
                } else {
                    ignored++;
                }
                continue;
            }
            if (!preferredTerms.containsKey(conceptId)) {
                errors.add(new ActionsLotLineError(
                        line, localId, "localId", "Terme préférentiel introuvable pour ce concept"
                ));
                continue;
            }
            List<WorkshopCsvReader.Label> altLabels = row.getAltLabels();
            if (altLabels == null || altLabels.isEmpty()) {
                errors.add(new ActionsLotLineError(line, localId, "skos:altLabel", "Aucun synonyme sur cette ligne"));
                continue;
            }
            boolean any = false;
            for (WorkshopCsvReader.Label altLabel : altLabels) {
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
            if (!any) {
                errors.add(new ActionsLotLineError(line, localId, "skos:altLabel", "Aucun synonyme sur cette ligne"));
            }
        }

        return new ActionsLotAltLabelValidationResult(
                true, null, rows.size(), valid.size(), errors.size(), ignored, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotAltLabelCandidate> candidates,
            String thesaurusId,
            int userId,
            boolean clearBefore
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à importer.");
        }

        Set<String> clearedConcepts = new HashSet<>();
        int applied = 0;
        int rejected = 0;
        var preferredTerms = persistence.findPreferredTermsByConceptIds(
                candidates.stream().map(ActionsLotAltLabelCandidate::conceptId).toList(),
                thesaurusId
        );

        for (ActionsLotAltLabelCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.label())) {
                rejected++;
                continue;
            }
            var preferredTerm = Optional.ofNullable(preferredTerms.get(candidate.conceptId()));
            if (preferredTerm.isEmpty()) {
                rejected++;
                continue;
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
            if (persistence.addNonPreferredTerm(term, userId)) {
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
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
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
            if (candidate == null || StringUtils.isBlank(candidate.label())) {
                rejected++;
                continue;
            }
            var preferredTerm = Optional.ofNullable(preferredTerms.get(candidate.conceptId()));
            if (preferredTerm.isEmpty()) {
                rejected++;
                continue;
            }
            persistence.deleteNonPreferredTerm(
                    preferredTerm.get().getIdTerm(),
                    candidate.lang(),
                    candidate.label(),
                    thesaurusId,
                    userId
            );
            applied++;
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
}
