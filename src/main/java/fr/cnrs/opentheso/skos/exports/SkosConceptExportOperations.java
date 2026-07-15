package fr.cnrs.opentheso.skos.exports;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.services.exports.rdf4j.ExportRdf4jHelperNew;
import fr.cnrs.opentheso.services.exports.rdf4j.WriteRdf4j;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SkosConceptExportOperations {

    private final ExportRdf4jHelperNew exportRdf4jHelperNew;
    private final PreferencesRepository preferencesRepository;

    public Optional<Preferences> findThesaurusPreferences(String thesaurusId) {
        return preferencesRepository.findByIdThesaurus(thesaurusId);
    }

    public void prepareExport(Preferences preferences) {
        exportRdf4jHelperNew.setInfos(preferences);
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId) {
        return exportRdf4jHelperNew.exportConceptV2(thesaurusId, conceptId, false);
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations) {
        return exportRdf4jHelperNew.exportConceptV2(thesaurusId, conceptId, includeRelations);
    }

    public SKOSResource exportThesaurusScheme(String thesaurusId, Preferences preferences) {
        return exportRdf4jHelperNew.exportThesoV2(thesaurusId, preferences);
    }

    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(new WriteRdf4j(document).getModel(), out, format);
        return out.toByteArray();
    }
}
