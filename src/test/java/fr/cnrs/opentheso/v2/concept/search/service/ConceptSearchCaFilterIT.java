package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConceptFullQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que les concepts 'CA' (candidats) sont exclus des résultats publics.
 *
 * La règle fonctionnelle : status='CA' = concept en cours de validation,
 * non visible publiquement. Le filtre s'applique à deux niveaux :
 *   1. SQL (WHERE status != 'CA') dans les requêtes de recherche
 *   2. Java (hydrateAll skip CA) comme filet de sécurité défensif
 *
 * Ce test couvre le niveau Java.
 */
@ExtendWith(MockitoExtension.class)
class ConceptSearchCaFilterIT {

    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;

    @Mock
    private ConceptFullQueryRepository conceptFullQueryRepository;

    private ConceptSearchHydrationService hydrationService;

    @BeforeEach
    void setUp() {
        hydrationService = new ConceptSearchHydrationService(
                conceptSearchQueryRepository,
                conceptFullQueryRepository
        );
    }

    @Test
    void hydrateAll_excludesCaConcepts() {
        List<String> ids = List.of("C_ACTIVE", "C_CA", "C_DEP");

        org.mockito.Mockito.when(conceptSearchQueryRepository.findStatusByIds(ids, "TH1"))
                .thenReturn(Map.of(
                        "C_ACTIVE", "D",
                        "C_CA", "CA",
                        "C_DEP", "DEP"
                ));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findPreferredLabelsByIds(ids, "TH1", "fr"))
                .thenReturn(Map.of("C_ACTIVE", "Actif", "C_CA", "Candidat", "C_DEP", "Déprécié"));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findSynonymsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findBroadersByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findRelatedsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());

        List<ConceptSearchResult> results = hydrationService.hydrateAll(ids, "TH1", "fr");

        assertEquals(2, results.size(), "CA concept must be excluded");
        assertTrue(results.stream().noneMatch(r -> "C_CA".equals(r.conceptId())),
                "C_CA must not appear in results");
    }

    @Test
    void hydrateAll_includesDeprecatedConcepts() {
        List<String> ids = List.of("C_DEP");

        org.mockito.Mockito.when(conceptSearchQueryRepository.findStatusByIds(ids, "TH1"))
                .thenReturn(Map.of("C_DEP", "DEP"));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findPreferredLabelsByIds(ids, "TH1", "fr"))
                .thenReturn(Map.of("C_DEP", "Ancien terme"));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findSynonymsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findBroadersByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findRelatedsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());

        List<ConceptSearchResult> results = hydrationService.hydrateAll(ids, "TH1", "fr");

        assertEquals(1, results.size(), "DEP concept must be included");
        assertTrue(results.get(0).deprecated(), "DEP concept must be flagged as deprecated");
    }

    @Test
    void hydrateAll_skipsConceptsNotFoundInDb() {
        List<String> ids = List.of("C_GHOST");

        org.mockito.Mockito.when(conceptSearchQueryRepository.findStatusByIds(ids, "TH1"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findPreferredLabelsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findSynonymsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findBroadersByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findRelatedsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());

        List<ConceptSearchResult> results = hydrationService.hydrateAll(ids, "TH1", "fr");

        assertTrue(results.isEmpty(), "Concept absent from DB must be silently skipped");
    }

    @Test
    void hydrateAll_activeConcept_isNotDeprecated() {
        List<String> ids = List.of("C1");

        org.mockito.Mockito.when(conceptSearchQueryRepository.findStatusByIds(ids, "TH1"))
                .thenReturn(Map.of("C1", "D"));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findPreferredLabelsByIds(ids, "TH1", "fr"))
                .thenReturn(Map.of("C1", "Terme actif"));
        org.mockito.Mockito.when(conceptSearchQueryRepository.findSynonymsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findBroadersByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());
        org.mockito.Mockito.when(conceptSearchQueryRepository.findRelatedsByIds(ids, "TH1", "fr"))
                .thenReturn(Collections.emptyMap());

        List<ConceptSearchResult> results = hydrationService.hydrateAll(ids, "TH1", "fr");

        assertEquals(1, results.size());
        assertFalse(results.get(0).deprecated());
    }
}
