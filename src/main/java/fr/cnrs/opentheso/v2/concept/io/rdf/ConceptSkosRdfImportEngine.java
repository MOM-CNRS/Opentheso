package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import fr.cnrs.opentheso.v2.concept.io.rdf.parser.ReadRdf4jDocument;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConceptSkosRdfImportEngine {

    private final ConceptSkosImportPersistence conceptSkosImportPersistence;

    public SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException {
        return new ReadRdf4jDocument().readRdfFlux(inputStream, format, lang, errorBuffer);
    }

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        conceptSkosImportPersistence.configureImport(dateFormat, userId, groupId, sourceLang, preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        conceptSkosImportPersistence.setImportDocument(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId) throws IOException {
        conceptSkosImportPersistence.importConcept(resource, thesaurusId, false);
    }

    public void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException {
        conceptSkosImportPersistence.importConcept(resource, thesaurusId, asCandidate);
    }
}
