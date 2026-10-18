package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.shared.persistence.UserTableColPrefEntity;
import fr.cnrs.opentheso.shared.persistence.UserTableColPrefId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTableColPrefRepository
        extends JpaRepository<UserTableColPrefEntity, UserTableColPrefId> {

    List<UserTableColPrefEntity> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}
