package fr.cnrs.opentheso.entites;

import lombok.*;

import java.io.Serializable;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConceptFacetId implements Serializable {

    private String idFacet;
    private String idConcept;
    private String idThesaurus;

}
