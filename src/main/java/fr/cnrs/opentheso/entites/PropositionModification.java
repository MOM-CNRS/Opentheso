package fr.cnrs.opentheso.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "proposition_modification")
public class PropositionModification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String idConcept;
    private String idTheso;
    private String status;
    private String nom;
    private String email;
    private String commentaire;
    private String approuvePar;
    private Date approuveDate;
    private String lang;
    private String date;
    private String adminComment;
    @Builder.Default
    @Column(name = "from_sync")
    private Boolean fromSync = Boolean.FALSE;
}
