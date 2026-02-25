package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptTermCandidat;
import fr.cnrs.opentheso.models.NodeTraductionCandidatProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ConceptTermCandidatRepository extends JpaRepository<ConceptTermCandidat, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptTermCandidat t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT tc.lang AS lang, tc.lexical_value AS lexicalValue
        FROM concept_term_candidat ctc
            JOIN term_candidat tc ON ctc.id_term = tc.id_term AND ctc.id_thesaurus = tc.id_thesaurus
        WHERE ctc.id_concept = :idConcept
        AND ctc.id_thesaurus = :idThesaurus
        """, nativeQuery = true)
    List<NodeTraductionCandidatProjection> getCandidateTranslations(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Query(value = "UPDATE concept_term_candidat SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
