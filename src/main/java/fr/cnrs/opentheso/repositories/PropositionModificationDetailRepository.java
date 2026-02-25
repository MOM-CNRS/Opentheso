package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.PropositionModificationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface PropositionModificationDetailRepository extends JpaRepository<PropositionModificationDetail, Integer> {

    List<PropositionModificationDetail> findAllByIdProposition(Integer propositionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PropositionModificationDetail pmd WHERE pmd.idProposition IN (SELECT pm.id FROM PropositionModification pm where pm.idTheso = :idThesaurus)")
    void deleteByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("DELETE FROM PropositionModificationDetail pmd WHERE pmd.idProposition IN (SELECT pm.id FROM PropositionModification pm where pm.idTheso = :idThesaurus and pm.idConcept = :idConcept)")
    void deleteByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);
}