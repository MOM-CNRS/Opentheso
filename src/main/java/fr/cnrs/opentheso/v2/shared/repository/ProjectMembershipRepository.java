package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ProjectMembershipRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void assignProjectRole(int userId, int roleId, int projectId) {
        entityManager.createNativeQuery("""
                        INSERT INTO user_role_group (id_user, id_role, id_group)
                        VALUES (:userId, :roleId, :projectId)
                        """)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @Transactional
    public void updateProjectRole(int userId, int roleId, int projectId) {
        entityManager.createNativeQuery("""
                        UPDATE user_role_group
                        SET id_role = :roleId
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @Transactional
    public void deleteProjectRole(int userId, int projectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_role_group
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter("userId", userId)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @Transactional
    public void deleteAllLimitedRoles(int userId, int projectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_role_only_on
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter("userId", userId)
                .setParameter("projectId", projectId)
                .executeUpdate();
    }

    @Transactional
    public void assignLimitedRole(int userId, int roleId, int projectId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        INSERT INTO user_role_only_on (id_user, id_role, id_group, id_theso)
                        VALUES (:userId, :roleId, :projectId, :thesaurusId)
                        """)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("projectId", projectId)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void deleteLimitedRole(int userId, int roleId, int projectId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_role_only_on
                        WHERE id_user = :userId
                          AND id_role = :roleId
                          AND id_group = :projectId
                          AND id_theso = :thesaurusId
                        """)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .setParameter("projectId", projectId)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void replaceLimitedRoles(int userId, int roleId, int projectId, List<String> thesaurusIds) {
        deleteAllLimitedRoles(userId, projectId);
        for (String thesaurusId : thesaurusIds) {
            assignLimitedRole(userId, roleId, projectId, thesaurusId);
        }
    }

    public boolean isThesaurusInProject(String thesaurusId, int projectId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
                FROM user_group_thesaurus
                WHERE id_thesaurus = :thesaurusId AND id_group = :projectId
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("projectId", projectId)
                .getSingleResult());
    }

    @Transactional
    public void moveThesaurus(String thesaurusId, int targetProjectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_group_thesaurus WHERE id_thesaurus = :thesaurusId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO user_group_thesaurus (id_group, id_thesaurus)
                        VALUES (:projectId, :thesaurusId)
                        """)
                .setParameter("projectId", targetProjectId)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void deleteProjectMemberships(int projectId) {
        entityManager.createNativeQuery("DELETE FROM user_role_group WHERE id_group = :projectId")
                .setParameter("projectId", projectId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_role_only_on WHERE id_group = :projectId")
                .setParameter("projectId", projectId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_group_thesaurus WHERE id_group = :projectId")
                .setParameter("projectId", projectId)
                .executeUpdate();
    }
}
