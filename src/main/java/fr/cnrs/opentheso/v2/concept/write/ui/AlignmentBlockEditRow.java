package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class AlignmentBlockEditRow implements Serializable {

    private int alignmentId;
    private int typeId;
    private String uri;
    private String source;
    private boolean existing;

    public AlignmentBlockEditRow(int alignmentId, int typeId, String uri, String source, boolean existing) {
        this.alignmentId = alignmentId;
        this.typeId = typeId;
        this.uri = uri;
        this.source = source;
        this.existing = existing;
    }
}
