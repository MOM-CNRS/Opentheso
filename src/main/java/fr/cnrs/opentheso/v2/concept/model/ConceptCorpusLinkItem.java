package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptCorpusLinkItem(
        String corpusName,
        String uriLink,
        int noticeCount,
        boolean onlyUriLink,
        boolean active
) implements Serializable {

    public String getCorpusName() {
        return corpusName;
    }

    public String getUriLink() {
        return uriLink;
    }

    public int getNoticeCount() {
        return noticeCount;
    }

    public boolean isOnlyUriLink() {
        return onlyUriLink;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasLink() {
        return onlyUriLink || noticeCount > 0;
    }

    public boolean getHasLink() {
        return hasLink();
    }
}
