package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.GraphViewListRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GraphViewQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<GraphViewListRow> findViewsByUserId(int userId) {
        String sql = """
                SELECT gv.id,
                       gv.name,
                       gv.description,
                       COALESCE(
                           json_agg(
                               json_build_object(
                                   'thesaurusId', ec.top_concept_thesaurus_id,
                                   'conceptId', ec.top_concept_id
                               )
                           ) FILTER (WHERE ec.top_concept_thesaurus_id IS NOT NULL),
                           '[]'::json
                       ) AS exports_json
                FROM graph_view gv
                LEFT JOIN graph_view_exported_concept_branch ec ON ec.graph_view_id = gv.id
                WHERE gv.id_user = :userId
                GROUP BY gv.id, gv.name, gv.description
                ORDER BY lower(gv.name)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream().map(this::mapListRow).toList();
    }

    public Optional<GraphViewListRow> findViewById(int viewId) {
        String sql = """
                SELECT gv.id,
                       gv.name,
                       gv.description,
                       COALESCE(
                           json_agg(
                               json_build_object(
                                   'thesaurusId', ec.top_concept_thesaurus_id,
                                   'conceptId', ec.top_concept_id
                               )
                           ) FILTER (WHERE ec.top_concept_thesaurus_id IS NOT NULL),
                           '[]'::json
                       ) AS exports_json
                FROM graph_view gv
                LEFT JOIN graph_view_exported_concept_branch ec ON ec.graph_view_id = gv.id
                WHERE gv.id = :viewId
                GROUP BY gv.id, gv.name, gv.description
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("viewId", viewId)
                .getResultList();
        return rows.stream().findFirst().map(this::mapListRow);
    }

    public boolean isViewOwnedByUser(int viewId, int userId) {
        String sql = """
                SELECT EXISTS(
                    SELECT 1 FROM graph_view WHERE id = :viewId AND id_user = :userId
                )
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("viewId", viewId)
                .setParameter("userId", userId)
                .getSingleResult());
    }

    private GraphViewListRow mapListRow(Object[] row) {
        return new GraphViewListRow(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                row[3] == null ? "[]" : row[3].toString()
        );
    }
}
