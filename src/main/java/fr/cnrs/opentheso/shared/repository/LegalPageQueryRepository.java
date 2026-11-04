package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.shared.model.LegalPageContent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LegalPageQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<LegalPageContent> find(String code, String lang) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT title, htmlcode
                        FROM legal_page
                        WHERE code = :code AND lang = :lang
                        """)
                .setParameter("code", code)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        String title = row[0] == null ? "" : String.valueOf(row[0]);
        String html = row.length > 1 && row[1] != null ? String.valueOf(row[1]) : "";
        return Optional.of(new LegalPageContent(title, html));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> findTitles(String lang) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT code, title
                        FROM legal_page
                        WHERE lang = :lang
                        """)
                .setParameter("lang", lang)
                .getResultList();
        Map<String, String> titles = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            titles.put(String.valueOf(row[0]), row[1] == null ? "" : String.valueOf(row[1]));
        }
        return titles;
    }

    @Transactional
    public void upsert(String code, String lang, String title, String html) {
        entityManager.createNativeQuery("""
                        INSERT INTO legal_page (code, lang, title, htmlcode)
                        VALUES (:code, :lang, :title, :html)
                        ON CONFLICT (code, lang)
                        DO UPDATE SET title = EXCLUDED.title, htmlcode = EXCLUDED.htmlcode
                        """)
                .setParameter("code", code)
                .setParameter("lang", lang)
                .setParameter("title", title)
                .setParameter("html", html)
                .executeUpdate();
    }
}
