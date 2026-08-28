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
@Entity(name = "V2UserConceptBlockPref")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserConceptBlockPrefId.class)
@Table(name = "user_concept_block_pref")
public class UserConceptBlockPrefEntity {

    @Id
    @Column(name = "id_user", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "block_id", nullable = false)
    private String blockId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "collapsed", nullable = false)
    private boolean collapsed;
}
