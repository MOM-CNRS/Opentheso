package fr.cnrs.opentheso.v2.publicapi.thesaurus.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class ThesaurusPublicQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ConceptLinkItem> findFlatConceptList(String thesaurusId, String lang) {
        String sql = """
                SELECT c.id_concept, t.lexical_value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.id_thesaurus = :thesaurusId
                  AND t.lang = :lang
                  AND c.status != 'CA'
                ORDER BY t.lexical_value
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> new ConceptLinkItem(
                        row[0] != null ? (String) row[0] : "",
                        row[1] != null ? (String) row[1] : ""
                ))
                .filter(item -> StringUtils.isNotBlank(item.conceptId()))
                .toList();
    }
}
