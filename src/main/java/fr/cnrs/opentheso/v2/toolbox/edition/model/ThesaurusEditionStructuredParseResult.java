package fr.cnrs.opentheso.v2.toolbox.edition.model;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import org.apache.commons.lang3.StringUtils;

public record ThesaurusEditionStructuredParseResult(NodeTree root, int totalConcepts, String error) {

    public static ThesaurusEditionStructuredParseResult error(String message) {
        return new ThesaurusEditionStructuredParseResult(null, 0, message);
    }

    public boolean isSuccess() {
        return StringUtils.isBlank(error);
    }
}
