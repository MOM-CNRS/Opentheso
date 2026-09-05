package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public class PasswordResetCommandRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void invalidateActiveTokens(int userId) {
        entityManager.createNativeQuery("""
                        UPDATE password_reset_token
                        SET used = true
                        WHERE id_user = :userId
                          AND used = false
                        """)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
    }

    @Transactional
    public void insertToken(int userId, String token, LocalDateTime expiresAt) {
        entityManager.createNativeQuery("""
                        INSERT INTO password_reset_token (token, id_user, expires_at, used)
                        VALUES (:token, :userId, :expiresAt, false)
                        """)
                .setParameter("token", token)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("expiresAt", expiresAt)
                .executeUpdate();
    }
}
