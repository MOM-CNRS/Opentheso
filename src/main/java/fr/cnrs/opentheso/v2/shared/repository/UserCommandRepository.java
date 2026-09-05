package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.UserSearchRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class UserCommandRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean existsByUsernameIgnoreCase(String username) {
        String sql = """
                SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(username) = LOWER(:username))
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USERNAME, username)
                .getSingleResult());
    }

    public boolean existsByMailIgnoreCase(String mail) {
        String sql = """
                SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(mail) = LOWER(:mail))
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("mail", mail)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public String findInstitution(int userId) {
        String sql = "SELECT institution FROM users WHERE id_user = :userId";
        List<Object> result = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .getResultList();
        return result.isEmpty() ? null : (String) result.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<UserSearchRow> searchByUsernameLike(String username, int limit) {
        String sql = """
                SELECT id_user, username, mail
                FROM users
                WHERE LOWER(username) LIKE LOWER(:pattern)
                  AND issuperadmin = false
                ORDER BY LOWER(username)
                LIMIT :limit
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.PATTERN, "%" + username + "%")
                .setParameter(NativeQueryParams.LIMIT, limit)
                .getResultList();
        return mapUserSearchRows(rows);
    }

    /**
     * Annuaire de tous les comptes (y compris super-admin), filtré par nom ou e-mail.
     * Une requête vide renvoie les premiers utilisateurs par ordre alphabétique.
     */
    @SuppressWarnings("unchecked")
    public List<UserSearchRow> searchDirectory(String query, int limit) {
        String q = query == null ? "" : query.trim();
        int cap = Math.min(Math.max(limit, 1), 200);
        String sql;
        if (q.isEmpty()) {
            sql = """
                    SELECT id_user, username, mail
                    FROM users
                    WHERE username IS NOT NULL AND BTRIM(username) <> ''
                    ORDER BY LOWER(username)
                    LIMIT :limit
                    """;
        } else {
            sql = """
                    SELECT id_user, username, mail
                    FROM users
                    WHERE username IS NOT NULL AND BTRIM(username) <> ''
                      AND (
                        LOWER(username) LIKE LOWER(:pattern)
                        OR LOWER(COALESCE(mail, '')) LIKE LOWER(:pattern)
                      )
                    ORDER BY LOWER(username)
                    LIMIT :limit
                    """;
        }
        var nativeQuery = entityManager.createNativeQuery(sql).setParameter(NativeQueryParams.LIMIT, cap);
        if (!q.isEmpty()) {
            nativeQuery.setParameter(NativeQueryParams.PATTERN, "%" + q + "%");
        }
        return mapUserSearchRows(nativeQuery.getResultList());
    }

    private static List<UserSearchRow> mapUserSearchRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new UserSearchRow(
                        ((Number) row[0]).intValue(),
                        (String) row[1],
                        (String) row[2]
                ))
                .toList();
    }

    @Transactional
    public int createUser(
            String username,
            String mail,
            String password,
            boolean alertMail,
            String institution,
            boolean active,
            boolean passToModify,
            boolean verified
    ) {
        String sql = """
                INSERT INTO users (
                    username, password, mail, alertmail, issuperadmin, active,
                    passtomodify, verified, key_never_expire, isservice_account, rgpd_consent, institution
                )
                VALUES (
                    :username, :password, :mail, :alertMail, false, :active,
                    :passToModify, :verified, false, false, true, :institution
                )
                RETURNING id_user
                """;
        Number id = (Number) entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USERNAME, username)
                .setParameter("password", password != null ? password : "")
                .setParameter("mail", mail)
                .setParameter("alertMail", alertMail)
                .setParameter("active", active)
                .setParameter("passToModify", passToModify)
                .setParameter("verified", verified)
                .setParameter("institution", institution)
                .getSingleResult();
        return id.intValue();
    }

    @Transactional
    public void updateUserProfile(
            int userId,
            String username,
            String mail,
            boolean alertMail,
            String institution,
            boolean active
    ) {
        entityManager.createNativeQuery("""
                        UPDATE users
                        SET username = :username,
                            mail = :mail,
                            alertmail = :alertMail,
                            institution = :institution,
                            active = :active
                        WHERE id_user = :userId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.USERNAME, username)
                .setParameter("mail", mail)
                .setParameter("alertMail", alertMail)
                .setParameter("institution", institution)
                .setParameter("active", active)
                .executeUpdate();
    }

    @Transactional
    public void updatePassword(int userId, String encodedPassword) {
        entityManager.createNativeQuery("""
                        UPDATE users
                        SET password = :password, passtomodify = false
                        WHERE id_user = :userId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("password", encodedPassword)
                .executeUpdate();
    }

    /**
     * Persiste l'autorisation API ({@code key_never_expire} / {@code key_expires_at}).
     * Si {@code authorized} est faux, la clé stockée est effacée.
     */
    @Transactional
    public void updateApiKeySettings(int userId, boolean authorized, boolean keyNeverExpire, java.time.LocalDate keyExpiresAt) {
        entityManager.createNativeQuery("""
                        UPDATE users
                        SET key_never_expire = :keyNeverExpire,
                            key_expires_at = :keyExpiresAt,
                            apikey = CASE WHEN :authorized THEN apikey ELSE NULL END
                        WHERE id_user = :userId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("authorized", authorized)
                .setParameter("keyNeverExpire", keyNeverExpire)
                .setParameter("keyExpiresAt", keyExpiresAt)
                .executeUpdate();
    }

    @Transactional
    public void deleteUserCascade(int userId) {
        entityManager.createNativeQuery("DELETE FROM password_reset_token WHERE id_user = :userId")
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_role_only_on WHERE id_user = :userId")
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_role_group WHERE id_user = :userId")
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users WHERE id_user = :userId")
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
    }

    @Transactional
    public void setSuperAdmin(int userId, boolean superAdmin) {
        entityManager.createNativeQuery("""
                        UPDATE users SET issuperadmin = :superAdmin WHERE id_user = :userId
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("superAdmin", superAdmin)
                .executeUpdate();
    }
}
