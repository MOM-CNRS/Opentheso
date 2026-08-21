package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class PlatformHomeQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<String> findHomePageHtml(String lang) {
        String sql = """
                SELECT htmlcode
                FROM homepage
                WHERE lang = :lang
                LIMIT 1
                """;
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Transactional
    public boolean upsertHomePageHtml(String lang, String html) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE homepage
                        SET htmlcode = :html
                        WHERE lang = :lang
                        """)
                .setParameter("html", html)
                .setParameter("lang", lang)
                .executeUpdate();
        if (updated > 0) {
            return true;
        }
        entityManager.createNativeQuery("""
                        INSERT INTO homepage (lang, htmlcode)
                        VALUES (:lang, :html)
                        """)
                .setParameter("lang", lang)
                .setParameter("html", html)
                .executeUpdate();
        return true;
    }

    @SuppressWarnings("unchecked")
    public Optional<String> findGoogleAnalyticsCode() {
        List<String> rows = entityManager.createNativeQuery("""
                        SELECT googleanalytics
                        FROM info
                        LIMIT 1
                        """)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Transactional
    public void saveGoogleAnalyticsCode(String code) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE info
                        SET googleanalytics = :code
                        """)
                .setParameter("code", code)
                .executeUpdate();
        if (updated > 0) {
            return;
        }
        entityManager.createNativeQuery("""
                        INSERT INTO info (version_opentheso, version_bdd, googleanalytics)
                        VALUES ('0.0.0', 'xyz', :code)
                        """)
                .setParameter("code", code)
                .executeUpdate();
    }
}
