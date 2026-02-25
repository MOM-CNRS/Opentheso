package fr.cnrs.opentheso.entites;

import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConceptReplacedById {

    private String idConcept1;
    private String idConcept2;
    private String idThesaurus;
}

