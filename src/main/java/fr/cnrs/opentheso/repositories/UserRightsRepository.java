package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRightsRepository extends JpaRepository<User, Integer> {

    @Query(value = """
        SELECT 
            CASE 
                WHEN u.issuperadmin THEN 1
                ELSE COALESCE(oo.id_role, urg.id_role)
            END AS id_role
        FROM users u
        LEFT JOIN user_role_only_on oo 
            ON oo.id_user = u.id_user AND oo.id_theso = :idTheso
        LEFT JOIN user_group_thesaurus ugt 
            ON ugt.id_thesaurus = :idTheso
        LEFT JOIN user_role_group urg 
            ON urg.id_user = u.id_user AND urg.id_group = ugt.id_group
        WHERE u.id_user = :idUser
        """, nativeQuery = true)
    Integer findRoleIdOnTheso(@Param("idUser") int idUser, @Param("idTheso") String idTheso);
}
