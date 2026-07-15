package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

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
        try {
            return conceptSkosExportPersistence.exportConcept(thesaurusId, conceptId);
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS concept impossible", ex);
        }
    }

    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        return conceptSkosExportPersistence.serializeSkos(document, format);
    }
}
