package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.SsoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SsoTokenRepository extends JpaRepository<SsoToken, UUID> {

    // Nettoyage des tokens expirés (à appeler via un @Scheduled)
    @Modifying
    @Transactional
    @Query("DELETE FROM SsoToken t WHERE t.expiresAt < :now OR t.used = true")
    void deleteExpiredAndUsedTokens(LocalDateTime now);
}
