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
                        ON CONFLICT (id_user, id_group)
                        DO UPDATE SET id_role = EXCLUDED.id_role
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.ROLE_ID, roleId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
    }

    @Transactional
    public void updateProjectRole(int userId, int roleId, int projectId) {
        entityManager.createNativeQuery("""
                        UPDATE user_role_group
                        SET id_role = :roleId
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.ROLE_ID, roleId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
    }

    @Transactional
    public void deleteProjectRole(int userId, int projectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_role_group
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
    }

    @Transactional
    public void deleteAllLimitedRoles(int userId, int projectId) {
        doDeleteAllLimitedRoles(userId, projectId);
    }

    private void doDeleteAllLimitedRoles(int userId, int projectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_role_only_on
                        WHERE id_user = :userId AND id_group = :projectId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
    }

    @Transactional
    public void assignLimitedRole(int userId, int roleId, int projectId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        INSERT INTO user_role_only_on (id_user, id_role, id_group, id_theso)
                        VALUES (:userId, :roleId, :projectId, :thesaurusId)
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.ROLE_ID, roleId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.ROLE_ID, roleId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void replaceLimitedRoles(int userId, int roleId, int projectId, List<String> thesaurusIds) {
        doDeleteAllLimitedRoles(userId, projectId);
        if (thesaurusIds.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder(
                "INSERT INTO user_role_only_on (id_user, id_role, id_group, id_theso) VALUES "
        );
        for (int i = 0; i < thesaurusIds.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(:userId, :roleId, :projectId, :t").append(i).append(")");
        }
        var query = entityManager.createNativeQuery(sql.toString())
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.ROLE_ID, roleId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId);
        for (int i = 0; i < thesaurusIds.size(); i++) {
            query.setParameter("t" + i, thesaurusIds.get(i));
        }
        query.executeUpdate();
    }

    public boolean isThesaurusInProject(String thesaurusId, int projectId) {
        String sql = """
                SELECT EXISTS(
                    SELECT 1 FROM user_group_thesaurus
                    WHERE id_thesaurus = :thesaurusId AND id_group = :projectId
                )
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .getSingleResult());
    }

    @Transactional
    public void moveThesaurus(String thesaurusId, int targetProjectId) {
        entityManager.createNativeQuery("""
                        DELETE FROM user_group_thesaurus WHERE id_thesaurus = :thesaurusId
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO user_group_thesaurus (id_group, id_thesaurus)
                        VALUES (:projectId, :thesaurusId)
                        """)
                .setParameter(NativeQueryParams.PROJECT_ID, targetProjectId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void deleteProjectMemberships(int projectId) {
        entityManager.createNativeQuery("DELETE FROM user_role_group WHERE id_group = :projectId")
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_role_only_on WHERE id_group = :projectId")
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_group_thesaurus WHERE id_group = :projectId")
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
                .executeUpdate();
    }
}
