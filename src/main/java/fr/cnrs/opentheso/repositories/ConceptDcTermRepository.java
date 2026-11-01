package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ConceptDcTermRepository extends JpaRepository<ConceptDcTerm, Integer> {

    List<ConceptDcTerm> findAllByIdThesaurusAndIdConcept(String idThesaurus, String idConcept);

    /**
     * Même identifiant que la fiche, ou l'ARK stocké à la place de {@code id_concept}.
     */
    @Query(value = """
            SELECT cd.name, cd.value, COALESCE(cd.language, ''), COALESCE(cd.data_type, '')
            FROM concept_dcterms cd
            WHERE cd.id_thesaurus = :idThesaurus
              AND (
                cd.id_concept = :idConcept
                OR cd.id_concept LIKE '%/' || :idConcept
                OR cd.id_concept LIKE '%idc=' || :idConcept || '&%'
                OR cd.id_concept LIKE '%idf=' || :idConcept || '&%'
                OR cd.id_concept LIKE '%idc=' || :idConcept
                OR cd.id_concept LIKE '%idf=' || :idConcept
                OR cd.id_concept IN (
                    SELECT c.id_ark FROM concept c
                    WHERE c.id_concept = :idConcept
                      AND c.id_thesaurus = :idThesaurus
                      AND c.id_ark IS NOT NULL AND btrim(c.id_ark) <> ''
                    UNION
                    SELECT regexp_replace(c.id_ark, '^.*/', '') FROM concept c
                    WHERE c.id_concept = :idConcept
                      AND c.id_thesaurus = :idThesaurus
                      AND c.id_ark IS NOT NULL AND position('/' in c.id_ark) > 0
                )
              )
            ORDER BY cd.name, cd.value
            """, nativeQuery = true)
    List<Object[]> findDisplayedMetadata(@Param("idThesaurus") String idThesaurus,
                                         @Param("idConcept") String idConcept);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptDcTerm t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Query(value = "UPDATE concept_dcterms SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
