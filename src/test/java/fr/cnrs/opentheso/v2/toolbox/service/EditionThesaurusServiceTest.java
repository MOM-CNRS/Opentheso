package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.fixtures.ToolboxTestFixtures;
import fr.cnrs.opentheso.v2.toolbox.session.EditionThesaurusLegacySupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditionThesaurusServiceTest {

    @Mock
    private EditionQueryRepository editionQueryRepository;
    @Mock
    private EditionThesaurusLegacySupport editionThesaurusLegacySupport;

    private EditionThesaurusService service;

    @BeforeEach
    void setUp() {
        service = new EditionThesaurusService(editionQueryRepository, editionThesaurusLegacySupport);
        ReflectionTestUtils.setField(service, "workLanguage", "fr");
    }

    @Test
    void listAdminThesauri_mapsRows() {
        when(editionQueryRepository.findAdminThesauriForUser(5, false, "fr"))
                .thenReturn(List.of(ToolboxTestFixtures.sampleThesaurusRow()));

        var result = service.listAdminThesauri(5, false);

        assertEquals(1, result.size());
        assertEquals("TH1", result.get(0).id());
        assertEquals("Thésaurus test", result.get(0).title());
        assertTrue(result.get(0).privateThesaurus() == false);
    }

    @Test
    void loadStatistics_aggregatesCounts() {
        when(editionQueryRepository.countAllConceptStats("TH1")).thenReturn(new int[]{10, 2, 1});

        var stats = service.loadStatistics("TH1");

        assertEquals(10, stats.conceptCount());
        assertEquals(2, stats.candidateCount());
        assertEquals(1, stats.deprecatedCount());
    }

    @Test
    void deleteThesaurus_deletesRightsAndThesaurus() {
        when(editionThesaurusLegacySupport.deleteThesaurus("TH1")).thenReturn(true);

        service.deleteThesaurus("TH1", false);

        verify(editionThesaurusLegacySupport).deleteRights("TH1");
        verify(editionThesaurusLegacySupport).deleteThesaurus("TH1");
        verify(editionThesaurusLegacySupport, never()).deleteAllHandleIds("TH1");
    }

    @Test
    void deleteThesaurus_deletesPerennialIdentifiersWhenRequested() {
        when(editionThesaurusLegacySupport.deleteThesaurus("TH1")).thenReturn(true);

        service.deleteThesaurus("TH1", true);

        verify(editionThesaurusLegacySupport).deleteAllHandleIds("TH1");
    }

    @Test
    void deleteThesaurus_requiresThesaurusId() {
        assertThrows(InvalidToolboxDataException.class, () -> service.deleteThesaurus(" ", false));
    }

    @Test
    void deleteThesaurus_failsWhenDeletionReturnsFalse() {
        when(editionThesaurusLegacySupport.deleteThesaurus("TH1")).thenReturn(false);

        assertThrows(InvalidToolboxDataException.class, () -> service.deleteThesaurus("TH1", false));
    }
}
