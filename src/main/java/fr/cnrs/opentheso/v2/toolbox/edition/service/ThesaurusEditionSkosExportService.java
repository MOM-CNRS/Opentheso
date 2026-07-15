package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.ThesaurusSkosSerializer;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.Rio;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionSkosExportService {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;

    public StreamedContent exportThesaurus(String thesaurusId, String thesaurusTitle, String formatCode) throws Exception {
        return exportThesaurus(thesaurusId, thesaurusTitle, formatCode, ThesaurusEditionExportOptions.full());
    }

    public StreamedContent exportThesaurus(
            String thesaurusId,
            String thesaurusTitle,
            String formatCode,
            ThesaurusEditionExportOptions exportOptions
    ) throws Exception {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }

        var document = thesaurusSkosDocumentBuilder.buildDocument(
                thesaurusId,
                exportOptions == null ? ThesaurusEditionExportOptions.full() : exportOptions
        );
        var resolved = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();

        try (var output = new ByteArrayOutputStream()) {
            var serializer = new ThesaurusSkosSerializer(document);
            Rio.write(serializer.getModel(), output, resolved.rdfFormat());
            serializer.closeCache();

            byte[] bytes = output.toByteArray();
            return DefaultStreamedContent.builder()
                    .contentType("application/xml")
                    .name(node.getValue() + "_" + node.getId() + resolved.extension())
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Export SKOS impossible", ex);
        }
    }
}
