package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class CustomRelationEditRow implements Serializable {

    private String id;
    private String label;
    private String role;
    private String roleLabel;
    private boolean reciprocal;

    public CustomRelationEditRow(String id, String label, String role, String roleLabel, boolean reciprocal) {
        this.id = id;
        this.label = label;
        this.role = role;
        this.roleLabel = roleLabel;
        this.reciprocal = reciprocal;
    }

    public String getDisplayLabel() {
        return StringUtils.isBlank(label) ? "(" + id + ")" : label;
    }

    public String getDisplayRole() {
        return StringUtils.firstNonBlank(roleLabel, role, "");
    }
}
