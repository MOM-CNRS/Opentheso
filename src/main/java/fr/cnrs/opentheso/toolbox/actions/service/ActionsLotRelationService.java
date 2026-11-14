package fr.cnrs.opentheso.toolbox.actions.service;

import fr.cnrs.opentheso.concept.write.persistence.ConceptRelationWriteRepository;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotLineError;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotRelationCandidate;
import fr.cnrs.opentheso.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActionsLotRelationService {

    static final String EXPECTED_COLUMNS = "Colonnes attendues : localId, localId cible.";

    private final WorkshopBulkImportPersistence persistence;
    private final ConceptRelationWriteRepository relations;

    public ActionsLotImportValidationResult<ActionsLotRelationCandidate> validate(
            byte[] content,
            int choiceDelimiter,
            String identifierType,
            String thesaurusId
    ) {
        if (content == null || content.length == 0) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.NO_FILE);
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.NO_THESAURUS);
        }
        List<RawPair> rows;
        try {
            rows = readPairs(content, choiceDelimiter);
        } catch (IllegalArgumentException ex) {
            return ActionsLotImportValidationResult.failure(ex.getMessage());
        } catch (IOException ex) {
            return ActionsLotImportValidationResult.failure(ActionsLotMessages.READ_ERROR_PREFIX + ex.getMessage());
        }
        if (rows.isEmpty()) {
            return ActionsLotImportValidationResult.failure("Aucune ligne lue. " + EXPECTED_COLUMNS);
        }

        Set<String> localIds = new HashSet<>();
        for (RawPair row : rows) {
            if (StringUtils.isNotBlank(row.source())) {
                localIds.add(row.source().trim());
            }
            if (StringUtils.isNotBlank(row.target())) {
                localIds.add(row.target().trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotRelationCandidate> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RawPair row : rows) {
            collect(row, thesaurusId, resolved, seen, errors, valid);
        }
        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotRelationCandidate> candidates,
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
        for (ActionsLotRelationCandidate candidate : candidates) {
            if (candidate == null
                    || StringUtils.isBlank(candidate.sourceConceptId())
                    || StringUtils.isBlank(candidate.targetConceptId())) {
                rejected++;
                continue;
            }
            boolean saved = relations.addRelatedRelation(
                    candidate.sourceConceptId(),
                    candidate.targetConceptId(),
                    thesaurusId,
                    userId
            );
            if (saved) {
                applied++;
            } else {
                rejected++;
            }
        }
        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " association(s) appliquée(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    private void collect(
            RawPair row,
            String thesaurusId,
            Map<String, String> resolved,
            Set<String> seen,
            List<ActionsLotLineError> errors,
            List<ActionsLotRelationCandidate> valid
    ) {
        String source = StringUtils.trimToEmpty(row.source());
        String target = StringUtils.trimToEmpty(row.target());
        if (StringUtils.isBlank(source)) {
            errors.add(new ActionsLotLineError(row.line(), "— (vide)", "localId", "Identifiant obligatoire manquant"));
            return;
        }
        if (StringUtils.isBlank(target)) {
            errors.add(new ActionsLotLineError(row.line(), source, "localId cible", "Identifiant cible obligatoire manquant"));
            return;
        }
        String sourceId = resolved.get(source);
        if (StringUtils.isBlank(sourceId)) {
            errors.add(new ActionsLotLineError(row.line(), source, "localId", "Identifiant introuvable dans le thésaurus"));
            return;
        }
        String targetId = resolved.get(target);
        if (StringUtils.isBlank(targetId)) {
            errors.add(new ActionsLotLineError(
                    row.line(), target, "localId cible", "Identifiant introuvable dans le thésaurus"
            ));
            return;
        }
        if (sourceId.equals(targetId)) {
            errors.add(new ActionsLotLineError(row.line(), source, "localId cible", "Un concept ne peut pas être associé à lui-même"));
            return;
        }
        String pairKey = pairKey(sourceId, targetId);
        if (!seen.add(pairKey)) {
            errors.add(new ActionsLotLineError(row.line(), source, "localId cible", "Association déjà présente dans le fichier"));
            return;
        }
        if (relations.hasRelatedRelation(sourceId, targetId, thesaurusId)
                || relations.hasRelatedRelation(targetId, sourceId, thesaurusId)) {
            errors.add(new ActionsLotLineError(row.line(), source, "localId cible", "Relation déjà existante"));
            return;
        }
        valid.add(new ActionsLotRelationCandidate(row.line(), source, target, sourceId, targetId));
    }

    private static List<RawPair> readPairs(byte[] content, int choiceDelimiter) throws IOException {
        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .build();
        byte[] body = stripBom(content);
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            String sourceHeader = header(parser.getHeaderMap(), "localid", "identifier", "id");
            String targetHeader = header(parser.getHeaderMap(), "localid cible", "skos:related", "related");
            if (sourceHeader == null || targetHeader == null) {
                throw new IllegalArgumentException(EXPECTED_COLUMNS);
            }
            List<RawPair> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new RawPair(
                        (int) record.getRecordNumber() + 1,
                        record.get(sourceHeader),
                        record.get(targetHeader)
                ));
            }
            return rows;
        }
    }

    private static String header(Map<String, Integer> headers, String... names) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (String name : names) {
            for (String header : headers.keySet()) {
                if (header != null && name.equals(header.trim().toLowerCase(Locale.ROOT))) {
                    return header;
                }
            }
        }
        return null;
    }

    private static String pairKey(String left, String right) {
        return left.compareTo(right) < 0 ? left + "\0" + right : right + "\0" + left;
    }

    private static byte[] stripBom(byte[] content) {
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF) {
            byte[] stripped = new byte[content.length - 3];
            System.arraycopy(content, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return content;
    }

    private record RawPair(int line, String source, String target) {
    }
}
