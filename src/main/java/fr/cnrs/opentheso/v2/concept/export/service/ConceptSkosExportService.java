package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class ConceptSkosExportService {

    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;

    @Transactional(readOnly = true)
    public ExportResult exportConcept(String thesaurusId, String conceptId, String formatCode) throws IOException {
        return doExportConcepts(
                thesaurusId,
                StringUtils.isBlank(conceptId) ? List.of() : List.of(conceptId),
                formatCode,
                null
        );
    }

    @Transactional(readOnly = true)
    public ExportResult exportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            String formatCode,
            BiConsumer<Integer, Integer> progress
    ) throws IOException {
        return doExportConcepts(thesaurusId, conceptIds, formatCode, progress);
    }

    private ExportResult doExportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            String formatCode,
            BiConsumer<Integer, Integer> progress
    ) throws IOException {
        SKOSXmlDocument document = doBuildDocument(thesaurusId, conceptIds, progress, false);
        return serialize(document, thesaurusId, conceptIds, formatCode);
    }

    public ExportResult serialize(
            SKOSXmlDocument document,
            String thesaurusId,
            Collection<String> conceptIds,
            String formatCode
    ) throws IOException {
        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
        List<String> ids = normalizedIds(conceptIds);
        String suffix = ids.size() == 1 ? ids.get(0) : "selection";
        return new ExportResult(
                content,
                thesaurusId + "_" + suffix + format.extension(),
                contentType(formatCode)
        );
    }

    @Transactional(readOnly = true)
    public SKOSXmlDocument buildDocument(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress
    ) {
        return doBuildDocument(thesaurusId, conceptIds, progress, false);
    }

    @Transactional(readOnly = true)
    public SKOSXmlDocument buildDocument(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress,
            boolean clearHtml
    ) {
        return doBuildDocument(thesaurusId, conceptIds, progress, clearHtml);
    }

    private SKOSXmlDocument doBuildDocument(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress,
            boolean clearHtml
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Concept ou thésaurus manquant");
        }
        List<String> ids = normalizedIds(conceptIds);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Concept ou thésaurus manquant");
        }

        Preferences preferences = conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .orElse(null);
        validatePreferences(preferences);

        conceptSkosRdfExportEngine.prepareExport(preferences);
        SKOSXmlDocument document = new SKOSXmlDocument();
        document.setConceptScheme(conceptSkosRdfExportEngine.exportConceptScheme(thesaurusId, preferences));
        List<SKOSResource> resources = conceptSkosRdfExportEngine.exportConcepts(thesaurusId, ids, progress, clearHtml);
        for (SKOSResource resource : resources) {
            if (resource != null) {
                document.addconcept(resource);
            }
        }
        return document;
    }

    public static String contentType(String formatCode) {
        return switch (formatCode == null ? "" : formatCode.toLowerCase()) {
            case "jsonld" -> "application/ld+json";
            case "json" -> "application/json";
            case "turtle" -> "text/turtle";
            case "csv" -> "text/csv";
            default -> "application/rdf+xml";
        };
    }

    private static List<String> normalizedIds(Collection<String> conceptIds) {
        if (conceptIds == null) {
            return List.of();
        }
        return conceptIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    private void validatePreferences(Preferences preferences) {
        if (preferences == null) {
            throw new IllegalStateException("Absence des préférences !");
        }
        if (StringUtils.isEmpty(preferences.getCheminSite())) {
            throw new IllegalStateException("Manque l'URL du site, veuillez paramétrer les préférences du thésaurus!");
        }
        if (StringUtils.isEmpty(preferences.getOriginalUri())) {
            throw new IllegalStateException("Manque l'URL du site, veuillez paramétrer les préférences du thésaurus!");
        }
    }
}
