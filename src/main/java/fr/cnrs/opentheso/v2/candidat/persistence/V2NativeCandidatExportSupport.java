package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.candidat.session.CandidatExportLegacySupport;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatExportSupport implements CandidatExportLegacySupport {

    private final ConceptSkosExportPersistence conceptSkosExportPersistence;

    @Override
    public Preferences loadThesaurusPreferences(String thesaurusId) {
        return conceptSkosExportPersistence.findThesaurusPreferences(thesaurusId).orElse(null);
    }

    @Override
    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return conceptSkosExportPersistence.exportConceptScheme(thesaurusId, preferences);
    }

    @Override
    public SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations) {
        try {
            return conceptSkosExportPersistence.exportConcept(thesaurusId, conceptId, includeRelations);
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS candidat impossible", ex);
        }
    }

    @Override
    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        return conceptSkosExportPersistence.serializeSkos(document, format);
    }
}
