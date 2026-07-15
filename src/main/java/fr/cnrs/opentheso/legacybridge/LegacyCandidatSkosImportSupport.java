package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.imports.rdf4j.ImportRdf4jHelper;
import fr.cnrs.opentheso.services.imports.rdf4j.ReadRDF4JNewGen;
import fr.cnrs.opentheso.v2.candidat.session.CandidatSkosImportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class LegacyCandidatSkosImportSupport implements CandidatSkosImportLegacySupport {

    private final ImportRdf4jHelper importRdf4jHelper;

    @Override
    public SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException {
        return new ReadRDF4JNewGen().readRdfFlux(inputStream, format, lang, errorBuffer);
    }

    @Override
    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        importRdf4jHelper.setInfos(dateFormat, userId, groupId, sourceLang);
        importRdf4jHelper.setNodePreference(preferences);
    }

    @Override
    public void setImportDocument(SKOSXmlDocument document) {
        importRdf4jHelper.setRdf4jThesaurus(document);
    }

    @Override
    public void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException {
        importRdf4jHelper.addConcept(resource, thesaurusId, asCandidate);
    }
}
