package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.exports.rdf4j.ExportRdf4jHelperNew;
import fr.cnrs.opentheso.services.exports.rdf4j.WriteRdf4j;
import fr.cnrs.opentheso.v2.candidat.session.CandidatExportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LegacyCandidatExportSupport implements CandidatExportLegacySupport {

    private final ExportRdf4jHelperNew exportRdf4jHelperNew;
    private final PreferenceService preferenceService;

    @Override
    public Preferences loadThesaurusPreferences(String thesaurusId) {
        return preferenceService.getThesaurusPreferences(thesaurusId);
    }

    @Override
    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return exportRdf4jHelperNew.exportThesoV2(thesaurusId, preferences);
    }

    @Override
    public SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations) {
        return exportRdf4jHelperNew.exportConceptV2(thesaurusId, conceptId, includeRelations);
    }

    @Override
    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(new WriteRdf4j(document).getModel(), out, format);
        return out.toByteArray();
    }
}
