package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


public interface PreferencesRepository extends JpaRepository<Preferences, Integer> {


    @Modifying
    @Transactional
    @Query("""
            UPDATE Preferences p
            SET p.preferredName = :preferredName
            WHERE p.idThesaurus = :idThesaurus
            """)
    int updatePreferredName(
            @Param("idThesaurus") String idThesaurus,
            @Param("preferredName") String preferredName);

    @Query("""
            SELECT COUNT(p) > 0
            FROM Preferences p
            WHERE p.preferredName = :preferredName
              AND p.idThesaurus <> :idThesaurus
            """)
    boolean existsInAnotherThesaurus(
            @Param("idThesaurus") String idThesaurus,
            @Param("preferredName") String preferredName
    );

    Optional<Preferences> findByIdThesaurus(String idThesaurus);

    @Query("""
            select p.idThesaurus
            from Preferences p
            where p.preferredName = :preferredName
            """)
    String findIdThesaurusByPreferredName(@Param("preferredName") String preferredName);

    @Transactional
    @Modifying
    void deleteByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Preferences t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Preferences t SET t.userArk = :userArk WHERE t.idThesaurus = :idThesaurus")
    void updateUserArk(@Param("idThesaurus") String idThesaurus, @Param("userArk") String userArk);

    @Modifying
    @Transactional
    @Query("UPDATE Preferences t SET t.useArkLocal = :useArkLocal WHERE t.idThesaurus = :idThesaurus")
    void updateUserArkLocal(@Param("idThesaurus") String idThesaurus, @Param("useArkLocal") String useArkLocal);

    @Modifying
    @Transactional
    @Query("UPDATE Preferences t SET t.useHandle = :useHandle WHERE t.idThesaurus = :idThesaurus")
    void updateUserHandle(@Param("idThesaurus") String idThesaurus, @Param("useHandle") String useHandle);

}
