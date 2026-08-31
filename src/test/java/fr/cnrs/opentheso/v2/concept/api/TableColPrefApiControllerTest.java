package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TableColPrefDto;
import fr.cnrs.opentheso.v2.user.model.TableColPref;
import fr.cnrs.opentheso.v2.user.service.TableColPrefService;
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
class TableColPrefApiControllerTest {

    @Mock
    private AuthenticatedUserSource authenticatedUserSource;
    @Mock
    private TableColPrefService tableColPrefService;

    private TableColPrefApiController controller;

    @BeforeEach
    void setUp() {
        controller = new TableColPrefApiController(authenticatedUserSource, tableColPrefService);
    }

    @Test
    void getPref_unauthorizedWhenAnonymous() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.empty());

        assertEquals(HttpStatus.UNAUTHORIZED, controller.getPref().getStatusCode());
        verifyNoInteractions(tableColPrefService);
    }

    @Test
    void getPref_returnsStoredSelection() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(7));
        when(tableColPrefService.getPref(7)).thenReturn(new TableColPref(Set.of("status", "path")));

        var response = controller.getPref();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().selected().contains("status"));
        assertEquals(2, response.getBody().selected().size());
    }

    @Test
    void savePref_persistsBody() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(3));
        when(tableColPrefService.savePref(3, List.of("notation"))).thenReturn(
                new TableColPref(Set.of("notation"))
        );

        var response = controller.savePref(new TableColPrefDto(List.of("notation")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("notation"), response.getBody().selected());
    }

    @Test
    void savePref_emptyBodyUsesEmptySelection() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(3));
        when(tableColPrefService.savePref(3, List.of())).thenReturn(new TableColPref(Set.of()));

        var response = controller.savePref(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().selected().isEmpty());
    }
}
