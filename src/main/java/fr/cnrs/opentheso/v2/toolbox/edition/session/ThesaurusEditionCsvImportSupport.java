package fr.cnrs.opentheso.v2.toolbox.edition.session;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvParseResult;

import java.util.List;

public interface ThesaurusEditionCsvImportSupport {

    ThesaurusEditionCsvParseResult parse(byte[] content, char delimiter);

    ThesaurusEditionCsvImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            String formatDate,
            Integer projectGroupId,
            int userId,
            String userName,
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages
    );
}
