package fr.cnrs.opentheso.v2.concept.ui;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ThesaurusTreeNode implements Serializable {

    private String id;
    private String label;
    private String notation;
    private String nodeType;
    private String status;
    private String candidateBy = "";
    private String candidateOn = "";
    private String path;
    private int depth;
    private boolean hasChildren;
    private boolean expanded;
    private boolean childrenLoaded;
    private List<ThesaurusTreeNode> children = new ArrayList<>();

    public String getPad() {
        return (6 + depth * 18) + "px";
    }

    public String getNotationOrDash() {
        return StringUtils.isBlank(notation) ? "—" : notation;
    }

    public boolean isCandidate() {
        return "candidat".equals(status) || "rejete".equals(status);
    }

    public boolean isRejected() {
        return "rejete".equals(status);
    }

    public boolean isDeprecated() {
        return "deprecie".equals(status);
    }

    public boolean isFacet() {
        return "facet".equals(nodeType);
    }
}
