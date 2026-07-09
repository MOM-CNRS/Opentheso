package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvStructuredExportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvStructuredExportSupport implements ThesaurusEditionCsvStructuredExportSupport {

    private final ThesaurusEditionCsvStructuredExportPersistence thesaurusEditionCsvStructuredExportPersistence;

    @Override
    public String[][] buildStructuredMatrix(String thesaurusId, String languageCode) {
        return thesaurusEditionCsvStructuredExportPersistence.buildStructuredMatrix(thesaurusId, languageCode);
    }
}
