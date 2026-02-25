package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptCandidat;
import fr.cnrs.opentheso.models.CandidateSearchProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ConceptCandidatRepository extends JpaRepository<ConceptCandidat, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptCandidat t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    List<ConceptCandidat> findAllByIdThesaurusAndStatus(String idThesaurus, String status);

    @Query(value = """
        SELECT DISTINCT t.lang as lang, t.id_term as idTerm, t.lexical_value as lexicalValue, c.id_concept as idConcept,
            c.id_thesaurus as idThesaurus, c.created as created, u.username as username, t.contributor as contributor
        FROM preferred_term pt
            JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
            JOIN users u ON t.contributor = u.id_user
            JOIN candidat_status cs ON cs.id_concept = c.id_concept AND cs.id_thesaurus = c.id_thesaurus
        WHERE cs.id_status = :status
        AND t.lang = :lang
        AND c.id_thesaurus = :idThesaurus
        AND f_unaccent(lower(t.lexical_value)) LIKE CONCAT('%', f_unaccent(lower(:value)), '%')
        AND (:statut = 'CA' AND c.status = 'CA' OR :statut != 'CA' AND c.status != 'CA')
        ORDER BY t.lexical_value ASC
        """, nativeQuery = true)
    List<CandidateSearchProjection> searchCandidatesByValue(@Param("value") String value, @Param("idThesaurus") String idThesaurus,
            @Param("lang") String lang, @Param("status") int status, @Param("statut") String statut);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_candidat SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
