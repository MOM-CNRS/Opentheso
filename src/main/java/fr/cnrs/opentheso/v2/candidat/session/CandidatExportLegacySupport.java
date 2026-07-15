package fr.cnrs.opentheso.v2.candidat.session;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;

public interface CandidatExportLegacySupport {

    Preferences loadThesaurusPreferences(String thesaurusId);

    SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences);

    SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations);

    byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException;
}
