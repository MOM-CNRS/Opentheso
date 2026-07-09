package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvExportQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvExportQuerySupport implements ThesaurusEditionCsvExportQuerySupport {

    private final ThesaurusEditionCsvExportPersistence thesaurusEditionCsvExportPersistence;

    @Override
    public List<String> listConceptIds(String thesaurusId, List<String> groupIds) {
        return thesaurusEditionCsvExportPersistence.listConceptIds(thesaurusId, groupIds);
    }

    @Override
    public Optional<ThesaurusCsvByIdRow> loadConceptForCsvById(String conceptId, String thesaurusId, String languageCode) {
        return thesaurusEditionCsvExportPersistence.loadConceptForCsvById(conceptId, thesaurusId, languageCode);
    }

    @Override
    public List<NodeDeprecated> listDeprecatedConcepts(String thesaurusId, String languageCode) {
        return thesaurusEditionCsvExportPersistence.listDeprecatedConcepts(thesaurusId, languageCode);
    }
}
