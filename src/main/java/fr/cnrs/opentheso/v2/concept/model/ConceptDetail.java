package fr.cnrs.opentheso.v2.concept.model;

import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;

import java.util.Collections;
import java.util.List;

public record ConceptDetail(
        ConceptSummary summary,
        List<BreadcrumbStep> breadcrumb,
        List<ConceptRelation> broaderTerms,
        List<ConceptRelation> narrowerTerms,
        List<ConceptRelation> relatedTerms,
        List<String> synonyms,
        List<String> hiddenSynonyms,
        List<ConceptLabel> translations,
        List<ConceptNote> notes,
        List<ConceptAlignmentGroup> alignmentGroups,
        List<ConceptRelation> collections,
        List<ConceptRelation> facets,
        List<ConceptRelation> replacedBy,
        List<ConceptRelation> replaces,
        List<List<BreadcrumbStep>> breadcrumbPaths,
        List<ConceptImageItem> images,
        List<ConceptGpsPoint> gpsPoints,
        List<ConceptExternalResourceItem> externalResources,
        List<ConceptCustomRelationItem> customRelations,
        List<ConceptCorpusLinkItem> corpusLinks,
        ConceptIdentifiers identifiers,
        String contributors,
        String preferredTermId
) {

    public ConceptDetail(
            ConceptSummary summary,
            List<BreadcrumbStep> breadcrumb,
            List<ConceptRelation> broaderTerms,
            List<ConceptRelation> narrowerTerms,
            List<ConceptRelation> relatedTerms,
            List<String> synonyms,
            List<String> hiddenSynonyms,
            List<ConceptLabel> translations,
            List<ConceptNote> notes,
            List<ConceptAlignmentGroup> alignmentGroups,
            List<ConceptRelation> collections,
            List<ConceptRelation> facets,
            List<ConceptRelation> replacedBy,
            List<ConceptRelation> replaces
    ) {
        this(
                summary,
                breadcrumb,
                broaderTerms,
                narrowerTerms,
                relatedTerms,
                synonyms,
                hiddenSynonyms,
                translations,
                notes,
                alignmentGroups,
                collections,
                facets,
                replacedBy,
                replaces,
                List.of(List.copyOf(breadcrumb)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "",
                ""
        );
    }

    public ConceptSummary getSummary() {
        return summary;
    }

    public List<BreadcrumbStep> getBreadcrumb() {
        return breadcrumb;
    }

    public List<List<BreadcrumbStep>> getBreadcrumbPaths() {
        return breadcrumbPaths;
    }

    public List<ConceptRelation> getBroaderTerms() {
        return broaderTerms;
    }

    public List<ConceptRelation> getNarrowerTerms() {
        return narrowerTerms;
    }

    public List<ConceptRelation> getRelatedTerms() {
        return relatedTerms;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getHiddenSynonyms() {
        return hiddenSynonyms;
    }

    public List<ConceptLabel> getTranslations() {
        return translations;
    }

    public List<ConceptNote> getNotes() {
        return notes;
    }

    public List<ConceptAlignmentGroup> getAlignmentGroups() {
        return alignmentGroups;
    }

    public List<ConceptRelation> getCollections() {
        return collections;
    }

    public List<ConceptRelation> getFacets() {
        return facets;
    }

    public List<ConceptRelation> getReplacedBy() {
        return replacedBy;
    }

    public List<ConceptRelation> getReplaces() {
        return replaces;
    }

    public List<ConceptImageItem> getImages() {
        return images;
    }

    public List<ConceptGpsPoint> getGpsPoints() {
        return gpsPoints;
    }

    public List<ConceptExternalResourceItem> getExternalResources() {
        return externalResources;
    }

    public List<ConceptCustomRelationItem> getCustomRelations() {
        return customRelations;
    }

    public List<ConceptCorpusLinkItem> getCorpusLinks() {
        return corpusLinks;
    }

    public ConceptIdentifiers getIdentifiers() {
        return identifiers;
    }

    public String getContributors() {
        return contributors;
    }

    public String getPreferredTermId() {
        return preferredTermId;
    }

    public boolean isDeprecated() {
        return ConceptStatusPolicy.isDeprecated(summary.status());
    }

    public boolean getDeprecated() {
        return isDeprecated();
    }

    public boolean hasNotesOfType(String typeCode) {
        return notes.stream().anyMatch(note -> org.apache.commons.lang3.StringUtils.equals(note.typeCode(), typeCode));
    }

    public List<ConceptNote> notesOfType(String typeCode) {
        return notes.stream()
                .filter(note -> org.apache.commons.lang3.StringUtils.equals(note.typeCode(), typeCode))
                .toList();
    }

    public boolean hasAlignments() {
        return alignmentGroups.stream().anyMatch(group -> !group.items().isEmpty());
    }

    public boolean hasMediaData() {
        return (images != null && !images.isEmpty())
                || (gpsPoints != null && !gpsPoints.isEmpty())
                || (externalResources != null && !externalResources.isEmpty());
    }

    public boolean hasCustomRelations() {
        return !customRelations.isEmpty();
    }

    public List<ConceptCustomRelationItem> outgoingCustomRelations() {
        if (customRelations == null || customRelations.isEmpty()) {
            return Collections.emptyList();
        }
        return customRelations.stream()
                .filter(relation -> !relation.reciprocal())
                .toList();
    }

    public List<ConceptCustomRelationItem> getOutgoingCustomRelations() {
        return outgoingCustomRelations();
    }

    public List<ConceptCustomRelationItem> reciprocalCustomRelations() {
        return customRelations.stream()
                .filter(ConceptCustomRelationItem::reciprocal)
                .toList();
    }

    public List<ConceptCustomRelationItem> getReciprocalCustomRelations() {
        return reciprocalCustomRelations();
    }

    public boolean hasOutgoingCustomRelations() {
        return !outgoingCustomRelations().isEmpty();
    }

    public boolean getHasOutgoingCustomRelations() {
        return hasOutgoingCustomRelations();
    }

    public boolean hasReciprocalCustomRelations() {
        return !reciprocalCustomRelations().isEmpty();
    }

    public boolean getHasReciprocalCustomRelations() {
        return hasReciprocalCustomRelations();
    }

    public boolean hasCorpusLinks() {
        return !corpusLinks.isEmpty();
    }
}
