package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.imports.rdf4j.ImportRdf4jHelper;
import fr.cnrs.opentheso.services.imports.rdf4j.ReadRDF4JNewGen;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ConceptSkosRdfImportEngine {

    private final ImportRdf4jHelper importRdf4jHelper;

    public SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException {
        return new ReadRDF4JNewGen().readRdfFlux(inputStream, format, lang, errorBuffer);
    }

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        importRdf4jHelper.setInfos(dateFormat, userId, groupId, sourceLang);
        importRdf4jHelper.setNodePreference(preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        importRdf4jHelper.setRdf4jThesaurus(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId) throws IOException {
        importRdf4jHelper.addConcept(resource, thesaurusId, false);
    }
}
