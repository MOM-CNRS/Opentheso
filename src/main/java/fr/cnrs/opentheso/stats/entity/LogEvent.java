package fr.cnrs.opentheso.stats.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA mappée sur la table stat_log_event.
 */
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

    public LogEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StatEventType getEventType() {
        return eventType;
    }

    public void setEventType(StatEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getThesaurusLabel() {
        return thesaurusLabel;
    }

    public void setThesaurusLabel(String thesaurusLabel) {
        this.thesaurusLabel = thesaurusLabel;
    }

    public String getThesaurusId() {
        return thesaurusId;
    }

    public void setThesaurusId(String thesaurusId) {
        this.thesaurusId = thesaurusId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public void setConceptLabel(String conceptLabel) {
        this.conceptLabel = conceptLabel;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getCollectionLabel() {
        return collectionLabel;
    }

    public void setCollectionLabel(String collectionLabel) {
        this.collectionLabel = collectionLabel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}