package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.concept.NodeCompareTheso;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCompareCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotConceptCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotDeprecateCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.workshop.io.WorkshopCsvConceptMapper;
import fr.cnrs.opentheso.v2.toolbox.workshop.io.WorkshopCsvReader;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
public class ActionsLotConceptService {

    public static final String ADD_TEMPLATE = """
            URI,skos:prefLabel@fr,skos:prefLabel@en,skos:definition@fr,skos:broader
            20,France,France_en,Pays de la liberté##fait partie de l'Europe,19
            21,Lyon,Lyon_en,Ville de la gastronomie,20
            """;

    public static final String MERGE_TEMPLATE = """
            identifier,skos:prefLabel@fr,skos:prefLabel@it,skos:altLabel@en,skos:altLabel@fr
            4587,astre,astro,heavenly body##astronomical body,
            """;

    public static final String DEPRECATE_TEMPLATE = """
            deprecated,isReplacedBy,skos:note@fr
            https://ark.frantiq.fr/ark:/26678/crtcg26jeN4R9,https://ark.frantiq.fr/ark:/26678/pcrtbpkL4pLqjd,La bonne localité est « Las Médulas » (31076)
            """;

    public static final String COMPARE_TEMPLATE = """
            skos:prefLabel@fr
            Espagne
            """;

