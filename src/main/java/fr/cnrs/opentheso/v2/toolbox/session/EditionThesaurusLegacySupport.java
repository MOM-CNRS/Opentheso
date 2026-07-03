package fr.cnrs.opentheso.v2.toolbox.session;

public interface EditionThesaurusLegacySupport {

    void deleteAllHandleIds(String thesaurusId);

    void deleteRights(String thesaurusId);

    boolean deleteThesaurus(String thesaurusId);
}
