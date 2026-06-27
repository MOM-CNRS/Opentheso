package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CorpusEditor implements Serializable {

    private String corpusName;
    private String uriLink;
    private String uriCount;
    private boolean active;
    private boolean onlyUriLink;
    private boolean omekaS;

    public static CorpusEditor from(ThesaurusCorpus corpus) {
        CorpusEditor editor = new CorpusEditor();
        editor.setCorpusName(corpus.corpusName());
        editor.setUriLink(corpus.uriLink());
        editor.setUriCount(corpus.uriCount());
        editor.setActive(corpus.active());
        editor.setOnlyUriLink(corpus.onlyUriLink());
        editor.setOmekaS(corpus.omekaS());
        return editor;
    }

    public static CorpusEditor empty() {
        return new CorpusEditor();
    }

    public ThesaurusCorpus toModel() {
        return new ThesaurusCorpus(corpusName, uriLink, uriCount, active, onlyUriLink, omekaS, null);
    }
}
