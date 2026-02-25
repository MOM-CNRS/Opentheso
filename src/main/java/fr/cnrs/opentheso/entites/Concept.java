package fr.cnrs.opentheso.entites;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.EntityListeners;
import lombok.Setter;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Date;


@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "concept")
@EntityListeners(AuditingEntityListener.class)
public class Concept implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_concept", nullable = false)
    private String idConcept;

    private String idThesaurus;

    @Column(name = "id_ark", nullable = false)
    private String idArk = "";

    @CreatedDate
    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @LastModifiedDate
    @Column(name = "modified", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date modified;

    @Column(name = "status")
    private String status;

    @Column(name = "notation", nullable = false)
    private String notation = "";

    @Column(name = "top_concept")
    private Boolean topConcept;

    @Column(name = "gps", nullable = false)
    private Boolean gps = false;

    @Column(name = "id_handle", nullable = false)
    private String idHandle = "";

    @Column(name = "id_doi", nullable = false)
    private String idDoi = "";

    @Column(name = "creator", nullable = false)
    private Integer creator = -1;

    @Column(name = "contributor", nullable = false)
    private Integer contributor = -1;

    @Column(name = "concept_type", nullable = false)
    private String conceptType = "concept";
}
