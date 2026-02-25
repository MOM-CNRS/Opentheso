package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.models.NodeEMProjection;
import fr.cnrs.opentheso.models.terms.NodeEM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface NonPreferredTermRepository extends JpaRepository<NonPreferredTerm, Integer> {

    @Query(value = """
        SELECT COUNT(*) > 0 
        FROM non_preferred_term 
        WHERE f_unaccent(LOWER(lexical_value)) = f_unaccent(LOWER(:title)) 
        AND id_thesaurus = :idThesaurus 
        AND lang = :idLang
        """, nativeQuery = true)
    boolean isAltLabelExist(@Param("title") String title, @Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdTermAndLexicalValueAndLang(String idThesaurus, String idTerm, String lexicalValue, String lang);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdTerm(String idThesaurus, String idTerm);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdTermAndLangAndLexicalValue(String idThesaurus, String idTerm, String lang, String lexicalValue);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdTermAndLang(String idThesaurus, String idTerm, String lang);

    @Modifying
    @Transactional
    void deleteByIdThesaurus(String idThesaurus);

    Optional<NonPreferredTerm> findByIdTermAndLexicalValueAndLangAndIdThesaurus(String idTerm, String lexicalValue, String lang, String idThesaurus);

    List<NonPreferredTerm> findAllByIdThesaurusAndIdTermAndLangAndHidenNot(String idThesaurus, String idTerm, String lang, boolean hiden);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM non_preferred_term 
        USING preferred_term 
        WHERE non_preferred_term.id_thesaurus = preferred_term.id_thesaurus 
            AND non_preferred_term.id_term = preferred_term.id_term 
            AND preferred_term.id_concept = :idConcept 
            AND non_preferred_term.id_thesaurus = :idThesaurus
    """, nativeQuery = true)
    int deleteAllByConceptAndThesaurus(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT npt.lexical_value 
        FROM non_preferred_term npt
        JOIN preferred_term pt ON pt.id_thesaurus = npt.id_thesaurus AND pt.id_term = npt.id_term
        WHERE pt.id_concept = :idConcept 
            AND npt.id_thesaurus = :idThesaurus 
            AND npt.lang = :idLang
        ORDER BY f_unaccent(LOWER(npt.lexical_value))
    """, nativeQuery = true)
    List<String> findAltLabelsByConceptAndThesaurusAndLang(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus,
            @Param("idLang") String idLang);

    @Query(value = """
        SELECT new fr.cnrs.opentheso.models.terms.NodeEM(npt.lexicalValue, npt.created, npt.modified, npt.source, npt.status, npt.hiden, npt.lang)
        FROM NonPreferredTerm npt JOIN PreferredTerm pt ON pt.idTerm = npt.idTerm AND pt.idThesaurus = npt.idThesaurus
        WHERE pt.idConcept = :idConcept
        AND npt.idThesaurus = :idThesaurus
        ORDER BY npt.lexicalValue ASC
    """)
    List<NodeEM> findAllNodeEMByConcept(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);


    @Query("""
        SELECT new fr.cnrs.opentheso.models.terms.NodeEM(npt.lexicalValue, npt.created, npt.modified, npt.source, npt.status, npt.hiden, npt.lang)
        FROM NonPreferredTerm npt JOIN PreferredTerm pt ON pt.idTerm = npt.idTerm AND pt.idThesaurus = npt.idThesaurus
        WHERE pt.idConcept = :idConcept 
        AND npt.idThesaurus = :idThesaurus 
        AND npt.lang = :idLang
        ORDER BY npt.lexicalValue
    """)
    List<NodeEM> findNodeEMByConceptAndLang(@Param("idConcept") String idConcept,
                                            @Param("idThesaurus") String idThesaurus,
                                            @Param("idLang") String idLang);

    @Modifying
    @Transactional
    @Query("UPDATE NonPreferredTerm t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT npt.lexical_value AS lexicalValue, npt.created AS created, npt.modified AS modified, npt.source AS source, npt.status AS status, npt.hiden AS hiden
        FROM non_preferred_term npt
            JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
        WHERE pt.id_concept = :idConcept
        AND npt.id_thesaurus = :idThesaurus
        AND npt.lang = :idLang
        ORDER BY npt.lexical_value ASC
    """, nativeQuery = true)
    List<NodeEMProjection> findNonPreferredTerms(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Modifying
    @Transactional
    @Query(value = "UPDATE non_preferred_term SET id_thesaurus = :target FROM preferred_term " +
            "WHERE non_preferred_term.id_term = preferred_term.id_term " +
            "AND non_preferred_term.id_thesaurus = preferred_term.id_thesaurus " +
            "AND preferred_term.id_concept = :concept " +
            "AND non_preferred_term.id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
