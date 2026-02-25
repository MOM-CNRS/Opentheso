package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ThesaurusDcTermRepository extends JpaRepository<ThesaurusDcTerm, Integer> {
    boolean existsByIdThesaurusAndNameAndValue(String idThesaurus, String name, String value);

    List<ThesaurusDcTerm> findAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);


    @Modifying
    @Transactional
    @Query("DELETE FROM ThesaurusDcTerm tdt WHERE tdt.idThesaurus = :idThesaurus AND tdt.id = :id")
    void deleteDcElementThesaurus(@Param("id") Integer id, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ThesaurusDcTerm t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

}
