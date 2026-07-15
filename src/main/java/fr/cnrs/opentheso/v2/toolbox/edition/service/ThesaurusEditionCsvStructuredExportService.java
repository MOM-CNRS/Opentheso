package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredExportPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredExportService {

    private final ThesaurusEditionCsvStructuredExportPersistence thesaurusEditionCsvStructuredExportPersistence;
    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public StreamedContent exportThesaurus(String thesaurusId, String thesaurusTitle, String languageCode) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }
        if (StringUtils.isBlank(languageCode)) {
            throw new IllegalStateException("Langue manquante");
        }

        String[][] matrix = thesaurusEditionCsvStructuredExportPersistence.buildStructuredMatrix(thesaurusId, languageCode);
        byte[] csvBytes = thesaurusCsvWriter.importTreeCsv(matrix, ';');
        if (csvBytes == null || csvBytes.length == 0) {
            throw new IllegalStateException("Export CSV structuré vide");
        }

        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();

        byte[] content = csvBytes;
        return DefaultStreamedContent.builder()
                .contentType("text/csv")
                .name(node.getValue() + "_" + node.getId() + ".csv")
                .stream(() -> new ByteArrayInputStream(content))
                .build();
    }
}
