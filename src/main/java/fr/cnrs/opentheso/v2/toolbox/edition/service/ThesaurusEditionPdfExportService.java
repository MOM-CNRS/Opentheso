package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionPdfExportService {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ThesaurusPdfWriter thesaurusPdfWriter;

    public StreamedContent exportThesaurus(
            String thesaurusId,
            String thesaurusTitle,
            String languageCode1,
            String languageCode2,
            boolean hierarchical,
            boolean includeImages,
            ThesaurusEditionExportOptions exportOptions
    ) throws Exception {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }
        if (StringUtils.isBlank(languageCode1)) {
            throw new IllegalStateException("Langue principale manquante");
        }

        var document = thesaurusSkosDocumentBuilder.buildDocument(
                thesaurusId,
                exportOptions == null ? ThesaurusEditionExportOptions.full() : exportOptions
        );
        ThesaurusPdfExportType exportType = hierarchical
                ? ThesaurusPdfExportType.HIERARCHIQUE
                : ThesaurusPdfExportType.ALPHABETIQUE;
        byte[] pdfBytes = thesaurusPdfWriter.createPdfFile(
                document,
                languageCode1,
                StringUtils.defaultString(languageCode2),
                exportType,
                includeImages
        );
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException("Export PDF vide");
        }

        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();

        byte[] content = pdfBytes;
        return DefaultStreamedContent.builder()
                .contentType("application/pdf")
                .name(node.getValue() + "_" + node.getId() + ".pdf")
                .stream(() -> new ByteArrayInputStream(content))
                .build();
    }
}
