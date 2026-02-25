package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Roles;
import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.entites.UserRoleOnlyOn;
import fr.cnrs.opentheso.models.users.NodeUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface UserRoleOnlyOnRepository extends JpaRepository<UserRoleOnlyOn, Integer> {

    @Query("SELECT new fr.cnrs.opentheso.models.users.NodeUserRole(u.thesaurus.idThesaurus, r.name, r.id) " +
            "FROM UserRoleOnlyOn u " +
            "JOIN u.role r " +
            "WHERE u.group.id = :idGroup " +
            "AND u.user.id = :idUser " +
            "ORDER BY LOWER(u.thesaurus.idThesaurus)")
    List<NodeUserRole> getListRoleByThesaurusLimited(@Param("idGroup") int idGroup, @Param("idUser") int idUser);

    List<UserRoleOnlyOn> findAllByUserAndRoleOrderByThesaurus(User user, Roles role);

    @Modifying
    @Transactional
    void deleteByUserAndGroup(User user, UserGroupLabel group);

    @Modifying
    @Transactional
    void deleteByThesaurusIdThesaurus(String thesaurus);

    @Modifying
    @Transactional
    void deleteByUserAndGroupAndRoleAndThesaurus(User user, UserGroupLabel group, Roles role, Thesaurus thesaurus);

    List<UserRoleOnlyOn> findAllByUserOrderByThesaurus(User user);

    UserRoleOnlyOn findByUserAndGroupAndThesaurus(User user, UserGroupLabel group, Thesaurus thesaurus);

    @Query("""
        SELECT new fr.cnrs.opentheso.models.users.NodeUserRole(u.id, u.username, u.active, r.id, r.name, uroo.thesaurus.idThesaurus)
        FROM UserRoleOnlyOn uroo
            JOIN User u ON uroo.user.id = u.id
            JOIN Roles r ON uroo.role.id = r.id
        WHERE uroo.group.id = :idGroup
          AND u.isSuperAdmin = false
        ORDER BY LOWER(u.username)
    """)
    List<NodeUserRole> findAllUsersWithLimitedRoleByGroup(@Param("idGroup") int idGroup);


    @Query("""
        SELECT ro.role.id 
        FROM UserRoleOnlyOn ro 
        WHERE ro.user.id = :idUser AND ro.group.id = :idGroup AND ro.thesaurus.idThesaurus = :idThesaurus
    """)
    Optional<Integer> findRoleFromOnlyOn(@Param("idUser") int idUser, @Param("idGroup") int idGroup, @Param("idThesaurus") String idThesaurus);

    @Query("""
        SELECT rg.role.id
        FROM UserRoleGroup rg
        JOIN UserGroupThesaurus ugt ON rg.group.id = ugt.idGroup
        WHERE rg.user.id = :idUser AND rg.group.id = :idGroup AND ugt.idThesaurus = :idThesaurus
    """)
    Optional<Integer> findRoleFromGroup(@Param("idUser") int idUser, @Param("idGroup") int idGroup, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE UserRoleOnlyOn t SET t.thesaurus.idThesaurus = :newIdThesaurus WHERE t.thesaurus.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);
}
