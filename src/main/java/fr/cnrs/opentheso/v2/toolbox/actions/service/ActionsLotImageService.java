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

        char delimiter = CsvDelimiterSupport.resolveDelimiter(choiceDelimiter);
        WorkshopCsvReader reader = new WorkshopCsvReader(delimiter);
        List<ThesaurusCsvConceptObject> rows;
        try {
            try (Reader bodyReader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
                if (!reader.readFileImage(bodyReader)) {
                    return ActionsLotImportValidationResult.failure(
                            "Lecture CSV impossible. Vérifiez le séparateur et les en-têtes (localId, foaf:image)."
                    );
                }
            }
            rows = reader.getConceptObjects();
        } catch (Exception ex) {
            return ActionsLotImportValidationResult.failure("Erreur de lecture : " + ex.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return ActionsLotImportValidationResult.failure(
                    "Aucune ligne lue. Vérifiez le séparateur et les en-têtes (localId, foaf:image)."
            );
        }

        List<ActionsLotLineError> errors = new ArrayList<>();
        List<ActionsLotImageCandidate> valid = new ArrayList<>();
        int line = 1;

        Set<String> localIds = new HashSet<>();
        for (ThesaurusCsvConceptObject row : rows) {
            if (row == null) {
                continue;
            }
            String localId = StringUtils.firstNonBlank(StringUtils.trimToNull(row.getLocalId()), row.getIdConcept());
            if (StringUtils.isNotBlank(localId)) {
                localIds.add(localId.trim());
            }
        }
        Map<String, String> resolved = persistence.resolveConceptIds(localIds, identifierType, thesaurusId);

        for (ThesaurusCsvConceptObject row : rows) {
            line++;
            if (row == null) {
                continue;
            }
            String localId = StringUtils.firstNonBlank(StringUtils.trimToNull(row.getLocalId()), row.getIdConcept());
            localId = StringUtils.trimToEmpty(localId);
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
            int validBefore = valid.size();
            int errorsBefore = errors.size();
            List<NodeImage> images = row.getImages();
            if (images != null) {
                for (NodeImage image : images) {
                    if (image == null || StringUtils.isBlank(image.getUri())) {
                        continue;
                    }
                    if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(image.getUri())) {
                        errors.add(new ActionsLotLineError(line, localId, "foaf:image", "URL non valide : " + image.getUri()));
                        continue;
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
            }
            if (valid.size() == validBefore && errors.size() == errorsBefore) {
                errors.add(new ActionsLotLineError(line, localId, "foaf:image", "Aucune image valide sur cette ligne"));
            }
        }

        return new ActionsLotImportValidationResult<>(
                true, null, rows.size(), valid.size(), errors.size(), 0, errors, valid
        );
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
}
