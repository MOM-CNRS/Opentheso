package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvExportService {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ThesaurusCsvWriter thesaurusCsvWriter;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    public StreamedContent exportThesaurus(
            String thesaurusId,
            String thesaurusTitle,
            char delimiter,
            List<String> selectedLanguageCodes,
            ThesaurusEditionExportOptions exportOptions
    ) throws Exception {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }

        var document = thesaurusSkosDocumentBuilder.buildDocument(
                thesaurusId,
                exportOptions == null ? ThesaurusEditionExportOptions.full() : exportOptions
        );
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        List<NodeLangTheso> usedLanguages = toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);

        List<NodeLangTheso> exportLanguages = usedLanguages;
        if (CollectionUtils.isNotEmpty(selectedLanguageCodes)) {
            exportLanguages = usedLanguages.stream()
                    .filter(lang -> selectedLanguageCodes.contains(lang.getCode()))
                    .toList();
        }
        if (exportLanguages.isEmpty()) {
            exportLanguages = usedLanguages;
        }
        if (exportLanguages.isEmpty()) {
            throw new IllegalStateException("Aucune langue disponible pour l'export CSV");
        }

        byte[] csvBytes = thesaurusCsvWriter.writeCsv(document, exportLanguages, delimiter);
        if (csvBytes == null || csvBytes.length == 0) {
            throw new IllegalStateException("Export CSV vide");
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

    public List<NodeLangTheso> listExportLanguages(String thesaurusId) {
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        return toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);
    }
}
