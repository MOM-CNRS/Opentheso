package fr.cnrs.opentheso.v2.toolbox.edition.session;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public interface ThesaurusEditionSkosImportSupport {

    SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException;

    String importNewThesaurus(
            SKOSXmlDocument document,
            String formatDate,
            int userId,
            Integer projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi,
            Preferences preferences
    ) throws SQLException;

    String getLastErrorMessage();
}
