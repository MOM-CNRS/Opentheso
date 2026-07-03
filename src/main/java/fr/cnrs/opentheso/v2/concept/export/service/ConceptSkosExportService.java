package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ConceptSkosExportService {

    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;

    public ExportResult exportConcept(String thesaurusId, String conceptId, String formatCode) throws IOException {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(conceptId)) {
            throw new IllegalStateException("Concept ou thésaurus manquant");
        }

        Preferences preferences = conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .orElse(null);
        validatePreferences(preferences);

        conceptSkosRdfExportEngine.prepareExport(preferences);
        SKOSXmlDocument document = new SKOSXmlDocument();
        document.addconcept(conceptSkosRdfExportEngine.exportConcept(thesaurusId, conceptId));

        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());

        return new ExportResult(
                content,
                thesaurusId + "_" + conceptId + format.extension(),
                "application/xml"
        );
    }

    private void validatePreferences(Preferences preferences) {
        if (preferences == null) {
            throw new IllegalStateException("Absence des préférences !");
        }
        if (StringUtils.isEmpty(preferences.getCheminSite())) {
            throw new IllegalStateException("Manque l'URL du site, veuillez paramétrer les préférences du thésaurus!");
        }
        if (StringUtils.isEmpty(preferences.getOriginalUri())) {
            throw new IllegalStateException("Manque l'URL du site, veuillez paramétrer les préférences du thésaurus!");
        }
    }
}
