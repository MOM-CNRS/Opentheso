package fr.cnrs.opentheso.v2.concept.model;

import lombok.Data;

import java.util.List;

/**
 * Données brutes assemblées pour la fiche concept v2, avant projection en {@link ConceptDetail}.
 */
@Data
public class ConceptFullSnapshot {

    private String uri;
    private int resourceType;
    private String identifier;
    private String permanentId;
    private String notation;
    private int resourceStatus;
    private String conceptType;
    private String created;
    private String modified;
    private String creatorName;
    private List<String> contributorName;
    private ConceptTermLabel prefLabel;
    private List<ConceptTermLabel> altLabels;
    private List<ConceptTermLabel> hiddenLabels;
    private List<ConceptTermLabel> prefLabelsTraduction;
    private List<ConceptTermLabel> altLabelTraduction;
    private List<ConceptTermLabel> hiddenLabelTraduction;
    private List<ConceptHierarchicalRelation> narrowers;
    private List<ConceptHierarchicalRelation> broaders;
    private List<ConceptHierarchicalRelation> relateds;
    private List<ConceptSnapshotNote> notes;
    private List<ConceptSnapshotNote> definitions;
    private List<ConceptSnapshotNote> examples;
    private List<ConceptSnapshotNote> editorialNotes;
    private List<ConceptSnapshotNote> changeNotes;
    private List<ConceptSnapshotNote> scopeNotes;
    private List<ConceptSnapshotNote> historyNotes;
    private List<ConceptUriLabel> exactMatchs;
    private List<ConceptUriLabel> closeMatchs;
    private List<ConceptUriLabel> broadMatchs;
    private List<ConceptUriLabel> relatedMatchs;
    private List<ConceptUriLabel> narrowMatchs;
    private List<ConceptGpsPoint> gps;
    private List<ConceptExternalResourceItem> externalResources;
    private List<ConceptImageItem> images;
    private List<ConceptUriLabel> membres;
    private List<ConceptUriLabel> replacedBy;
    private List<ConceptUriLabel> replaces;
    private List<ConceptUriLabel> facets;

    public boolean isDeprecated() {
        return resourceStatus == ConceptResourceStatus.DEPRECATED;
    }

    public boolean isCandidate() {
        return resourceStatus == ConceptResourceStatus.CANDIDATE;
    }

    public boolean isConcept() {
        return resourceStatus == ConceptResourceStatus.CONCEPT;
    }

    public boolean isCustomRelation() {
        return "concept".equalsIgnoreCase(conceptType);
    }
}
