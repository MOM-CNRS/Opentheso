package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatControllerTest {

    @Mock
    private CandidatService candidatService;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private UserService userService;

    private CandidatController controller;

    @BeforeEach
    void setUp() {
        controller = new CandidatController(candidatService, apiKeyService, userService);
    }

    @Test
    void addCandidate_rejectsDuplicatePreferredLabel() {
        User user = new User();
        user.setId(3);
        when(userService.getUserByApiKeyBcrypt("key")).thenReturn(user);
        when(candidatService.unsupportedNoteType(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(candidatService.preferredLabelExists("TH1", "Amphore")).thenReturn(true);

        var response = controller.addCandidate("key", Candidate.builder()
                .thesoId("TH1")
                .terme(List.of(Element.builder().value("Amphore").lang("fr").build()))
                .build());

        assertEquals(409, response.getStatusCode().value());
        verify(candidatService, never()).saveCandidat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void addCandidate_rejectsUnknownNoteType() {
        User user = new User();
        user.setId(3);
        when(userService.getUserByApiKeyBcrypt("key")).thenReturn(user);
        when(candidatService.unsupportedNoteType(org.mockito.ArgumentMatchers.any())).thenReturn("custom");

        var response = controller.addCandidate("key", Candidate.builder()
                .thesoId("TH1")
                .terme(List.of(Element.builder().value("Amphore").lang("fr").build()))
                .notes(List.of(CandidateNote.builder().type("custom").value("texte").lang("fr").build()))
                .build());

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Type de note inconnu : custom", response.getBody());
        verify(candidatService, never()).saveCandidat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void addCandidate_createsWhenLabelIsFree() {
        User user = new User();
        user.setId(3);
        when(userService.getUserByApiKeyBcrypt("key")).thenReturn(user);
        when(candidatService.unsupportedNoteType(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(candidatService.preferredLabelExists("TH1", "Amphore")).thenReturn(false);
        when(candidatService.saveCandidat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(3)))
                .thenReturn(true);

        var response = controller.addCandidate("key", Candidate.builder()
                .thesoId("TH1")
                .terme(List.of(Element.builder().value("Amphore").lang("fr").build()))
                .notes(List.of(CandidateNote.builder().type("scopeNote").value("Usage").lang("fr").build()))
                .build());

        assertEquals(200, response.getStatusCode().value());
        verify(candidatService).saveCandidat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(3));
    }
}
