package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.exports.SkosConceptExportOperations;
import fr.cnrs.opentheso.v2.candidat.session.CandidatExportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatExportSupport implements CandidatExportLegacySupport {

    private final SkosConceptExportOperations skosConceptExportOperations;

    @Override
    public Preferences loadThesaurusPreferences(String thesaurusId) {
        return skosConceptExportOperations.findThesaurusPreferences(thesaurusId).orElse(null);
    }

    @Override
    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return skosConceptExportOperations.exportThesaurusScheme(thesaurusId, preferences);
    }

    @Override
    public SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations) {
        return skosConceptExportOperations.exportConcept(thesaurusId, conceptId, includeRelations);
    }

    @Override
    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        return skosConceptExportOperations.serializeSkos(document, format);
    }
}
