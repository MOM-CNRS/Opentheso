package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvDeprecatedExportSupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvDeprecatedExportService {

    private final ThesaurusEditionCsvDeprecatedExportSupport thesaurusEditionCsvDeprecatedExportSupport;

    public StreamedContent exportThesaurus(
            String thesaurusId,
            String thesaurusTitle,
            String languageCode,
            char delimiter
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }
        if (StringUtils.isBlank(languageCode)) {
            throw new IllegalStateException("Langue manquante");
        }

        byte[] csvBytes = thesaurusEditionCsvDeprecatedExportSupport.writeCsvByDeprecated(
                thesaurusId,
                languageCode,
                delimiter
        );
        if (csvBytes == null || csvBytes.length == 0) {
            throw new IllegalStateException("Export concepts dépréciés vide");
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
