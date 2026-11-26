package fr.cnrs.opentheso.suggestion.persistence;

import fr.cnrs.opentheso.suggestion.model.InstanceFeatures;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InstanceFeatureRepository {

    private static final String SELECT_SQL = """
            SELECT suggestion_enabled, language_visible, rest_api_visible, graph_api_visible, api_v1_enabled
            FROM instance_feature
            WHERE id = 1
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO instance_feature (
                id, suggestion_enabled, language_visible, rest_api_visible, graph_api_visible, api_v1_enabled, updated_at
            ) VALUES (1, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
                suggestion_enabled = EXCLUDED.suggestion_enabled,
                language_visible = EXCLUDED.language_visible,
                rest_api_visible = EXCLUDED.rest_api_visible,
                graph_api_visible = EXCLUDED.graph_api_visible,
                api_v1_enabled = EXCLUDED.api_v1_enabled,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public InstanceFeatureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<InstanceFeatures> find() {
        List<InstanceFeatures> rows = jdbcTemplate.query(SELECT_SQL, (rs, rowNum) -> new InstanceFeatures(
                rs.getBoolean("suggestion_enabled"),
                rs.getBoolean("language_visible"),
                rs.getBoolean("rest_api_visible"),
                rs.getBoolean("graph_api_visible"),
                rs.getBoolean("api_v1_enabled")));
        return rows.stream().findFirst();
    }

    public void save(InstanceFeatures features) {
        jdbcTemplate.update(
                UPSERT_SQL,
                features.suggestionEnabled(),
                features.languageVisible(),
                features.restApiVisible(),
                features.graphApiVisible(),
                features.apiV1Enabled());
    }
}
