package fr.cnrs.opentheso.v2.setting.model;

public record ThesaurusCorpus(
        String corpusName,
        String uriLink,
        String uriCount,
        boolean active,
        boolean onlyUriLink,
        boolean omekaS,
        Integer sort
) {

    public String getCorpusName() {
        return corpusName;
    }

    public String getUriLink() {
        return uriLink;
    }

    public String getUriCount() {
        return uriCount;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isOnlyUriLink() {
        return onlyUriLink;
    }

    public boolean isOmekaS() {
        return omekaS;
    }

    public Integer getSort() {
        return sort;
    }
}
