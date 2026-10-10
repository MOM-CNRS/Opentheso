package fr.cnrs.opentheso.v2.stats.persistence;

import fr.cnrs.opentheso.v2.stats.model.StatEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class StatEventCommandRepository {

    private static final String INSERT_SQL = """
            INSERT INTO stat_log_event (
                event_type, event_time, thesaurus_id, thesaurus_label,
                concept_id, concept_label, lang,
                collection_id, collection_label,
                url, http_method,
                searched_term, selected_term, nb_results
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public StatEventCommandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            StatEventType type,
            LocalDateTime eventTime,
            String thesaurusId,
            String thesaurusLabel,
            String conceptId,
            String conceptLabel,
            String lang,
            String collectionId,
            String collectionLabel,
            String url,
            String httpMethod,
            String searchedTerm,
            String selectedTerm,
            Integer nbResults
    ) {
        jdbcTemplate.update(
                INSERT_SQL,
                type.name(),
                StatJdbc.ts(eventTime),
                StatJdbc.blankToNull(thesaurusId),
                StatJdbc.blankToNull(thesaurusLabel),
                StatJdbc.blankToNull(conceptId),
                StatJdbc.blankToNull(conceptLabel),
                StatJdbc.blankToNull(lang),
                StatJdbc.blankToNull(collectionId),
                StatJdbc.blankToNull(collectionLabel),
                StatJdbc.blankToNull(url),
                StatJdbc.blankToNull(httpMethod),
                StatJdbc.blankToNull(searchedTerm),
                StatJdbc.blankToNull(selectedTerm),
                nbResults
        );
    }
}
