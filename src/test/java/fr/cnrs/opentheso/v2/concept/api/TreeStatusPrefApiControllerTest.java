package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TreeStatusPrefDto;
import fr.cnrs.opentheso.v2.user.model.TreeStatusPref;
import fr.cnrs.opentheso.v2.user.service.TreeStatusPrefService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreeStatusPrefApiControllerTest {

    @Mock
    private AuthenticatedUserSource authenticatedUserSource;
    @Mock
    private TreeStatusPrefService treeStatusPrefService;

    private TreeStatusPrefApiController controller;

    @BeforeEach
    void setUp() {
        controller = new TreeStatusPrefApiController(authenticatedUserSource, treeStatusPrefService);
    }

    @Test
    void getPref_unauthorizedWhenAnonymous() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.empty());

        assertEquals(HttpStatus.UNAUTHORIZED, controller.getPref().getStatusCode());
        verifyNoInteractions(treeStatusPrefService);
    }

    @Test
    void getPref_returnsStoredSelection() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(7));
        when(treeStatusPrefService.getPref(7)).thenReturn(new TreeStatusPref(Set.of("valide", "deprecie")));

        var response = controller.getPref();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().selected().contains("valide"));
        assertEquals(2, response.getBody().selected().size());
    }

    @Test
    void savePref_persistsBody() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(3));
        when(treeStatusPrefService.savePref(3, List.of("rejete"))).thenReturn(
                new TreeStatusPref(Set.of("rejete"))
        );

        var response = controller.savePref(new TreeStatusPrefDto(List.of("rejete")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("rejete"), response.getBody().selected());
    }

    @Test
    void savePref_emptyBodyUsesEmptySelection() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(3));
        when(treeStatusPrefService.savePref(3, List.of())).thenReturn(new TreeStatusPref(Set.of()));

        var response = controller.savePref(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().selected().isEmpty());
    }
}
