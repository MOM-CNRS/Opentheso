package fr.cnrs.opentheso.v2.concept.export.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatSkosExportMetadataPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.ThesaurusSkosSerializer;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConceptSkosExportPersistence {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final PreferencesRepository preferencesRepository;
    private final CandidatSkosExportMetadataPersistence candidatSkosExportMetadataPersistence;

    public Optional<Preferences> findThesaurusPreferences(String thesaurusId) {
        return preferencesRepository.findByIdThesaurus(thesaurusId);
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId) throws Exception {
        return exportConcept(thesaurusId, conceptId, false);
    }

    public SKOSResource exportConcept(String thesaurusId, String conceptId, boolean candidatExport) throws Exception {
        Preferences preferences = findThesaurusPreferences(thesaurusId).orElse(null);
        if (preferences == null) {
            return null;
        }
        SKOSResource resource = thesaurusSkosDocumentBuilder.exportConcept(thesaurusId, conceptId, preferences);
        if (resource != null && candidatExport) {
            candidatSkosExportMetadataPersistence.enrich(resource, conceptId, thesaurusId);
        }
        return resource;
    }

    public SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return thesaurusSkosDocumentBuilder.exportConceptScheme(thesaurusId, preferences);
    }

    public byte[] serializeSkos(SKOSXmlDocument document, RDFFormat format) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            var serializer = new ThesaurusSkosSerializer(document);
            Rio.write(serializer.getModel(), output, format);
            serializer.closeCache();
            return output.toByteArray();
        }
    }
}
