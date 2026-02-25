package fr.cnrs.opentheso.entites;

import lombok.*;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ThesaurusAlignementSourceId {

    private String idThesaurus;
    private Integer idAlignementSource;
}
