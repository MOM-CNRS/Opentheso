package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImageCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
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
import java.util.Set;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;

@Service
@RequiredArgsConstructor
public class ActionsLotImageService {

    public static final String TEMPLATE = """
            localId,foaf:image
            26678/crtcg26jeN4R9,rdf:about=https://media.fr/image.jpg@@dcterms:rights=Web@@dcterms:title=lait@@dcterms:creator=moi
            """;

    private final WorkshopBulkImportPersistence persistence;

    public ActionsLotImportValidationResult<ActionsLotImageCandidate> validate(
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
        ImageParse parsed = readImageRows(content, choiceDelimiter);
        if (parsed.failure() != null) {
            return parsed.failure();
        }
        return collectImageCandidates(parsed.rows(), identifierType, thesaurusId);
    }

    @Transactional
    public ActionsLotApplyResult applyImport(
            List<ActionsLotImageCandidate> candidates,
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
        Set<String> existingImages = clearBefore
                ? Set.of()
                : persistence.findExistingImageKeys(
                        candidates.stream().map(ActionsLotImageCandidate::conceptId).toList(),
                        thesaurusId
                );
        int applied = 0;
        int rejected = 0;

        for (ActionsLotImageCandidate candidate : candidates) {
            if (applyImageCandidate(candidate, thesaurusId, userId, clearBefore, clearedConcepts, existingImages)) {
                applied++;
            } else {
                rejected++;
            }
        }

        return new ActionsLotApplyResult(
                true,
                "Import terminé : " + applied + " image(s) ajoutée(s).",
                candidates.size(),
                applied,
                rejected
        );
    }

    private boolean applyImageCandidate(
            ActionsLotImageCandidate candidate,
            String thesaurusId,
            int userId,
            boolean clearBefore,
            Set<String> clearedConcepts,
            Set<String> existingImages
    ) {
        if (candidate == null || StringUtils.isBlank(candidate.uri())) {
            return false;
        }
        if (clearBefore && clearedConcepts.add(candidate.conceptId())) {
            persistence.deleteImages(thesaurusId, candidate.conceptId());
        }
        if (!clearBefore && existingImages.contains(
                WorkshopBulkImportPersistence.imageKey(candidate.conceptId(), candidate.uri())
        )) {
            return false;
        }
        try {
            persistence.addExternalImage(
                    candidate.conceptId(),
                    thesaurusId,
                    candidate.title(),
                    candidate.rights(),
                    candidate.uri(),
                    candidate.creator(),
                    userId
            );
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public byte[] templateBytes() {
        return TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    private ImageParse readImageRows(byte[] content, int choiceDelimiter) {
        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        try {
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileImage(bodyReader)) {
                    return ImageParse.fail(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes (localId, foaf:image)."
                    );
                }
            }
            List<ThesaurusCsvConceptObject> rows = reader.getConceptObjects();
            if (rows == null || rows.isEmpty()) {
                return ImageParse.fail(
                        "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, foaf:image)."
                );
            }
            return ImageParse.ok(rows);
        } catch (Exception ex) {
            return ImageParse.fail("Erreur de lecture : " + ex.getMessage());
        }
    }

    private ActionsLotImportValidationResult<ActionsLotImageCandidate> collectImageCandidates(
            List<ThesaurusCsvConceptObject> rows,
            String identifierType,
            String thesaurusId
    ) {
        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotImageCandidate> valid = new ArrayList<>();
        int line = 1;
        Set<String> localIds = new HashSet<>();
        for (ThesaurusCsvConceptObject row : rows) {
            collectImageLocalId(row, localIds);
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);
        for (ThesaurusCsvConceptObject row : rows) {
            line++;
            collectImageRow(row, line, resolved, errors, valid);
        }
        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
    }

    private static void collectImageLocalId(ThesaurusCsvConceptObject row, Set<String> localIds) {
        if (row == null) {
            return;
        }
        String localId = StringUtils.firstNonBlank(StringUtils.trimToNull(row.getLocalId()), row.getIdConcept());
        if (StringUtils.isNotBlank(localId)) {
            localIds.add(localId.trim());
        }
    }

    private static void collectImageRow(
            ThesaurusCsvConceptObject row,
            int line,
            Map<String, String> resolved,
            List<ActionsLotLineError> errors,
            List<ActionsLotImageCandidate> valid
    ) {
        if (row == null) {
            return;
        }
        String localId = StringUtils.trimToEmpty(
                StringUtils.firstNonBlank(StringUtils.trimToNull(row.getLocalId()), row.getIdConcept()));
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
        int validBefore = valid.size();
        int errorsBefore = errors.size();
        appendValidImages(row.getImages(), line, localId, conceptId, errors, valid);
        if (valid.size() == validBefore && errors.size() == errorsBefore) {
            errors.add(new ActionsLotLineError(line, localId, "foaf:image", "Aucune image valide sur cette ligne"));
        }
    }

    private static void appendValidImages(
            List<NodeImage> images,
            int line,
            String localId,
            String conceptId,
            List<ActionsLotLineError> errors,
            List<ActionsLotImageCandidate> valid
    ) {
        if (images == null) {
            return;
        }
        for (NodeImage image : images) {
            appendValidImage(image, line, localId, conceptId, errors, valid);
        }
    }

    private static void appendValidImage(
            NodeImage image,
            int line,
            String localId,
            String conceptId,
            List<ActionsLotLineError> errors,
            List<ActionsLotImageCandidate> valid
    ) {
        if (image == null || StringUtils.isBlank(image.getUri())) {
            return;
        }
        if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(image.getUri())) {
            errors.add(new ActionsLotLineError(line, localId, "foaf:image", "URL non valide : " + image.getUri()));
            return;
        }
        valid.add(new ActionsLotImageCandidate(
                line,
                localId,
                conceptId,
                image.getUri().trim(),
                StringUtils.trimToEmpty(image.getImageName()),
                StringUtils.trimToEmpty(image.getCopyRight()),
                StringUtils.trimToEmpty(image.getCreator())
        ));
    }

    private record ImageParse(List<ThesaurusCsvConceptObject> rows, ActionsLotImportValidationResult<ActionsLotImageCandidate> failure) {
        private static ImageParse ok(List<ThesaurusCsvConceptObject> rows) {
            return new ImageParse(rows, null);
        }

        private static ImageParse fail(String message) {
            return new ImageParse(List.of(), ActionsLotImportValidationResult.failure(message));
        }
    }
}
