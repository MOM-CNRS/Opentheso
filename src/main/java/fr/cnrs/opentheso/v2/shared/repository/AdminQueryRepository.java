package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.AdminThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminUserRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AdminQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<AdminUserRow> findAllUsers() {
        String sql = """
                SELECT u.id_user, u.username, g.id_group, g.label_group, r.id, r.name
                FROM user_role_group urg
                JOIN users u ON urg.id_user = u.id_user
                JOIN user_group_label g ON urg.id_group = g.id_group
                JOIN roles r ON urg.id_role = r.id
                UNION ALL
                SELECT u.id_user, u.username, -1, '', 0, ''
                FROM users u
                WHERE COALESCE(u.issuperadmin, false) = false
                  AND NOT EXISTS (SELECT 1 FROM user_role_group urg WHERE urg.id_user = u.id_user)
                UNION ALL
                SELECT u.id_user, u.username, -1, '', 1, 'superAdmin'
                FROM users u
                WHERE COALESCE(u.issuperadmin, false) = true
                ORDER BY LOWER(username)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream().map(this::toAdminUserRow).toList();
    }

    @SuppressWarnings("unchecked")
    public List<AdminThesaurusRow> findAllThesauri(String lang) {
        String sql = """
                SELECT
                    t.id_thesaurus,
                    COALESCE(tl_sub.title, t.id_thesaurus) AS thesaurus_title,
                    COALESCE(ugt.id_group, -1) AS id_group,
                    COALESCE(ugl.label_group, '') AS label_group,
                    COALESCE(t."private", false) AS is_private,
                    t.created
                FROM thesaurus t
                LEFT JOIN user_group_thesaurus ugt ON ugt.id_thesaurus = t.id_thesaurus
                LEFT JOIN user_group_label ugl ON ugl.id_group = ugt.id_group
                LEFT JOIN LATERAL (
                    SELECT tl.title
                    FROM thesaurus_label tl
                    LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                    WHERE tl.id_thesaurus = t.id_thesaurus
                      AND tl.lang = COALESCE(p.source_lang, :lang)
                    LIMIT 1
                ) tl_sub ON true
                ORDER BY t.created DESC NULLS LAST, LOWER(t.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream().map(this::toAdminThesaurusRow).toList();
    }

    private AdminUserRow toAdminUserRow(Object[] row) {
        return new AdminUserRow(
                ((Number) row[0]).intValue(),
                (String) row[1],
                ((Number) row[2]).intValue(),
                row[3] != null ? (String) row[3] : "",
                ((Number) row[4]).intValue(),
                row[5] != null ? (String) row[5] : ""
        );
    }

    private AdminThesaurusRow toAdminThesaurusRow(Object[] row) {
        LocalDateTime createdAt = null;
        if (row[5] instanceof Timestamp timestamp) {
            createdAt = timestamp.toLocalDateTime();
        } else if (row[5] instanceof LocalDateTime localDateTime) {
            createdAt = localDateTime;
        }
        return new AdminThesaurusRow(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).intValue(),
                row[3] != null ? (String) row[3] : "",
                row[4] != null && (Boolean) row[4],
                createdAt
        );
    }
}
