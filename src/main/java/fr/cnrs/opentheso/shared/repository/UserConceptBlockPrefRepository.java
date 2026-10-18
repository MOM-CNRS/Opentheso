package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.shared.persistence.UserConceptBlockPrefEntity;
import fr.cnrs.opentheso.shared.persistence.UserConceptBlockPrefId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConceptBlockPrefRepository
        extends JpaRepository<UserConceptBlockPrefEntity, UserConceptBlockPrefId> {

    List<UserConceptBlockPrefEntity> findByUserIdOrderByPositionAsc(Integer userId);

    void deleteByUserId(Integer userId);
}
