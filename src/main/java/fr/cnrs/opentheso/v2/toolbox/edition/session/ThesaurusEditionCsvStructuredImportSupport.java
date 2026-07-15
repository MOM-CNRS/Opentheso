package fr.cnrs.opentheso.v2.toolbox.edition.session;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredParseResult;

public interface ThesaurusEditionCsvStructuredImportSupport {

    ThesaurusEditionStructuredParseResult parse(byte[] content, char delimiter);

    ThesaurusEditionStructuredImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            Integer projectGroupId,
            int userId,
            String userName,
            NodeTree root
    );
}
