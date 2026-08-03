package fr.cnrs.opentheso.v2.sync.api;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusWriteAuthorizationService;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncReceiveService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncControllerTest {

    @Mock
    private ThesaurusSyncReceiveService thesaurusSyncReceiveService;
    @Mock
    private ThesaurusWriteAuthorizationService thesaurusWriteAuthorizationService;
    @Mock
    private HttpServletRequest request;

    private ThesaurusSyncController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new ThesaurusSyncController(thesaurusSyncReceiveService, thesaurusWriteAuthorizationService);
        user = User.builder().id(3).username("editor").mail("e@ex.com").build();
    }

    @Test
    void syncConcepts_rejectsMissingAuthenticatedUser() {
        when(request.getAttribute("authenticatedUser")).thenReturn(null);

        assertThrows(ApiKeyInvalidException.class, () ->
                controller.syncConcepts(request, "TH_MASTER", emptyBody()));
    }

    @Test
    void syncConcepts_rejectsUnauthorizedWriter() {
        when(request.getAttribute("authenticatedUser")).thenReturn(user);
        when(thesaurusWriteAuthorizationService.canUserWrite(3, "TH_MASTER")).thenReturn(false);

        assertThrows(UserCantWriteOnThesaurusException.class, () ->
                controller.syncConcepts(request, "TH_MASTER", emptyBody()));
    }

    @Test
    void syncConcepts_returnsOkOnSuccess() {
        when(request.getAttribute("authenticatedUser")).thenReturn(user);
        when(thesaurusWriteAuthorizationService.canUserWrite(3, "TH_MASTER")).thenReturn(true);
        SyncBatchResponse body = SyncBatchResponse.from(List.of(SyncConceptResult.skipped("C1", "C1", "ok")));
        when(thesaurusSyncReceiveService.receiveBatch("TH_MASTER", emptyBody(), user)).thenReturn(body);

        ResponseEntity<?> response = controller.syncConcepts(request, "TH_MASTER", emptyBody());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SyncBatchResponse bodyResponse = (SyncBatchResponse) response.getBody();
        assertEquals(1, bodyResponse.skipped());
        verify(thesaurusSyncReceiveService).receiveBatch("TH_MASTER", emptyBody(), user);
    }

    @Test
    void syncConcepts_returnsBadRequestWhenTargetNotMaster() {
        when(request.getAttribute("authenticatedUser")).thenReturn(user);
        when(thesaurusWriteAuthorizationService.canUserWrite(3, "TH_MASTER")).thenReturn(true);
        when(thesaurusSyncReceiveService.receiveBatch("TH_MASTER", emptyBody(), user))
                .thenThrow(new IllegalStateException("not master"));

        ResponseEntity<?> response = controller.syncConcepts(request, "TH_MASTER", emptyBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("not master", response.getBody());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    }

    private static SyncBatchRequest emptyBody() {
        return new SyncBatchRequest("TH_SLAVE", null, null, null, null, List.of());
    }
}
