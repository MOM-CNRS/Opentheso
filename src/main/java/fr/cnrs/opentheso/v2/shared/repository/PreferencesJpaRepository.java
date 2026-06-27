package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesJpaRepository extends JpaRepository<PreferencesEntity, Integer> {

    Optional<PreferencesEntity> findByIdThesaurus(String idThesaurus);

    boolean existsByPreferredName(String preferredName);
}
