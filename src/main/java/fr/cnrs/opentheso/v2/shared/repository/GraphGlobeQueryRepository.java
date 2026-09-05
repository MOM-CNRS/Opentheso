package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class GraphGlobeQueryRepository {

    private static final int DEFAULT_NEIGHBOR_CAP = 120;

    @PersistenceContext
    private EntityManager em;

    /**
     * Points du globe : id, label, statut. Requête légère pour charger tout le thésaurus.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findGlobeConcepts(
            String thesaurusId,
            String lang,
            boolean includeCandidates,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   COALESCE(c.status, '') AS status
            FROM concept c
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE c.id_thesaurus = :thesaurusId
            """);
        if (!includeCandidates) {
            sql.append(" AND c.status != 'CA' ");
        }
        sql.append(" ORDER BY label ");
        Query query = em.createNativeQuery(sql.toString())
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang);
        query.setMaxResults(Math.max(1, limit));
        return query.getResultList();
    }

    /**
     * Voisins BT / NT / RT d'un concept, avec libellé.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findNeighborhood(String thesaurusId, String conceptId, String lang, int limit) {
        Query query = em.createNativeQuery("""
            SELECT hr.id_concept2 AS other_id,
                   COALESCE(t.lexical_value, hr.id_concept2) AS label,
                   hr.role AS role
            FROM hierarchical_relationship hr
            LEFT JOIN preferred_term pt
                ON pt.id_concept = hr.id_concept2
                AND pt.id_thesaurus = hr.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = hr.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND (hr.role LIKE 'BT%' OR hr.role LIKE 'NT%' OR hr.role = 'RT')
            ORDER BY hr.role, label
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter("lang", lang);
        query.setMaxResults(Math.max(1, limit > 0 ? limit : DEFAULT_NEIGHBOR_CAP));
        return query.getResultList();
    }
}
