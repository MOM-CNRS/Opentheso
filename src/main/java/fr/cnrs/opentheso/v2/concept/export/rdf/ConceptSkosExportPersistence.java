package fr.cnrs.opentheso.v2.concept.export.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatSkosExportMetadataPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.ThesaurusSkosSerializer;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

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
        List<SKOSResource> resources = exportConcepts(
                thesaurusId,
                StringUtils.isBlank(conceptId) ? List.of() : List.of(conceptId),
                null
        );
        SKOSResource resource = resources.isEmpty() ? null : resources.get(0);
        if (resource != null && candidatExport) {
            candidatSkosExportMetadataPersistence.enrich(resource, conceptId, thesaurusId);
        }
        return resource;
    }

    public List<SKOSResource> exportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress
    ) throws Exception {
        return exportConcepts(thesaurusId, conceptIds, progress, false);
    }

    @Transactional(readOnly = true)
    public List<SKOSResource> exportConcepts(
            String thesaurusId,
            Collection<String> conceptIds,
            BiConsumer<Integer, Integer> progress,
            boolean clearHtml
    ) throws Exception {
        Preferences preferences = findThesaurusPreferences(thesaurusId).orElse(null);
        if (preferences == null) {
            return List.of();
        }
        return thesaurusSkosDocumentBuilder.exportConcepts(
                thesaurusId, conceptIds, preferences, clearHtml, progress
        );
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
