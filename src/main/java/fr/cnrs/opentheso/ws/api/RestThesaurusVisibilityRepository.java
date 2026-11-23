package fr.cnrs.opentheso.ws.api;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Visibilité d'un thésaurus pour les API REST : public, ou rattaché à un projet de l'utilisateur.
 */
@Repository
class RestThesaurusVisibilityRepository {

    private static final String VISIBLE = """
            COALESCE(t."private", false) = false
            OR (
                CAST(:userId AS integer) >= 0
                AND (
                    EXISTS (
                        SELECT 1
                        FROM user_role_group urg
                        JOIN user_group_thesaurus ugt ON ugt.id_group = urg.id_group
                        WHERE urg.id_user = CAST(:userId AS integer)
                          AND ugt.id_thesaurus = t.id_thesaurus
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM user_role_only_on uro
                        JOIN user_group_thesaurus ugt ON ugt.id_group = uro.id_group
                        WHERE uro.id_user = CAST(:userId AS integer)
                          AND ugt.id_thesaurus = t.id_thesaurus
                    )
                )
            )
            """;

    @PersistenceContext
    private EntityManager entityManager;

    Optional<ThesaurusAccessFacts> describe(int userId, String thesaurusId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    COALESCE(t."private", false),
                    EXISTS (
                        SELECT 1
                        FROM user_group_thesaurus ugt
                        WHERE ugt.id_thesaurus = t.id_thesaurus
                    ),
                    EXISTS (
                        SELECT 1
                        FROM user_role_group urg
                        JOIN user_group_thesaurus ugt ON ugt.id_group = urg.id_group
                        WHERE urg.id_user = CAST(:userId AS integer)
                          AND ugt.id_thesaurus = t.id_thesaurus
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM user_role_only_on uro
                        JOIN user_group_thesaurus ugt ON ugt.id_group = uro.id_group
                        WHERE uro.id_user = CAST(:userId AS integer)
                          AND ugt.id_thesaurus = t.id_thesaurus
                    )
                FROM thesaurus t
                WHERE LOWER(BTRIM(t.id_thesaurus)) = LOWER(BTRIM(:thesaurusId))
                """)
                .setParameter("userId", userId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new ThesaurusAccessFacts(asBoolean(row[0]), asBoolean(row[1]), asBoolean(row[2])));
    }

    @SuppressWarnings("unchecked")
    List<String> visibleIds(int userId) {
        return entityManager.createNativeQuery("""
                SELECT t.id_thesaurus
                FROM thesaurus t
                WHERE """ + VISIBLE + """
                ORDER BY t.created DESC NULLS LAST, t.id_thesaurus
                """)
                .setParameter("userId", userId)
                .getResultList();
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    record ThesaurusAccessFacts(boolean isPrivate, boolean attachedToProject, boolean memberOfProject) {
    }
}
