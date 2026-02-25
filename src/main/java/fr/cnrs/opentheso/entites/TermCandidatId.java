package fr.cnrs.opentheso.entites;

import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TermCandidatId {

    private String idTerm;
    private String lexicalValue;
    private String lang;
    private String idThesaurus;

}
