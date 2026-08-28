package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface NoteRepository extends JpaRepository<Note, Integer> {

    List<Note> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    List<Note> findAllByIdentifierAndIdThesaurus(String identifier, String idThesaurus);

    Optional<Note> findByIdAndIdThesaurus(int id, String idThesaurus);

    Optional<Note> findByLexicalValue(String lexicalValue);

    List<Note> findAllByIdentifierAndIdThesaurusAndLang(String identifier, String idThesaurus, String lang);

    List<Note> findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
            String identifier, String idThesaurus, String noteTypeCode, String lang);

    Optional<Note> findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
            String identifier, String idThesaurus, String noteTypeCode, String lang, String lexicalValue);

    @Modifying
    @Transactional
    @Query("DELETE FROM Note n WHERE n.idThesaurus = :idThesaurus")
    void deleteAllByIdThesaurus(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    void deleteByIdAndIdThesaurus(int idNote, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdentifierAndIdThesaurus(String identifier, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdentifierAndLangAndNoteTypeCode(String idThesaurus, String identifier, String idLang, String noteTypeCode);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM Note n
        WHERE n.idThesaurus = :idThesaurus
          AND LOWER(n.noteSource) = LOWER(:noteSource)
          AND (n.identifier = :conceptId OR n.idConcept = :conceptId)
        """)
    void deleteByConceptThesaurusAndNoteSource(
            @Param("conceptId") String conceptId,
            @Param("idThesaurus") String idThesaurus,
            @Param("noteSource") String noteSource);

    @Modifying
    @Transactional
    @Query("UPDATE Note t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT COUNT(n.id)
        FROM note n JOIN concept_group_concept cgc ON cgc.idconcept = n.identifier AND cgc.idthesaurus = n.id_thesaurus
        WHERE n.id_thesaurus = :idThesaurus
            AND n.lang = :lang
            AND LOWER(cgc.idgroup) = LOWER(:idGroup)
    """, nativeQuery = true)
    int countNotesByGroupAndLangAndThesaurus(@Param("idGroup") String idGroup, @Param("idThesaurus") String idThesaurus, @Param("lang") String lang);

    @Query(value = """
        SELECT COUNT(n.id)
        FROM note n JOIN concept c ON c.id_concept = n.id_concept AND c.id_thesaurus = n.id_thesaurus
        WHERE n.lang = :lang
        AND n.id_thesaurus = :idThesaurus
        AND c.id_concept NOT IN (SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :idThesaurus)
    """, nativeQuery = true)
    int countNotesWithoutGroupByLangAndThesaurus(@Param("idThesaurus") String idThesaurus, @Param("lang") String lang);

    @Query(value = """
        SELECT COUNT(n.id)
        FROM note n
            JOIN preferred_term pt ON pt.id_term = n.id_term AND pt.id_thesaurus = n.id_thesaurus
        WHERE n.lang = :lang
        AND n.id_thesaurus = :idThesaurus
        AND pt.id_concept NOT IN (SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :idThesaurus)
    """, nativeQuery = true)
    int countNotesOfTermsWithoutGroup(@Param("idThesaurus") String idThesaurus, @Param("lang") String lang);

    @Modifying
    @Query(value = "UPDATE note SET id_thesaurus = :target FROM preferred_term WHERE note.id_term = preferred_term.id_term AND note.id_thesaurus = preferred_term.id_thesaurus AND preferred_term.id_concept = :concept AND note.id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByTerm(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Modifying
    @Query(value = "UPDATE note SET id_thesaurus = :target WHERE identifier = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByConcept(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    boolean existsByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(String identifier, String idThesaurus, String noteTypeCode, String lang);
}
