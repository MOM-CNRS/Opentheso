package fr.cnrs.opentheso.v2.concept.search.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests SQL réels contre une base PostgreSQL Testcontainers.
 * Vérifie que les filtres AND status != 'CA' dans les requêtes natives
 * fonctionnent correctement — ce que les tests unitaires avec mocks ne peuvent pas couvrir.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConceptSearchQueryRepository.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Sql("/sql/schema-minimal.sql")
@Sql("/sql/data-ca-filter.sql")
class ConceptSearchQueryRepositorySqlIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ConceptSearchQueryRepository repository;

    // -------------------------------------------------------------------------
    // Filtre status != 'CA' sur findStatusByIds
    // -------------------------------------------------------------------------

    @Test
    void findStatusByIds_includesAllStatuses_caAndActive() {
        // findStatusByIds retourne tous les statuts — le filtre CA est en amont (findConceptIds)
        // ou en aval (hydrateAll skip CA). Ce test vérifie que la requête renvoie bien CA.
        Map<String, String> statuses = repository.findStatusByIds(
                List.of("C_ACTIVE", "C_CA", "C_DEP"), "TH1");

        assertEquals(3, statuses.size());
        assertEquals("D",   statuses.get("C_ACTIVE"));
        assertEquals("CA",  statuses.get("C_CA"));
        assertEquals("DEP", statuses.get("C_DEP"));
    }

    @Test
    void findStatusByIds_unknownConcept_notInResult() {
        Map<String, String> statuses = repository.findStatusByIds(
                List.of("UNKNOWN"), "TH1");

        assertTrue(statuses.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Filtre status != 'CA' sur findBroadersByIds
    // -------------------------------------------------------------------------

    @Test
    void findBroadersByIds_excludesCaBroader() {
        // C_CHILD a deux parents : B_ACTIVE (D) et B_CA (CA)
        // Seul B_ACTIVE doit apparaître dans le résultat
        Map<String, List<String>> broaders = repository.findBroadersByIds(
                List.of("C_CHILD"), "TH1", "fr");

        List<String> childBroaders = broaders.getOrDefault("C_CHILD", List.of());
        assertEquals(1, childBroaders.size(),
                "CA broader must be excluded from results");
        assertEquals("Terme actif parent", childBroaders.get(0));
    }

    @Test
    void findBroadersByIds_conceptWithNoBroader_returnsEmpty() {
        Map<String, List<String>> broaders = repository.findBroadersByIds(
                List.of("C_ACTIVE"), "TH1", "fr");

        assertTrue(broaders.getOrDefault("C_ACTIVE", List.of()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // findPreferredLabelsByIds
    // -------------------------------------------------------------------------

    @Test
    void findPreferredLabelsByIds_returnsLabelForActiveConcept() {
        Map<String, String> labels = repository.findPreferredLabelsByIds(
                List.of("C_ACTIVE"), "TH1", "fr");

        assertEquals("Terme actif", labels.get("C_ACTIVE"));
    }

    @Test
    void findPreferredLabelsByIds_returnsLabelForCaConcept() {
        // Le label existe même pour un CA — l'exclusion est faite au niveau de hydrateAll
        Map<String, String> labels = repository.findPreferredLabelsByIds(
                List.of("C_CA"), "TH1", "fr");

        assertEquals("Concept candidat", labels.get("C_CA"));
    }

    // -------------------------------------------------------------------------
    // Chunking — vérifie que les listes > CHUNK_SIZE sont traitées correctement
    // -------------------------------------------------------------------------

    @Test
    void findStatusByIds_withLargeList_handlesChunking() {
        // Génère une liste de 600 IDs dont seul C_ACTIVE existe réellement en BD
        List<String> ids = new java.util.ArrayList<>();
        ids.add("C_ACTIVE");
        for (int i = 0; i < 599; i++) {
            ids.add("FAKE_" + i);
        }

        Map<String, String> statuses = repository.findStatusByIds(ids, "TH1");

        assertEquals(1, statuses.size(), "Only existing concept must be returned");
        assertEquals("D", statuses.get("C_ACTIVE"));
    }
}
