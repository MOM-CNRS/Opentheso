package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class ConceptSkosRdfExportEngine {

    private final ConceptSkosExportPersistence conceptSkosExportPersistence;

    public Optional<Preferences> findThesaurusPreferences(String thesaurusId) {
        return conceptSkosExportPersistence.findThesaurusPreferences(thesaurusId);
    }

    public void prepareExport(Preferences preferences) {
        // Preferences are passed explicitly to export methods.
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId) {
        List<SKOSResource> resources = exportConcepts(
                thesaurusId,
                conceptId == null ? List.of() : List.of(conceptId),
                null
        );
        return resources.isEmpty() ? null : resources.get(0);
    }

    public List<SKOSResource> exportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress
    ) {
        return exportConcepts(thesaurusId, conceptIds, progress, false);
    }

    public List<SKOSResource> exportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress,
            boolean clearHtml
    ) {
        try {
            return conceptSkosExportPersistence.exportConcepts(thesaurusId, conceptIds, progress, clearHtml);
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS concept impossible", ex);
        }
    }

    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return conceptSkosExportPersistence.exportConceptScheme(thesaurusId, preferences);
    }

    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        return conceptSkosExportPersistence.serializeSkos(document, format);
    }
}
