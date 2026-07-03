package fr.cnrs.opentheso.v2.candidat.session;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;

public interface CandidatSkosImportLegacySupport {

    SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException;

    void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences);

    void setImportDocument(SKOSXmlDocument document);

    void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException;
}
