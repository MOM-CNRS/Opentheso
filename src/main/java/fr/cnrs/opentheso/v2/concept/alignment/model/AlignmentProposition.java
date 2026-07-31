package fr.cnrs.opentheso.v2.concept.alignment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"conceptId", "targetUri"})
public class AlignmentProposition {

    private String conceptId;
    private String localLabel;
    private String localDefinition;
    private String targetLabel;
    private String targetUri;
    private String targetDefinition;
    private String sourceName;
    private int alignmentTypeId;
    private boolean alreadyAligned;
}
