package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvImportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvImportSupport implements ThesaurusEditionCsvImportSupport {

    private final ThesaurusEditionCsvImportPersistence thesaurusEditionCsvImportPersistence;

    @Override
    public ThesaurusEditionCsvParseResult parse(byte[] content, char delimiter) {
        return thesaurusEditionCsvImportPersistence.parse(content, delimiter);
    }

    @Override
    public ThesaurusEditionCsvImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            String formatDate,
            Integer projectGroupId,
            int userId,
            String userName,
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages
    ) {
        return thesaurusEditionCsvImportPersistence.importNewThesaurus(
                thesaurusName,
                sourceLang,
                formatDate,
                projectGroupId,
                userId,
                userName,
                conceptObjects,
                languages
        );
    }
}
