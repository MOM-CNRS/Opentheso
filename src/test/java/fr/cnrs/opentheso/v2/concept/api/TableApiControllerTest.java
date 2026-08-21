package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.model.ConceptTableRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptTableRowsResponse;
import fr.cnrs.opentheso.v2.concept.service.ConceptTableConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableApiControllerTest {

    @Mock
    private ConceptTableConsultationService conceptTableConsultationService;

    private TableApiController controller;

    @BeforeEach
    void setUp() {
        controller = new TableApiController(conceptTableConsultationService);
    }

    @Test
    void tableRows_usesQueryParams() {
        var expected = new ConceptTableRowsResponse(List.of(
                new ConceptTableRow("C1", "Adobe", "valide", "Normal", "Concept", "N1", "Racine", "", "")
        ), false);
        when(conceptTableConsultationService.loadRows("TH1", "en")).thenReturn(expected);

        var response = controller.tableRows("TH1", "en");

        assertEquals(expected, response);
        verify(conceptTableConsultationService).loadRows("TH1", "en");
    }

    @Test
    void tableRows_defaultsLangToFr() {
        when(conceptTableConsultationService.loadRows("TH1", "fr"))
                .thenReturn(new ConceptTableRowsResponse(List.of(), false));

        controller.tableRows("TH1", null);

        verify(conceptTableConsultationService).loadRows("TH1", "fr");
    }

    @Test
    void tableRows_returnsEmptyWhenNoThesaurus() {
        var response = controller.tableRows(" ", null);

        assertTrue(response.rows().isEmpty());
        verifyNoInteractions(conceptTableConsultationService);
    }
}
