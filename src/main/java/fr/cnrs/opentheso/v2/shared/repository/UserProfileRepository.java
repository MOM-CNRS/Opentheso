package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserEntity, Integer> {

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
        FROM V2User u
        WHERE LOWER(u.username) = LOWER(:username) AND u.id <> :userId
    """)
    boolean existsByUsernameIgnoreCaseExcludingId(@Param("username") String username, @Param("userId") Integer userId);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
        FROM V2User u
        WHERE LOWER(u.mail) = LOWER(:mail) AND u.id <> :userId
    """)
    boolean existsByMailIgnoreCaseExcludingId(@Param("mail") String mail, @Param("userId") Integer userId);
}
