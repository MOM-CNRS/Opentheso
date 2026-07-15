package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record ThesaurusHomeOverview(
        String thesaurusTitle,
        int conceptCount,
        String projectName,
        String lastModifiedDate,
        String permalinkUrl,
        String permalinkLabel,
        List<ConceptLinkItem> lastModifiedConcepts,
        List<ThesaurusMetadataItem> metadata,
        String homePageHtml
) {

    public String getThesaurusTitle() {
        return thesaurusTitle;
    }

    public int getConceptCount() {
        return conceptCount;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    public String getPermalinkLabel() {
        return permalinkLabel;
    }

    public List<ConceptLinkItem> getLastModifiedConcepts() {
        return lastModifiedConcepts;
    }

    public List<ThesaurusMetadataItem> getMetadata() {
        return metadata;
    }

    public String getHomePageHtml() {
        return homePageHtml;
    }
}
