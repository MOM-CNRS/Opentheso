package fr.cnrs.opentheso.stats.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité JPA mappée sur la table stat_log_event.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stat_log_event")
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private StatEventType eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "thesaurus_label", length = 500)
    private String thesaurusLabel;

    @Column(name = "thesaurus_id", length = 50)
    private String thesaurusId;

    @Column(name = "concept_id", length = 50)
    private String conceptId;

    @Column(name = "concept_label", length = 500)
    private String conceptLabel;

    @Column(name = "lang", length = 10)
    private String lang;

    @Column(name = "collection_id", length = 50)
    private String collectionId;

    @Column(name = "collection_label", length = 500)
    private String collectionLabel;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "searched_term", length = 500)
    private String searchedTerm;

    @Column(name = "selected_term", length = 500)
    private String selectedTerm;

    @Column(name = "nb_results")
    private Integer nbResults;
}