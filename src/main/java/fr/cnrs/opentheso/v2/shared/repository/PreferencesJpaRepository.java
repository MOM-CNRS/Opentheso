package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PreferencesJpaRepository extends JpaRepository<PreferencesEntity, Integer> {

    Optional<PreferencesEntity> findByIdThesaurus(String idThesaurus);

    boolean existsByPreferredName(String preferredName);

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

}
