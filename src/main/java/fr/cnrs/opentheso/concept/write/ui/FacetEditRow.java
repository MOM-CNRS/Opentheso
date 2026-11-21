package fr.cnrs.opentheso.concept.write.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class FacetEditRow implements Serializable {

    private String id;
    private String label;
    private String role;

    public FacetEditRow(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public FacetEditRow(String id, String label, String role) {
        this(id, label);
        this.role = role;
    }

    public String getDisplayLabel() {
        return StringUtils.isBlank(label) ? "(" + id + ")" : label;
    }
}
