package fr.cnrs.opentheso.v2.toolbox.export.service;

import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosExportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThesaurusSkosExportService {

    private final ThesaurusEditionSkosExportService thesaurusEditionSkosExportService;

    public StreamedContent exportThesaurus(String thesaurusId, String thesaurusTitle, String formatCode) throws Exception {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus manquant");
        }
        return thesaurusEditionSkosExportService.exportThesaurus(thesaurusId, thesaurusTitle, formatCode);
    }
}
