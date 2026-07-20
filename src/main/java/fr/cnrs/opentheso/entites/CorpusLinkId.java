package fr.cnrs.opentheso.entites;

import java.io.Serializable;
import java.util.Objects;

public class CorpusLinkId implements Serializable {

    private String idThesaurus;
    private String corpusName;

    public CorpusLinkId() {
    }

    public CorpusLinkId(String idThesaurus, String corpusName) {
        this.idThesaurus = idThesaurus;
        this.corpusName = corpusName;
    }

    public String getIdThesaurus() {
        return idThesaurus;
    }

    public void setIdThesaurus(String idThesaurus) {
        this.idThesaurus = idThesaurus;
    }

    public String getCorpusName() {
        return corpusName;
    }

    public void setCorpusName(String corpusName) {
        this.corpusName = corpusName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CorpusLinkId that)) {
            return false;
        }
        return Objects.equals(idThesaurus, that.idThesaurus)
                && Objects.equals(corpusName, that.corpusName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idThesaurus, corpusName);
    }
}
