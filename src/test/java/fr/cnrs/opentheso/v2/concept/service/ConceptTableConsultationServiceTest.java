package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.concept.model.ConceptTableRow;
import fr.cnrs.opentheso.v2.shared.repository.ConceptTableQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTableConsultationServiceTest {

    @Mock
    private ConceptTableQueryRepository conceptTableQueryRepository;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private ConceptTableConsultationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptTableConsultationService(conceptTableQueryRepository, authenticatedUserSource);
    }

    @Test
    void loadRows_mapsConceptAndBroaderPath() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptTableQueryRepository.findTableConceptRows("TH1", "fr", false, 15001)).thenReturn(List.<Object[]>of(
                new Object[]{"C2", "Adobe miniature", "GEN-2", "C", "concept", "Concept", 0, "", ""},
                new Object[]{"C1", "Adobe", "GEN-1", "C", "concept", "", 0, "", ""}
        ));
        when(conceptTableQueryRepository.findBroaderEdges("TH1", "fr")).thenReturn(List.<Object[]>of(
                new Object[]{"C2", "C1", "Adobe"},
                new Object[]{"C1", "ROOT", "Technique"}
        ));

        var response = service.loadRows("TH1", "fr");

        assertFalse(response.truncated());
        assertEquals(2, response.rows().size());
        ConceptTableRow child = response.rows().get(0);
        assertEquals("C2", child.id());
        assertEquals("valide", child.status());
        assertEquals("Normal", child.statusLabel());
        assertEquals("Concept", child.type());
        assertEquals("Technique › Adobe", child.path());
        assertEquals("Technique", response.rows().get(1).path());
        verify(conceptTableQueryRepository).findTableConceptRows(eq("TH1"), eq("fr"), eq(false), anyInt());
    }

    @Test
    void loadRows_includesCandidatesWhenLoggedIn() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(conceptTableQueryRepository.findTableConceptRows("TH1", "fr", true, 15001)).thenReturn(List.<Object[]>of(
                new Object[]{"CA1", "Aiguille", "", "CA", "concept", "Concept", CandidatStatusCode.PENDING, "c.roussel", "2026-06-30"}
        ));
        when(conceptTableQueryRepository.findBroaderEdges("TH1", "fr")).thenReturn(List.of());

        ConceptTableRow row = service.loadRows("TH1", "fr").rows().get(0);

        assertEquals("candidat", row.status());
        assertEquals("Candidat", row.statusLabel());
        assertEquals("c.roussel", row.candidateBy());
        assertEquals("2026-06-30", row.candidateOn());
        verify(conceptTableQueryRepository).findTableConceptRows("TH1", "fr", true, 15001);
    }

    @Test
    void loadRows_mapsRejectedAndDeprecated() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(conceptTableQueryRepository.findTableConceptRows(eq("TH1"), eq("fr"), anyBoolean(), anyInt())).thenReturn(List.<Object[]>of(
                new Object[]{"R1", "Rejeté", "", "CA", "concept", "Concept", CandidatStatusCode.REJECTED, "", ""},
                new Object[]{"D1", "Vieux", "N1", "DEP", "concept", "Concept", 0, "", ""}
        ));
        when(conceptTableQueryRepository.findBroaderEdges("TH1", "fr")).thenReturn(List.of());

        var rows = service.loadRows("TH1", "fr").rows();
        assertEquals("rejete", rows.get(0).status());
        assertEquals("Rejeté", rows.get(0).statusLabel());
        assertEquals("deprecie", rows.get(1).status());
        assertEquals("Déprécié", rows.get(1).statusLabel());
    }

    @Test
    void loadRows_returnsEmptyWhenThesaurusMissing() {
        assertTrue(service.loadRows(" ", "fr").rows().isEmpty());
    }

    @Test
    void pathFor_stopsOnCycle() {
        String path = ConceptTableConsultationService.pathFor(
                "A",
                Map.of("A", "B", "B", "A"),
                Map.of("A", "Alpha", "B", "Beta")
        );
        assertEquals("Alpha › Beta", path);
    }

    @Test
    void uiStatus_acceptedCandidateIsInserted() {
        assertEquals("insere", ConceptTableConsultationService.uiStatus("CA", CandidatStatusCode.ACCEPTED));
        assertEquals("insere", ConceptTableConsultationService.uiStatus("D", CandidatStatusCode.ACCEPTED));
        assertEquals("deprecie", ConceptTableConsultationService.uiStatus("DEP", CandidatStatusCode.ACCEPTED));
        assertEquals("Inséré", ConceptTableConsultationService.statusLabel("insere"));
    }
}
