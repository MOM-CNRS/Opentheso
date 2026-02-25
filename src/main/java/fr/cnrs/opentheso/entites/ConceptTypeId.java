package fr.cnrs.opentheso.entites;

import lombok.*;

import java.io.Serializable;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConceptTypeId implements Serializable {

    private String code;
    private String idThesaurus;

}
