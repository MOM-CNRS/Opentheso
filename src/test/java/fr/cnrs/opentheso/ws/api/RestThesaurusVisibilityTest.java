package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.ws.api.RestThesaurusVisibilityRepository.ThesaurusAccessFacts;
import fr.cnrs.opentheso.shared.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static fr.cnrs.opentheso.ws.api.RestThesaurusAccess.FOREIGN_PROJECT;
import static fr.cnrs.opentheso.ws.api.RestThesaurusAccess.NOT_FOUND;
import static fr.cnrs.opentheso.ws.api.RestThesaurusAccess.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestThesaurusVisibilityTest {

    @Mock
    private RestThesaurusVisibilityRepository repository;
    @Mock
    private ThesaurusRepository thesaurusRepository;
    @Mock
    private RightsService rightsService;
    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private HttpServletRequest request;

    private RestThesaurusVisibility visibility;

    @BeforeEach
    void setUp() {
        visibility = new RestThesaurusVisibility(repository, thesaurusRepository, rightsService, apiKeyService);
    }

    @Test
    void anonymousSeesOnlyTheVisibleSet() {
        when(repository.describe(-1, "th17")).thenReturn(Optional.of(facts(true, false, false)));
        when(repository.visibleIds(-1)).thenReturn(List.of("th60"));

        assertEquals(PRIVATE, visibility.explain(null, "th17"));
        assertFalse(visibility.canSee(null, "th17"));
        assertEquals(List.of("th60"), visibility.visibleIds(null));
    }

    @Test
    void missingThesaurusIsNotFound() {
        when(repository.describe(-1, "absent")).thenReturn(Optional.empty());

        assertEquals(NOT_FOUND, visibility.explain(null, "absent"));
        assertEquals("{\"message\":\"Thésaurus introuvable.\"}", NOT_FOUND.body());
    }

    @Test
    void privateThesaurusOfAnotherProjectIsRefusedAsSuch() {
        when(rightsService.capabilities(11)).thenReturn(member(11));
        when(repository.describe(11, "th22")).thenReturn(Optional.of(facts(true, true, false)));

        assertEquals(FOREIGN_PROJECT, visibility.explain(11, "th22"));
        assertEquals(403, FOREIGN_PROJECT.status());
    }

    @Test
    void memberUsesTheProjectQuery() {
        when(rightsService.capabilities(11)).thenReturn(member(11));
        when(repository.describe(11, "th17")).thenReturn(Optional.of(facts(true, true, true)));

        assertTrue(visibility.canSee(11, "th17"));
    }

    @Test
    void superAdminSeesEveryThesaurus() {
        when(rightsService.capabilities(1)).thenReturn(superAdmin());
        when(thesaurusRepository.findAllOrderByCreatedDesc()).thenReturn(List.of(
                Thesaurus.builder().idThesaurus("th17").build(),
                Thesaurus.builder().idThesaurus("th60").build()
        ));

        assertTrue(visibility.canSee(1, "th17"));
        assertEquals(List.of("th17", "th60"), visibility.visibleIds(1));
        verifyNoInteractions(repository);
    }

    @Test
    void apiKeyIdentifiesTheCaller() {
        when(request.getHeader("X-API-KEY")).thenReturn("secret");
        when(apiKeyService.findUserByApiKey("secret")).thenReturn(Optional.of(User.builder().id(11).build()));

        assertEquals(11, visibility.currentUserId(request));
    }

    @Test
    void sessionWithoutApiKeySeesOnlyPublicThesauri() {
        when(request.getHeader("X-API-KEY")).thenReturn(null);
        when(request.getHeader("API-KEY")).thenReturn("  ");

        assertNull(visibility.currentUserId(request));
        verify(request, never()).getSession(false);
    }

    private static ThesaurusAccessFacts facts(boolean isPrivate, boolean attachedToProject, boolean memberOfProject) {
        return new ThesaurusAccessFacts(isPrivate, attachedToProject, memberOfProject);
    }

    private static SessionUser member(int id) {
        return new SessionUser(id, "membre", "membre@example.org", false, false, true, false);
    }

    private static SessionUser superAdmin() {
        return new SessionUser(1, "admin", "admin@example.org", true, true, false, false);
    }
}
