package fr.cnrs.opentheso.entites;

import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ThesaurusArrayId {

    private String idThesaurus;
    private String idConceptParent;
    private String idFacet;

}
