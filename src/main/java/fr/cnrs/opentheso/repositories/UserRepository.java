package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.users.NodeUserGroupUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findAllByUsername(String username);

    Optional<User> findByApiKey(String apiKey);

    Optional<User> findByMail(String mail);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.password = :password")
    Optional<User> findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    @Query("SELECT new fr.cnrs.opentheso.models.users.NodeUserGroupUser(u.id, u.username, -1, '', 0, '') " +
            "FROM User u " +
            "WHERE u.isSuperAdmin != true " +
            "AND u.id NOT IN (SELECT urg.user.id FROM UserRoleGroup urg) " +
            "ORDER BY LOWER(u.username)")
    List<NodeUserGroupUser> getAllGroupUserWithoutGroup();

    @Query("SELECT new fr.cnrs.opentheso.models.users.NodeUserGroupUser(" +
            "u.id, u.username, -1, '', 1, 'SuperAdmin') " +
            "FROM User u " +
            "WHERE u.isSuperAdmin = true " +
            "ORDER BY LOWER(u.username)")
    List<NodeUserGroupUser> getAllUsersSuperadmin();

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.mail) LIKE LOWER(CONCAT('%', :mail, '%')) AND LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
    """)
    List<User> searchByMailAndUsername(@Param("mail") String mail, @Param("username") String username);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
    """)
    List<User> findAllByUsernameLikeIgnoreCase(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<User> findByUsername(@Param("username") String username);
}
