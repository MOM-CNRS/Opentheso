package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.UserConceptBlockPrefEntity;
import fr.cnrs.opentheso.v2.shared.persistence.UserConceptBlockPrefId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConceptBlockPrefRepository
        extends JpaRepository<UserConceptBlockPrefEntity, UserConceptBlockPrefId> {

    List<UserConceptBlockPrefEntity> findByUserIdOrderByPositionAsc(Integer userId);

    void deleteByUserId(Integer userId);
}
