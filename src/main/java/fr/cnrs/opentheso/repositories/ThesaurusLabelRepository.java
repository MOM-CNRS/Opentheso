package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ThesaurusLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface ThesaurusLabelRepository extends JpaRepository<ThesaurusLabel, String> {

    Optional<ThesaurusLabel> findByIdThesaurusAndLang(String idThesaurus, String lang);

    List<ThesaurusLabel> findByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndLang(String idThesaurus, String lang);

    @Modifying
    @Transactional
    void deleteByIdThesaurus(String idThesaurus);

    @Query("SELECT DISTINCT tl.lang FROM ThesaurusLabel tl WHERE tl.idThesaurus = :idThesaurus")
    List<String> findDistinctLangByIdThesaurus(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ThesaurusLabel t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

}
