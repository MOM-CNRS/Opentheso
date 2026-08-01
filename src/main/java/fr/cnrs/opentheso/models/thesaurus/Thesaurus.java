package fr.cnrs.opentheso.models.thesaurus;

import fr.cnrs.opentheso.models.nodes.DcElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Thesaurus {

    private String id_thesaurus;
    private String id_ark;
    private String contributor = "";
    private String coverage = "";
    private String creator = "";
    private Date created;
    private Date modified;
    private String description = "";
    private String format = "";
    private String language;
    private String publisher = "";
    private String relation = "";
    private String rights = "";
    private String source = "";
    private String subject = "";
    private String title = "";
    private String type = "";
    private boolean privateTheso;
    /** master | slave — rôle du thésaurus (opentheso:thesaurusRole). */
    private String thesaurusRole;
    private List <DcElement> dcElement = new ArrayList<>();


    public Thesaurus(String idThesaurus, String idArk, boolean isPrivate,
                     String title, String lang, String contributor, String coverage, String creator,
                     String description, String format, String publisher, String relation,
                     String rights, String source, String subject, String type) {

        this.id_thesaurus = idThesaurus;
        this.id_ark = idArk;
        this.privateTheso = isPrivate;
        this.title = title;
        this.language = lang;
        this.contributor = contributor;
        this.coverage = coverage;
        this.creator = creator;
        this.description = description;
        this.format = format;
        this.publisher = publisher;
        this.relation = relation;
        this.rights = rights;
        this.source = source;
        this.subject = subject;
        this.type = type;
    }

    public Thesaurus(String contributor, String coverage, String creator, String description, String format, String id_langue, String publisher, String relation, String rights, String source, String subject, String title, String type) {
        this.contributor = contributor;
        this.coverage = coverage;
        this.creator = creator;
        this.description = description;
        this.format = format;
        this.language = id_langue;
        this.publisher = publisher;
        this.relation = relation;
        this.rights = rights;
        this.source = source;
        this.subject = subject;
        this.title = title;
        this.type = type;
    }

    public void addDcElement(DcElement dcElement) {
        this.dcElement.add(dcElement);
    }
}
