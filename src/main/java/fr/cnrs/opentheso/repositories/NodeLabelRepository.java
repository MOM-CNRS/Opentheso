package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.NodeLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface NodeLabelRepository extends JpaRepository<NodeLabel, Integer> {

    Optional<NodeLabel> findByIdFacetAndIdThesaurusAndLang(String idFacet, String idThesaurus, String lang);

    List<NodeLabel> findByIdFacetAndIdThesaurusAndLangNot(String idFacet, String idThesaurus, String lang);

    Optional<NodeLabel> findByIdThesaurusAndLexicalValueAndLang(String idThesaurus, String lexicalValue, String lang);

    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void deleteAllByIdThesaurus(String thesaurusId);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdFacet(String thesaurusId, String idFacet);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdFacetAndLang(String thesaurusId, String idFacet, String lang);

    @Modifying
    @Transactional
    @Query("UPDATE NodeLabel t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT nl.idFacet, nl.lexicalValue, nl.lang, nl.created, nl.modified, ta.idConceptParent
        FROM NodeLabel nl JOIN ThesaurusArray ta ON nl.idFacet = ta.idFacet AND nl.idThesaurus = ta.idThesaurus
        WHERE nl.idThesaurus = :idThesaurus
    """)
    List<Object[]> findAllFacetsWithConceptParent(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT nl.idFacet, nl.lexicalValue 
        FROM NodeLabel nl 
        WHERE nl.lang = :lang
          AND LOWER(FUNCTION('unaccent', nl.lexicalValue)) LIKE LOWER(FUNCTION('unaccent', CONCAT('%', :name, '%')))
          AND nl.idThesaurus = :idThesaurus
        ORDER BY nl.lexicalValue
    """)
    List<Object[]> searchFacetsByName(@Param("name") String name, @Param("lang") String lang, @Param("idThesaurus") String idThesaurus);


}
