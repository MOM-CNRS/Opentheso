package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.exports.SkosConceptExportOperations;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptSkosRdfExportEngine {

    private final SkosConceptExportOperations skosConceptExportOperations;

    public Optional<Preferences> findThesaurusPreferences(String thesaurusId) {
        return skosConceptExportOperations.findThesaurusPreferences(thesaurusId);
    }

    public void prepareExport(Preferences preferences) {
        skosConceptExportOperations.prepareExport(preferences);
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId) {
        return skosConceptExportOperations.exportConcept(thesaurusId, conceptId);
    }

    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        return skosConceptExportOperations.serializeSkos(document, format);
    }
}
