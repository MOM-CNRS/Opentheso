package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ThesaurusAlignementSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface ThesaurusAlignementSourceRepository extends JpaRepository<ThesaurusAlignementSource, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdAlignementSource(String idThesaurus, Integer idAlignementSource);

    @Modifying
    @Transactional
    @Query("UPDATE ThesaurusAlignementSource t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

}
