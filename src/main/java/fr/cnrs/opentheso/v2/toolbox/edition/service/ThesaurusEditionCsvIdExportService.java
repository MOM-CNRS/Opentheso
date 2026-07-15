package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvIdExportService {

    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public StreamedContent exportThesaurus(
            String thesaurusId,
            String thesaurusTitle,
            String languageCode,
            char delimiter,
            boolean filterByGroup,
            List<String> selectedGroupIds
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }
        if (StringUtils.isBlank(languageCode)) {
            throw new IllegalStateException("Langue manquante");
        }

        List<String> groupIds = filterByGroup ? selectedGroupIds : null;
        byte[] csvBytes = thesaurusCsvWriter.writeCsvById(thesaurusId, languageCode, groupIds, delimiter);
        if (csvBytes == null || csvBytes.length == 0) {
            throw new IllegalStateException("Export CSV par ID vide");
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
