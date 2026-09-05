package fr.cnrs.opentheso.v2.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "V2Project")
@Table(name = "user_group_label")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_group")
    private Integer id;

    @Column(name = "label_group")
    private String label;

    public ProjectEntity() {
        // Constructeur sans argument requis par JPA.
    }
}
