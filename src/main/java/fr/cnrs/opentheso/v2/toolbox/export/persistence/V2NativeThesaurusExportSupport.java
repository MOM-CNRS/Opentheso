package fr.cnrs.opentheso.v2.toolbox.export.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosExportService;
import fr.cnrs.opentheso.v2.toolbox.export.session.ThesaurusExportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.primefaces.model.StreamedContent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class V2NativeThesaurusExportSupport implements ThesaurusExportLegacySupport {

    private final ThesaurusEditionSkosExportService thesaurusEditionSkosExportService;

    @Override
    public StreamedContent exportSkos(String thesaurusId, String thesaurusTitle, String format) throws Exception {
        return thesaurusEditionSkosExportService.exportThesaurus(thesaurusId, thesaurusTitle, format);
    }
}
