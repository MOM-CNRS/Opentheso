package fr.cnrs.opentheso.v2.preview.ui;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PreviewTreeNode implements Serializable {

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
    private List<PreviewTreeNode> children = new ArrayList<>();

    public String getPad() {
        return (6 + depth * 18) + "px";
    }

    public String getNotationOrDash() {
        return StringUtils.isBlank(notation) ? "—" : notation;
    }

    public boolean isCandidate() {
        return "candidat".equals(status);
    }
}
