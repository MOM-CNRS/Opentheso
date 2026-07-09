package fr.cnrs.opentheso.v2.toolbox.edition.session;

import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;

import java.util.List;
import java.util.Optional;

public interface ThesaurusEditionCsvExportQuerySupport {

    List<String> listConceptIds(String thesaurusId, List<String> groupIds);

    Optional<ThesaurusCsvByIdRow> loadConceptForCsvById(String conceptId, String thesaurusId, String languageCode);

    List<NodeDeprecated> listDeprecatedConcepts(String thesaurusId, String languageCode);
}
