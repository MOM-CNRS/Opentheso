package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;

import fr.cnrs.opentheso.candidat.api.CandidatAuthSupport;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.shared.exception.ModuleAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatControllerTest {

    private static final String API_KEY = "key";
    private static final String THESO_ID = "TH1";
    private static final int USER_ID = 3;

    @Mock
    private CandidatService candidatService;

    @Mock
    private UserService userService;

    @Mock
    private CandidatAuthSupport candidatAuthSupport;

    @Mock
    private fr.cnrs.opentheso.setting.service.ThesaurusApiSwitch thesaurusApiSwitch;

    @InjectMocks
    private CandidatController controller;

    @Test
    void addCandidate_rejectsInvalidApiKey() {
        when(userService.getUserByApiKeyBcrypt(API_KEY)).thenReturn(null);

        var response = controller.addCandidate(API_KEY, candidate("Amphore"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Clé d'API invalide", response.getBody());
        verifyNoInteractions(candidatAuthSupport, candidatService);
    }

    @Test
    void addCandidate_rejectsUserWithoutContributorRight() {
        givenAuthenticatedUser();
        doThrow(new ModuleAccessDeniedException("candidat"))
                .when(candidatAuthSupport).requireContributor(USER_ID, THESO_ID);
        Candidate candidate = candidate("Amphore");

        assertThrows(ModuleAccessDeniedException.class, () -> controller.addCandidate(API_KEY, candidate));

        verifyNoInteractions(candidatService);
    }

    @Test
    void addCandidate_rejectsUnknownNoteType() {
        givenAuthenticatedUser();
        Candidate candidate = candidate("Amphore");
        when(candidatService.unsupportedNoteType(candidate)).thenReturn("custom");

        var response = controller.addCandidate(API_KEY, candidate);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Type de note inconnu : custom", response.getBody());
        verify(candidatService, never()).saveCandidat(any(), anyInt());
    }

    @Test
    void addCandidate_rejectsDuplicatePreferredLabel() {
        givenAuthenticatedUser();
        when(candidatService.preferredLabelExists(THESO_ID, "Amphore")).thenReturn(true);

        var response = controller.addCandidate(API_KEY, candidate("Amphore"));

        assertEquals(409, response.getStatusCode().value());
        verify(candidatService, never()).saveCandidat(any(), anyInt());
    }

    @Test
    void addCandidate_createsWhenLabelIsFree() {
        givenAuthenticatedUser();
        Candidate candidate = candidate("Amphore");
        when(candidatService.preferredLabelExists(THESO_ID, "Amphore")).thenReturn(false);
        when(candidatService.saveCandidat(candidate, USER_ID)).thenReturn(true);

        var response = controller.addCandidate(API_KEY, candidate);

        assertEquals(200, response.getStatusCode().value());
        verify(candidatAuthSupport).requireContributor(USER_ID, THESO_ID);
        verify(candidatService).saveCandidat(candidate, USER_ID);
    }

    @Test
    void addCandidate_doesNotCheckBlankLabel() {
        givenAuthenticatedUser();
        Candidate candidate = candidate("  ");
        when(candidatService.saveCandidat(candidate, USER_ID)).thenReturn(true);

        var response = controller.addCandidate(API_KEY, candidate);

        assertEquals(200, response.getStatusCode().value());
        verify(candidatService, never()).preferredLabelExists(anyString(), anyString());
    }

    @Test
    void addCandidate_returnsBadRequestWhenSaveFails() {
        givenAuthenticatedUser();
        Candidate candidate = candidate("Amphore");
        when(candidatService.saveCandidat(candidate, USER_ID)).thenReturn(false);

        var response = controller.addCandidate(API_KEY, candidate);

        assertEquals(400, response.getStatusCode().value());
    }

    private void givenAuthenticatedUser() {
        User user = new User();
        user.setId(USER_ID);
        when(userService.getUserByApiKeyBcrypt(API_KEY)).thenReturn(user);
    }

    private static Candidate candidate(String label) {
        return Candidate.builder()
                .thesoId(THESO_ID)
                .terme(List.of(Element.builder().value(label).lang("fr").build()))
                .build();
    }
}