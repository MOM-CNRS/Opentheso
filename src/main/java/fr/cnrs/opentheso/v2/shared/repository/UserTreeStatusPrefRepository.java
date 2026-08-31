package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.UserTreeStatusPrefEntity;
import fr.cnrs.opentheso.v2.shared.persistence.UserTreeStatusPrefId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTreeStatusPrefRepository
        extends JpaRepository<UserTreeStatusPrefEntity, UserTreeStatusPrefId> {

    List<UserTreeStatusPrefEntity> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}
