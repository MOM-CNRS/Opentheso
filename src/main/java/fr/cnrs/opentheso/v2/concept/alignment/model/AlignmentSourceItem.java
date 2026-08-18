package fr.cnrs.opentheso.v2.concept.alignment.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class AlignmentSourceItem implements Serializable {

    private int sourceId;
    private String label;
    private String description;
    private boolean selected;
    private boolean global;
    private String sourceType;
    private String url;
    private String thesaurusOwner;

    public AlignmentSourceItem(
            int sourceId,
            String label,
            String description,
            boolean selected,
            boolean global,
            String sourceType,
            String url
    ) {
        this(sourceId, label, description, selected, global, sourceType, url, null);
    }

    public AlignmentSourceItem(
            int sourceId,
            String label,
            String description,
            boolean selected,
            boolean global,
            String sourceType,
            String url,
            String thesaurusOwner
    ) {
        this.sourceId = sourceId;
        this.label = label;
        this.description = description;
        this.selected = selected;
        this.global = global;
        this.sourceType = sourceType;
        this.url = url;
        this.thesaurusOwner = thesaurusOwner;
    }

    public boolean isLocalSource() {
        return !global;
    }
}
