package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.UserThesaurusRoleRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.OptionalInt;

@Repository
public class UserRoleQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<UserThesaurusRoleRow> findAllThesaurusRolesForUser(int userId) {
        String sql = """
                SELECT
                    ugl.id_group,
                    ugl.label_group,
                    ugt.id_thesaurus,
                    COALESCE(tl_sub.title, ugt.id_thesaurus) AS thesaurus_title,
                    COALESCE(limited_role.id_role, project_role.id_role) AS id_role
                FROM user_group_label ugl
                JOIN user_group_thesaurus ugt ON ugt.id_group = ugl.id_group
                LEFT JOIN LATERAL (
                    SELECT tl.title
                    FROM thesaurus_label tl
                    LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                    WHERE tl.id_thesaurus = ugt.id_thesaurus
                      AND tl.lang = COALESCE(p.source_lang, 'fr')
                    LIMIT 1
                ) tl_sub ON true
                LEFT JOIN LATERAL (
                    SELECT uro.id_role
                    FROM user_role_only_on uro
                    WHERE uro.id_user = :userId
                      AND uro.id_group = ugl.id_group
                      AND uro.id_theso = ugt.id_thesaurus
                    LIMIT 1
                ) limited_role ON true
                LEFT JOIN LATERAL (
                    SELECT urg.id_role
                    FROM user_role_group urg
                    WHERE urg.id_user = :userId
                      AND urg.id_group = ugl.id_group
                    LIMIT 1
                ) project_role ON true
                WHERE (
                    EXISTS (SELECT 1 FROM user_role_group urg2 WHERE urg2.id_user = :userId AND urg2.id_group = ugl.id_group)
                    OR EXISTS (SELECT 1 FROM user_role_only_on uro2 WHERE uro2.id_user = :userId AND uro2.id_group = ugl.id_group)
                )
                ORDER BY LOWER(ugl.label_group), LOWER(ugt.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .getResultList();

        return rows.stream()
                .filter(row -> row[4] != null)
                .map(row -> new UserThesaurusRoleRow(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).intValue()
                ))
                .toList();
    }

    public OptionalInt findBestRoleId(int userId) {
        String sql = """
                SELECT MIN(role_id) FROM (
                    SELECT id_role AS role_id FROM user_role_group WHERE id_user = :userId
                    UNION ALL
                    SELECT id_role AS role_id FROM user_role_only_on WHERE id_user = :userId
                ) roles
                """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .getSingleResult();
        if (result == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(((Number) result).intValue());
    }
}
