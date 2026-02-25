package fr.cnrs.opentheso.entites;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import lombok.EqualsAndHashCode;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConceptTermCandidatId implements Serializable {

    private String idConcept;
    private String idTerm;
    private String idThesaurus;

}
