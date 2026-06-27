package fr.cnrs.opentheso.v2.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "V2CorpusLink")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CorpusLinkId.class)
@Table(name = "corpus_link")
public class CorpusLinkEntity {

    @Id
    @Column(name = "idTheso")
    private String idThesaurus;

    @Id
    private String corpusName;

    private String uriCount;

    private String uriLink;

    private boolean active;

    private boolean onlyUriLink;

    private Integer sort;

    @Column(name = "omeka_s")
    private boolean omekaS;
}
