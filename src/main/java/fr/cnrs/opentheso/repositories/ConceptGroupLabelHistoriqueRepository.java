package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptGroupLabelHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface ConceptGroupLabelHistoriqueRepository extends JpaRepository<ConceptGroupLabelHistorique, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ConceptGroupLabelHistorique c WHERE c.idThesaurus = :idThesaurus")
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroupLabelHistorique t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);
}
