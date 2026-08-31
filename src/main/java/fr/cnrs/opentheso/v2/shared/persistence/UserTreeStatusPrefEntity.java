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
@Entity(name = "V2UserTreeStatusPref")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserTreeStatusPrefId.class)
@Table(name = "user_tree_status_pref")
public class UserTreeStatusPrefEntity {

    @Id
    @Column(name = "id_user", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "status_id", nullable = false)
    private String statusId;

    @Column(name = "selected", nullable = false)
    private boolean selected;
}
