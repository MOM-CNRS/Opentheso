package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.statistiques.StatistiqueService;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusStatisticsServiceTest {

    @Mock
    private StatistiqueService statistiqueService;
    @Mock
    private ThesaurusService thesaurusService;
    @Mock
    private ConceptService conceptService;
    @Mock
    private EditionQueryRepository editionQueryRepository;

    private ThesaurusStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusStatisticsService(
                statistiqueService,
                thesaurusService,
                conceptService,
                editionQueryRepository
        );
    }

    @Test
    void loadSummary_aggregatesCounts() {
        var lastModification = new Date();
        when(editionQueryRepository.countAllConceptStats("TH1")).thenReturn(new int[]{10, 2, 1});
        when(conceptService.getLastModification("TH1")).thenReturn(lastModification);

        var summary = service.loadSummary("TH1");

        assertEquals(10, summary.counts().conceptCount());
        assertEquals(2, summary.counts().candidateCount());
        assertEquals(1, summary.counts().deprecatedCount());
        assertEquals(lastModification, summary.lastModification());
    }

    @Test
    void loadCollectionStatistics_delegatesToStatistiqueService() {
        var row = GenericStatistiqueData.builder().collection("Collection A").build();
        when(statistiqueService.searchAllCollectionsByThesaurus("TH1", "fr")).thenReturn(List.of(row));

        var result = service.loadCollectionStatistics("TH1", "fr");

        assertEquals(1, result.size());
        assertEquals("Collection A", result.get(0).getCollection());
    }

    @Test
    void exportGenericReport_returnsCsvBytes() {
        var row = GenericStatistiqueData.builder()
                .collection("Collection A")
                .conceptsNbr(3)
                .build();

        byte[] content = service.exportGenericReport(List.of(row));

        assertNotNull(content);
    }

    @Test
    void loadLanguages_delegatesToThesaurusService() {
        var language = new NodeLangTheso();
        language.setCode("fr");
        when(thesaurusService.getAllUsedLanguagesOfThesaurusNode("TH1", "fr")).thenReturn(List.of(language));

        var languages = service.loadLanguages("TH1", "fr");

        assertEquals(1, languages.size());
        assertEquals("fr", languages.get(0).getCode());
    }

    @Test
    void loadConceptStatistics_delegatesToStatistiqueService() {
        var concept = ConceptStatisticData.builder().idConcept("C1").build();
        when(statistiqueService.searchAllConceptsByThesaurus("TH1", "fr", null, null, "", "100"))
                .thenReturn(List.of(concept));

        var result = service.loadConceptStatistics("TH1", "fr", null, null, "", "100");

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).getIdConcept());
    }

    @Test
    void loadCollections_delegatesToStatistiqueService() {
        var collection = DomaineDto.builder().id("G1").name("Domaine").build();
        when(statistiqueService.getListGroupes("TH1", "fr")).thenReturn(List.of(collection));

        var collections = service.loadCollections("TH1", "fr");

        assertEquals(1, collections.size());
        assertEquals("Domaine", collections.get(0).getName());
    }
}
