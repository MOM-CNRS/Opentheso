package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.imports.SkosConceptImportOperations;
import fr.cnrs.opentheso.v2.candidat.session.CandidatSkosImportLegacySupport;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfImportEngine;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatSkosImportSupport implements CandidatSkosImportLegacySupport {

    private final ConceptSkosRdfImportEngine conceptSkosRdfImportEngine;
    private final SkosConceptImportOperations skosConceptImportOperations;

    @Override
    public SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException {
        return conceptSkosRdfImportEngine.readSkos(inputStream, format, lang, errorBuffer);
    }

    @Override
    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        skosConceptImportOperations.configureImport(dateFormat, userId, groupId, sourceLang, preferences);
    }

    @Override
    public void setImportDocument(SKOSXmlDocument document) {
        skosConceptImportOperations.setImportDocument(document);
    }

    @Override
    public void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException {
        if (asCandidate) {
            skosConceptImportOperations.importCandidateConcept(resource, thesaurusId);
        } else {
            skosConceptImportOperations.importConcept(resource, thesaurusId);
        }
    }
}
