package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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
}
