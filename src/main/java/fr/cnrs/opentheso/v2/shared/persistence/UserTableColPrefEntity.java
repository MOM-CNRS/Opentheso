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
@Entity(name = "V2UserTableColPref")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserTableColPrefId.class)
@Table(name = "user_table_col_pref")
public class UserTableColPrefEntity {

    @Id
    @Column(name = "id_user", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "col_id", nullable = false)
    private String colId;

    @Column(name = "selected", nullable = false)
    private boolean selected;
}