    private final WorkshopBulkImportPersistence persistence;
    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public ActionsLotImportValidationResult<ActionsLotConceptCandidate> validateAdd(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        FullCsvParse parsed = parseFullCsv(content, choiceDelimiter, false, thesaurusId);
        if (parsed.error != null) {
            return ActionsLotImportValidationResult.failure(parsed.error);
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotConceptCandidate> valid = new ArrayList<>();
        int line = 1;

        Set<String> conceptLocalIds = new HashSet<>();
        Set<String> groupLocalIds = new HashSet<>();
        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            if (row == null || StringUtils.isBlank(row.getIdConcept())) {
                continue;
            }
            String type = StringUtils.defaultIfBlank(row.getType(), "skos:concept").toLowerCase();
            if ("skos:collection".equals(type)) {
                groupLocalIds.add(row.getIdConcept().trim());
            } else {
                conceptLocalIds.add(row.getIdConcept().trim());
            }
        }
        Map<String, String> existingConcepts = persistence.resolveConceptIds(conceptLocalIds, identifierType, thesaurusId);
        Set<String> existingGroups = existingGroupLocalIds(groupLocalIds, identifierType, thesaurusId);

        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            line++;
            if (row == null) {
                continue;
            }
            String identifier = StringUtils.trimToEmpty(row.getIdConcept());
            if (StringUtils.isBlank(identifier)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", "URI", "Identifiant obligatoire manquant"));
                continue;
            }
            String type = StringUtils.defaultIfBlank(row.getType(), "skos:concept").toLowerCase();
            if ("skos:collection".equals(type)) {
                if (existingGroups.contains(identifier)) {
                    errors.add(new ActionsLotLineError(
                            line, identifier, "URI", "Collection déjà présente dans le thésaurus"
                    ));
                    continue;
                }
            } else {
                if (existingConcepts.containsKey(identifier)) {
                    errors.add(new ActionsLotLineError(
                            line, identifier, "URI", "Identifiant déjà présent dans le thésaurus"
                    ));
                    continue;
                }
                if (row.getPrefLabels() == null || row.getPrefLabels().isEmpty()) {
                    errors.add(new ActionsLotLineError(
                            line, identifier, "skos:prefLabel", "prefLabel obligatoire manquant"
                    ));
                    continue;
                }
            }
            valid.add(new ActionsLotConceptCandidate(line, identifier, type));
        }
        return new ActionsLotImportValidationResult<>(
                true, null, parsed.rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyAdd(
            List<ActionsLotConceptCandidate> candidates,
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId,
            int userId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à importer.");
        }
        FullCsvParse parsed = parseFullCsv(content, choiceDelimiter, false, thesaurusId);
        if (parsed.error != null) {
            return ActionsLotApplyResult.failure(parsed.error);
        }
        Set<Integer> acceptedLines = new HashSet<>();
        for (ActionsLotConceptCandidate candidate : candidates) {
            if (candidate != null) {
                acceptedLines.add(candidate.line());
            }
        }

        persistence.setFormatDate("yyyy-MM-dd");
        int applied = 0;
        int rejected = 0;
        int line = 1;
        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            line++;
            if (row == null || !acceptedLines.contains(line)) {
                continue;
            }
            String type = StringUtils.defaultIfBlank(row.getType(), "skos:concept").toLowerCase();
            try {
                if ("skos:collection".equals(type)) {
                    if (StringUtils.isBlank(row.getIdConcept())) {
                        row.setIdConcept(null);
                    } else {
                        String groupId = resolveGroupId(row.getIdConcept(), identifierType, thesaurusId);
                        if (StringUtils.isNotBlank(groupId)) {
                            row.setIdConcept(groupId);
                        }
                    }
                    if (StringUtils.isNotBlank(row.getIdConcept()) && persistence.isIdGroupExiste(row.getIdConcept(), thesaurusId)) {
                        rejected++;
                        continue;
                    }
                    if (persistence.addGroup(thesaurusId, WorkshopCsvConceptMapper.toEditionModel(row))) {
                        if (row.getSubGroups() != null) {
                            for (String subGroup : row.getSubGroups()) {
                                if (StringUtils.isNotBlank(subGroup) && StringUtils.isNotBlank(row.getIdConcept())) {
                                    persistence.addSubGroup(row.getIdConcept(), subGroup, thesaurusId);
                                }
                            }
                        }
                        applied++;
                    } else {
                        rejected++;
                    }
                } else {
                    if (StringUtils.isBlank(row.getIdConcept())) {
                        row.setIdConcept(null);
                    } else {
                        String conceptId = resolveConceptId(row.getIdConcept(), identifierType, thesaurusId);
                        if (StringUtils.isNotBlank(conceptId)) {
                            row.setIdConcept(conceptId);
                        }
                    }
                    if (StringUtils.isNotBlank(row.getIdConcept()) && persistence.isIdExiste(row.getIdConcept(), thesaurusId)) {
                        rejected++;
                        continue;
                    }
                    if (persistence.addConceptV2(thesaurusId, WorkshopCsvConceptMapper.toEditionModel(row), userId, "yyyy-MM-dd")) {
                        applied++;
                    } else {
                        rejected++;
                    }
                }
            } catch (Exception ex) {
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " concept(s) / collection(s) ajouté(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public ActionsLotImportValidationResult<ActionsLotConceptCandidate> validateMerge(
            byte[] content,
            int choiceDelimiter,
            String thesaurusId
    ) {
        FullCsvParse parsed = parseFullCsv(content, choiceDelimiter, true, thesaurusId);
        if (parsed.error != null) {
            return ActionsLotImportValidationResult.failure(parsed.error);
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotConceptCandidate> valid = new ArrayList<>();
        int line = 1;
        Set<String> identifiers = new HashSet<>();
        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            if (row != null && StringUtils.isNotBlank(row.getIdConcept())) {
                identifiers.add(row.getIdConcept().trim());
            }
        }
        Set<String> existing = persistence.findExistingIdSet(identifiers, thesaurusId);
        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            line++;
            if (row == null) {
                continue;
            }
            String identifier = StringUtils.trimToEmpty(row.getIdConcept());
            if (StringUtils.isBlank(identifier)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", "identifier", "Identifiant obligatoire manquant"));
                continue;
            }
            if (!existing.contains(identifier)) {
                errors.add(new ActionsLotLineError(
                        line, identifier, "identifier", "Identifiant introuvable dans le thésaurus"
                ));
                continue;
            }
            valid.add(new ActionsLotConceptCandidate(line, identifier, "skos:concept"));
        }
        return new ActionsLotImportValidationResult<>(
                true, null, parsed.rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyMerge(
            List<ActionsLotConceptCandidate> candidates,
            byte[] content,
            int choiceDelimiter,
            String thesaurusId,
            int userId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à importer.");
        }
        FullCsvParse parsed = parseFullCsv(content, choiceDelimiter, true, thesaurusId);
        if (parsed.error != null) {
            return ActionsLotApplyResult.failure(parsed.error);
        }
        Set<Integer> acceptedLines = new HashSet<>();
        for (ActionsLotConceptCandidate candidate : candidates) {
            if (candidate != null) {
                acceptedLines.add(candidate.line());
            }
        }

        int applied = 0;
        int rejected = 0;
        int line = 1;
        String displayName = persistence.getUserDisplayName(userId);
        for (WorkshopCsvReader.ConceptObject row : parsed.rows) {
            line++;
            if (row == null || !acceptedLines.contains(line)) {
                continue;
            }
            try {
                if (persistence.updateConcept(thesaurusId, WorkshopCsvConceptMapper.toEditionModel(row), userId)) {
                    persistence.updateDateOfConcept(thesaurusId, row.getIdConcept(), userId);
                    persistence.save(ConceptDcTerm.builder()
                            .name(DCMIResource.CONTRIBUTOR)
                            .value(displayName)
                            .idConcept(row.getIdConcept())
                            .idThesaurus(thesaurusId)
                            .build());
                    applied++;
                } else {
                    rejected++;
                }
            } catch (Exception ex) {
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Remplacement terminé : " + applied + " concept(s) mis à jour.",
                candidates.size(),
                applied,
                rejected
        );
    }

    public ActionsLotImportValidationResult<ActionsLotDeprecateCandidate> validateDeprecate(
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
        List<NodeDeprecated> rows;
        try {
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileCsvDeprecateConcepts(bodyReader)) {
                    return ActionsLotImportValidationResult.failure(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes (deprecated, isReplacedBy)."
                    );
                }
            }
            rows = reader.getNodeDeprecateds();
        } catch (Exception ex) {
            return ActionsLotImportValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }
        if (rows == null || rows.isEmpty()) {
            return ActionsLotImportValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (deprecated, isReplacedBy)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotDeprecateCandidate> valid = new ArrayList<>();
        int line = 1;
        Set<String> localIds = new HashSet<>();
        for (NodeDeprecated row : rows) {
            if (row == null) {
                continue;
            }
            if (StringUtils.isNotBlank(row.getDeprecatedId())) {
                localIds.add(row.getDeprecatedId().trim());
            }
            if (StringUtils.isNotBlank(row.getReplacedById())) {
                localIds.add(row.getReplacedById().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);
        for (NodeDeprecated row : rows) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.trimToEmpty(row.getDeprecatedId());
            if (StringUtils.isBlank(localId)) {
                errors.add(new ActionsLotLineError(line, "— (vide)", "deprecated", "Identifiant obligatoire manquant"));
                continue;
            }
            String conceptId = resolved.get(localId);
            if (StringUtils.isBlank(conceptId)) {
                errors.add(new ActionsLotLineError(
                        line, localId, "deprecated", "Identifiant introuvable dans le thésaurus"
                ));
                continue;
            }
            String replacedLocal = StringUtils.trimToEmpty(row.getReplacedById());
            String replacedConcept = null;
            if (StringUtils.isNotBlank(replacedLocal)) {
                replacedConcept = resolved.get(replacedLocal);
                if (StringUtils.isBlank(replacedConcept)) {
                    errors.add(new ActionsLotLineError(
                            line, localId, "isReplacedBy", "Concept de remplacement introuvable"
                    ));
                    continue;
                }
            }
            valid.add(new ActionsLotDeprecateCandidate(
                    line,
                    localId,
                    conceptId,
                    replacedLocal,
                    replacedConcept,
                    StringUtils.trimToEmpty(row.getNote()),
                    StringUtils.defaultIfBlank(row.getNoteLang(), "fr")
            ));
        }
        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyDeprecate(
            List<ActionsLotDeprecateCandidate> candidates,
            String thesaurusId,
            int userId
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotApplyResult.failure("Aucun thésaurus sélectionné.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return ActionsLotApplyResult.failure("Aucune ligne valide à importer.");
        }
        int applied = 0;
        int rejected = 0;
        String displayName = persistence.getUserDisplayName(userId);
        for (ActionsLotDeprecateCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.conceptId())) {
                rejected++;
                continue;
            }
            try {
                if (!persistence.deprecateConcept(candidate.conceptId(), thesaurusId, userId)) {
                    rejected++;
                    continue;
                }
                if (StringUtils.isNotBlank(candidate.replacedByConceptId())) {
                    persistence.addReplacedBy(
                            candidate.conceptId(), thesaurusId, candidate.replacedByConceptId(), userId
                    );
                }
                if (StringUtils.isNotBlank(candidate.note())) {
                    if (!persistence.isNoteExist(
                            candidate.conceptId(), thesaurusId, candidate.noteLang(), candidate.note(), "note"
                    )) {
                        persistence.addNote(
                                candidate.conceptId(), candidate.noteLang(), thesaurusId,
                                candidate.note(), "note", "", userId
                        );
                    }
                }
                persistence.updateDateOfConcept(thesaurusId, candidate.conceptId(), userId);
                persistence.save(ConceptDcTerm.builder()
                        .name(DCMIResource.CONTRIBUTOR)
                        .value(displayName)
                        .idConcept(candidate.conceptId())
                        .idThesaurus(thesaurusId)
                        .build());
                applied++;
            } catch (Exception ex) {
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Obsolescence terminée : " + applied + " concept(s) rendu(s) obsolète(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    public ActionsLotImportValidationResult<ActionsLotCompareCandidate> validateCompare(
            byte[] content,
            int choiceDelimiter
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotImportValidationResult.failure("Aucun fichier à valider.");
        }
        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();
            CSVParser parser = format.parse(bodyReader);
            Map<String, Integer> headers = parser.getHeaderMap();
            if (headers == null || headers.size() != 1) {
                return ActionsLotImportValidationResult.failure(
                        "Une seule colonne est autorisée (ex. skos:prefLabel@fr)."
                );
            }
            String header = headers.keySet().iterator().next();
            if (header == null || !header.contains("@")) {
                return ActionsLotImportValidationResult.failure(
                        "La langue doit être précisée, exemple : skos:prefLabel@fr."
                );
            }
            String lang = header.substring(header.indexOf('@') + 1).trim();
            if (StringUtils.isBlank(lang)) {
                return ActionsLotImportValidationResult.failure("La langue n'a pas été trouvée.");
            }

            List<ActionsLotLineError> errors = new ArrayList<>();
            List<ActionsLotCompareCandidate> valid = new ArrayList<>();
            int line = 1;
            int rows = 0;
            for (CSVRecord record : parser) {
                line++;
                rows++;
                String value = record.isMapped(header) ? record.get(header) : null;
                if (StringUtils.isBlank(value)) {
                    errors.add(new ActionsLotLineError(line, "— (vide)", header, "Label obligatoire manquant"));
                    continue;
                }
                valid.add(new ActionsLotCompareCandidate(line, value.trim()));
            }
            if (rows == 0) {
                return ActionsLotImportValidationResult.failure("Aucune ligne lue.");
            }
            return new ActionsLotImportValidationResult<>(
                    true, null, rows, valid.size(), errors.size(), 0, errors, valid, lang
            );
        } catch (Exception ex) {
            return ActionsLotImportValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }
    }

    public byte[] compareToCsv(
            List<ActionsLotCompareCandidate> candidates,
            String thesaurusId,
            String lang,
            String searchType
    ) {
        if (candidates == null || candidates.isEmpty() || StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(lang)) {
            return new byte[0];
        }
        String mode = StringUtils.defaultIfBlank(searchType, "exactWord");
        List<NodeCompareTheso> rows = new ArrayList<>();
        Set<String> hitIds = new HashSet<>();
        for (ActionsLotCompareCandidate candidate : candidates) {
            if (candidate == null || StringUtils.isBlank(candidate.originalPrefLabel())) {
                continue;
            }
            List<NodeSearchMini> hits = switch (mode) {
                case "containsExactWord" -> persistence.searchExactMatch(
                        candidate.originalPrefLabel(), lang, thesaurusId, false
                );
                case "startWith" -> persistence.searchStartWith(
                        candidate.originalPrefLabel(), lang, thesaurusId, false
                );
                case "elastic" -> persistence.searchFullTextElastic(
                        candidate.originalPrefLabel(), lang, thesaurusId, false
                );
                default -> persistence.searchExactTermForAutocompletion(
                        candidate.originalPrefLabel(), lang, thesaurusId
                );
            };
            boolean written = false;
            if (hits != null) {
                for (NodeSearchMini hit : hits) {
                    if (hit == null || !(hit.isConcept() || hit.isAltLabel())) {
                        continue;
                    }
                    written = true;
                    NodeCompareTheso row = new NodeCompareTheso();
                    row.setOriginalPrefLabel(candidate.originalPrefLabel());
                    row.setIdConcept(hit.getIdConcept());
                    row.setPrefLabel(hit.getPrefLabel());
                    row.setAltLabel(hit.getAltLabelValue());
                    if (StringUtils.isNotBlank(hit.getIdConcept())) {
                        hitIds.add(hit.getIdConcept());
                    }
                    rows.add(row);
                }
            }
            if (!written) {
                NodeCompareTheso row = new NodeCompareTheso();
                row.setOriginalPrefLabel(candidate.originalPrefLabel());
                rows.add(row);
            }
        }
        if (!hitIds.isEmpty()) {
            var concepts = persistence.findConceptsByIds(hitIds, thesaurusId);
            for (NodeCompareTheso row : rows) {
                if (row == null || StringUtils.isBlank(row.getIdConcept())) {
                    continue;
                }
                var concept = concepts.get(row.getIdConcept());
                if (concept != null) {
                    row.setIdArk(concept.getIdArk());
                }
            }
        }
        byte[] csv = thesaurusCsvWriter.writeCsvFromNodeCompareTheso(rows, lang);
        return csv == null ? new byte[0] : csv;
    }

    public byte[] addTemplateBytes() {
        return ADD_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] mergeTemplateBytes() {
        return MERGE_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] deprecateTemplateBytes() {
        return DEPRECATE_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] compareTemplateBytes() {
        return COMPARE_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    private FullCsvParse parseFullCsv(byte[] content, int choiceDelimiter, boolean readEmpty, String thesaurusId) {
        if (content == null || content.length == 0) {
            return new FullCsvParse("Aucun fichier à valider.", List.of());
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return new FullCsvParse("Aucun thésaurus sélectionné.", List.of());
        }
        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        try {
            try (Reader langsReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.setLangs(langsReader)) {
                    return new FullCsvParse(
                            "Aucune langue détectée. Utilisez des colonnes du type skos:prefLabel@fr.",
                            List.of()
                    );
                }
            }
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFile(bodyReader, readEmpty)) {
                    return new FullCsvParse(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes.",
                            List.of()
                    );
                }
            }
            List<WorkshopCsvReader.ConceptObject> rows = reader.getConceptObjects();
            if (rows == null || rows.isEmpty()) {
                return new FullCsvParse("Aucune ligne lue. Vérifiez le séparateur et les en-têtes.", List.of());
            }
            return new FullCsvParse(null, rows);
        } catch (Exception ex) {
            return new FullCsvParse("Erreur de lecture : " + ex.getMessage(), List.of());
        }
    }

    private Set<String> existingGroupLocalIds(
            Set<String> localIds,
            String identifierType,
            String thesaurusId
    ) {
        if (localIds == null || localIds.isEmpty()) {
            return Set.of();
        }
        if (!"identifier".equalsIgnoreCase(StringUtils.defaultIfBlank(identifierType, "identifier"))
                && !"ark".equalsIgnoreCase(identifierType)
                && !"handle".equalsIgnoreCase(identifierType)) {
            identifierType = "identifier";
        }
        if ("identifier".equalsIgnoreCase(identifierType) || StringUtils.isBlank(identifierType)) {
            return persistence.findExistingGroupIdSet(localIds, thesaurusId);
        }
        Set<String> existingLocal = new HashSet<>();
        for (String localId : localIds) {
            String groupId = resolveGroupId(localId, identifierType, thesaurusId);
            if (StringUtils.isNotBlank(groupId) && persistence.isIdGroupExiste(groupId, thesaurusId)) {
                existingLocal.add(localId);
            }
        }
        return existingLocal;
    }

    private String resolveConceptId(String localId, String identifierType, String thesaurusId) {
        if ("ark".equalsIgnoreCase(identifierType)) {
            return persistence.getIdConceptFromArkId(localId, thesaurusId);
        }
        if ("handle".equalsIgnoreCase(identifierType)) {
            return persistence.getIdConceptFromHandleId(localId);
        }
        return localId;
    }

    private String resolveGroupId(String localId, String identifierType, String thesaurusId) {
        if ("ark".equalsIgnoreCase(identifierType)) {
            return persistence.getIdGroupFromArkId(localId, thesaurusId);
        }
        if ("handle".equalsIgnoreCase(identifierType)) {
            return persistence.getIdGroupFromHandleId(localId);
        }
        return localId;
    }

    private record FullCsvParse(String error, List<WorkshopCsvReader.ConceptObject> rows) {
    }
}
