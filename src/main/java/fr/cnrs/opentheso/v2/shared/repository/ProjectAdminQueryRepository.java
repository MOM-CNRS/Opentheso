package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.AssignableRoleRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectLimitedMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ProjectAdminQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ProjectSummaryRow> findProjectsByLabel(String label) {
        String sql = """
                SELECT id_group, label_group
                FROM user_group_label
                WHERE LOWER(label_group) LIKE LOWER(CONCAT('%', :label, '%'))
                ORDER BY LOWER(label_group)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("label", label)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectSummaryRow(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectSummaryRow> findAllProjects() {
        String sql = """
                SELECT id_group, label_group
                FROM user_group_label
                ORDER BY LOWER(label_group)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream()
                .map(row -> new ProjectSummaryRow(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectSummaryRow> findAccessibleProjectsForUser(int userId) {
        String sql = """
                SELECT DISTINCT ugl.id_group, ugl.label_group
                FROM user_group_label ugl
                WHERE ugl.id_group IN (
                    SELECT urg.id_group FROM user_role_group urg WHERE urg.id_user = :userId
                    UNION
                    SELECT uro.id_group FROM user_role_only_on uro WHERE uro.id_user = :userId
                )
                ORDER BY LOWER(ugl.label_group)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectSummaryRow(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }

    public Optional<Integer> findCallerRoleOnProject(int userId, int projectId) {
        String sql = """
                SELECT urg.id_role
                FROM user_role_group urg
                WHERE urg.id_user = :userId AND urg.id_group = :projectId
                LIMIT 1
                """;
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("projectId", projectId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0).intValue());
    }

    public boolean hasAdminRoleOnAnyProject(int userId, int managerRoleId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
                FROM user_role_group
                WHERE id_user = :userId AND id_role < :managerRoleId
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("managerRoleId", managerRoleId)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public Set<String> findThesauriNotInProject(List<String> thesaurusIds, int projectId) {
        if (thesaurusIds.isEmpty()) {
            return Set.of();
        }
        String sql = """
                SELECT ugt.id_thesaurus
                FROM user_group_thesaurus ugt
                WHERE ugt.id_group = :projectId
                  AND ugt.id_thesaurus IN (:ids)
                """;
        List<String> found = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("ids", thesaurusIds)
                .getResultList();
        Set<String> foundSet = new HashSet<>(found);
        return thesaurusIds.stream()
                .filter(id -> !foundSet.contains(id))
                .collect(Collectors.toSet());
    }

    public boolean isProjectAccessible(int userId, int projectId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
                FROM (
                    SELECT urg.id_group FROM user_role_group urg
                    WHERE urg.id_user = :userId AND urg.id_group = :projectId
                    UNION
                    SELECT uro.id_group FROM user_role_only_on uro
                    WHERE uro.id_user = :userId AND uro.id_group = :projectId
                ) accessible
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("projectId", projectId)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public List<ProjectThesaurusRow> findThesauriOfProject(int projectId, String lang) {
        String sql = """
                SELECT
                    ugt.id_thesaurus,
                    COALESCE(
                        (SELECT tl.title
                         FROM thesaurus_label tl
                         LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                         WHERE tl.id_thesaurus = ugt.id_thesaurus
                           AND tl.lang = COALESCE(p.source_lang, :lang)
                         LIMIT 1),
                        ugt.id_thesaurus
                    ) AS thesaurus_title,
                    COALESCE(t."private", false) AS is_private
                FROM user_group_thesaurus ugt
                JOIN thesaurus t ON t.id_thesaurus = ugt.id_thesaurus
                WHERE ugt.id_group = :projectId
                ORDER BY LOWER(ugt.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectThesaurusRow(
                        (String) row[0],
                        (String) row[1],
                        row[2] != null && (Boolean) row[2]
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectMemberRow> findMembersOfProject(int projectId, int minRoleId) {
        String sql = """
                SELECT u.id_user, u.username, u.active, r.id, r.name
                FROM user_role_group urg
                JOIN users u ON urg.id_user = u.id_user
                JOIN roles r ON urg.id_role = r.id
                WHERE urg.id_group = :projectId
                  AND u.issuperadmin = false
                  AND urg.id_role >= :minRoleId
                ORDER BY LOWER(u.username)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("minRoleId", minRoleId)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectMemberRow(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        row[2] != null && (Boolean) row[2],
                        ((Number) row[3]).intValue(),
                        (String) row[4]
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectLimitedMemberRow> findLimitedMembersOfProject(int projectId, String lang) {
        String sql = """
                SELECT
                    u.id_user,
                    u.username,
                    u.active,
                    r.id,
                    r.name,
                    uroo.id_theso,
                    COALESCE(
                        (SELECT tl.title
                         FROM thesaurus_label tl
                         LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                         WHERE tl.id_thesaurus = uroo.id_theso
                           AND tl.lang = COALESCE(p.source_lang, :lang)
                         LIMIT 1),
                        uroo.id_theso
                    ) AS thesaurus_title
                FROM user_role_only_on uroo
                JOIN users u ON uroo.id_user = u.id_user
                JOIN roles r ON uroo.id_role = r.id
                WHERE uroo.id_group = :projectId
                  AND u.issuperadmin = false
                ORDER BY LOWER(u.username)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectLimitedMemberRow(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        row[2] != null && (Boolean) row[2],
                        ((Number) row[3]).intValue(),
                        (String) row[4],
                        (String) row[5],
                        (String) row[6]
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<AssignableRoleRow> findAssignableRolesFrom(int minRoleId) {
        String sql = """
                SELECT id, name
                FROM roles
                WHERE id >= :minRoleId
                ORDER BY id
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("minRoleId", minRoleId)
                .getResultList();
        return rows.stream()
                .map(row -> new AssignableRoleRow(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }
}
