package fr.cnrs.opentheso.v2.concept.alignment.model;

import fr.cnrs.opentheso.models.alignment.SelectedResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"conceptId", "targetUri"})
public class AlignmentProposition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String conceptId;
    private String localLabel;
    private String localDefinition;
    private String localUri;
    private String targetLabel;
    private String targetUri;
    private String targetDefinition;
    private String sourceName;
    private int sourceId;
    private int alignmentTypeId;
    private boolean alreadyAligned;
    private boolean enriched;
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private List<SelectedResource> traductions = new ArrayList<>();

    @Builder.Default
    private List<SelectedResource> definitions = new ArrayList<>();

    @Builder.Default
    private List<SelectedResource> images = new ArrayList<>();

    public boolean hasEnrichments() {
        return (traductions != null && !traductions.isEmpty())
                || (definitions != null && !definitions.isEmpty())
                || (images != null && !images.isEmpty());
    }
}
