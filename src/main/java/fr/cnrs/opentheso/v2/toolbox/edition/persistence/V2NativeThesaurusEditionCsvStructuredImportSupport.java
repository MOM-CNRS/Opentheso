package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvStructuredImportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvStructuredImportSupport implements ThesaurusEditionCsvStructuredImportSupport {

    private final ThesaurusEditionCsvStructuredImportPersistence thesaurusEditionCsvStructuredImportPersistence;

    @Override
    public ThesaurusEditionStructuredParseResult parse(byte[] content, char delimiter) {
        return thesaurusEditionCsvStructuredImportPersistence.parse(content, delimiter);
    }

    @Override
    public ThesaurusEditionStructuredImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            Integer projectGroupId,
            int userId,
            String userName,
            NodeTree root
    ) {
        return thesaurusEditionCsvStructuredImportPersistence.importNewThesaurus(
                thesaurusName,
                sourceLang,
                projectGroupId,
                userId,
                userName,
                root
        );
    }
}
