package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke local contre {@code opentheso6} : valide les {@code CALL} natifs
 * {@code opentheso_add_new_concept} / {@code opentheso_add_external_images}
 * (binding dates + signature images) après le correctif Hibernate {@code @Procedure}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "fr.cnrs.opentheso.entites")
@EnableJpaRepositories(basePackageClasses = {ConceptRepository.class, ImagesRepository.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/opentheso6",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false"
})
class OpenthesoProcedureImportLocalIT {

    private static final String THESAURUS_ID = "th_smoke_proc";
    private static final String CONCEPT_ID = "smoke_c1";

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void requireLocalDatabase() {
        Assumptions.assumeTrue(isLocalDbReachable(), "PostgreSQL local opentheso6 indisponible — smoke ignoré");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM external_images WHERE id_thesaurus = ?", THESAURUS_ID);
        jdbcTemplate.update("DELETE FROM preferred_term WHERE id_thesaurus = ?", THESAURUS_ID);
        jdbcTemplate.update("DELETE FROM term WHERE id_thesaurus = ?", THESAURUS_ID);
        jdbcTemplate.update("DELETE FROM concept WHERE id_thesaurus = ?", THESAURUS_ID);
    }

    @Test
    void addNewConcept_withCalendarDates_succeeds() {
        var created = V2Dates.toSqlDate(
                LocalDate.of(2026, 2, 27).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var modified = V2Dates.toSqlDate(
                LocalDate.of(2026, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        assertDoesNotThrow(() -> conceptRepository.addNewConcept(
                THESAURUS_ID,
                CONCEPT_ID,
                1,
                "",
                "concept",
                "",
                "",
                true,
                "",
                "",
                "smoke label@@fr",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                created,
                modified,
                null
        ));

        assertTrue(conceptExistsViaJdbc());
        assertEquals("2026-02-27", created.toString());
        assertEquals("2026-09-01", modified.toString());
    }

    @Test
    void addExternalImages_positionalCall_succeeds() {
        jdbcTemplate.update("""
                INSERT INTO concept (id_concept, id_thesaurus, status, concept_type, top_concept, creator, contributor)
                VALUES (?, ?, '', 'concept', true, 1, 1)
                ON CONFLICT DO NOTHING
                """, CONCEPT_ID, THESAURUS_ID);

        String imagesStr = "Paris@@WikiData@@https://example.com/paris.jpg@@Firas";

        assertDoesNotThrow(() -> imagesRepository.addExternalImages(THESAURUS_ID, CONCEPT_ID, 1, imagesStr));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM external_images WHERE id_thesaurus = ? AND id_concept = ?",
                Integer.class,
                THESAURUS_ID,
                CONCEPT_ID
        );
        assertEquals(1, count);
    }

    private boolean conceptExistsViaJdbc() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM concept WHERE id_thesaurus = ? AND id_concept = ?",
                Integer.class,
                THESAURUS_ID,
                CONCEPT_ID
        );
        return count != null && count > 0;
    }

    private static boolean isLocalDbReachable() {
        try (var ignored = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/opentheso6", "postgres", "postgres")) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
