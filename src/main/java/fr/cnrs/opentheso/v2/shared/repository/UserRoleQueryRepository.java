package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.UserThesaurusRoleRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                    COALESCE(
                        (SELECT tl.title
                         FROM thesaurus_label tl
                         LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                         WHERE tl.id_thesaurus = ugt.id_thesaurus
                           AND tl.lang = COALESCE(p.source_lang, 'fr')
                         LIMIT 1),
                        ugt.id_thesaurus
                    ) AS thesaurus_title,
                    COALESCE(
                        (SELECT uro.id_role
                         FROM user_role_only_on uro
                         WHERE uro.id_user = :userId
                           AND uro.id_group = ugl.id_group
                           AND uro.id_theso = ugt.id_thesaurus
                         LIMIT 1),
                        (SELECT urg.id_role
                         FROM user_role_group urg
                         JOIN user_group_thesaurus ugt2 ON urg.id_group = ugt2.id_group
                         WHERE urg.id_user = :userId
                           AND urg.id_group = ugl.id_group
                           AND ugt2.id_thesaurus = ugt.id_thesaurus
                         LIMIT 1)
                    ) AS id_role
                FROM user_group_label ugl
                JOIN user_group_thesaurus ugt ON ugt.id_group = ugl.id_group
                WHERE ugl.id_group IN (
                    SELECT urg2.id_group FROM user_role_group urg2 WHERE urg2.id_user = :userId
                    UNION
                    SELECT uro2.id_group FROM user_role_only_on uro2 WHERE uro2.id_user = :userId
                )
                ORDER BY LOWER(ugl.label_group), LOWER(ugt.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
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
}
