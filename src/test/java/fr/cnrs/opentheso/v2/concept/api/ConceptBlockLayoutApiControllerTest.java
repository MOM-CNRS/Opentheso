package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.ConceptBlockLayoutDto;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockIds;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import fr.cnrs.opentheso.v2.user.service.ConceptBlockLayoutService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptBlockLayoutApiControllerTest {

    @Mock
    private AuthenticatedUserSource authenticatedUserSource;
    @Mock
    private ConceptBlockLayoutService conceptBlockLayoutService;

    private ConceptBlockLayoutApiController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptBlockLayoutApiController(authenticatedUserSource, conceptBlockLayoutService);
    }

    @Test
    void getLayout_unauthorizedWhenAnonymous() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.empty());

        var response = controller.getLayout();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(conceptBlockLayoutService);
    }

    @Test
    void getLayout_returnsSavedLayout() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(9));
        when(conceptBlockLayoutService.getLayout(9)).thenReturn(
                new ConceptBlockLayout(List.of("notes", "contexte"), Set.of("notes"))
        );

        var response = controller.getLayout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new ConceptBlockLayoutDto(List.of("notes", "contexte"), List.of("notes")), response.getBody());
    }

    @Test
    void saveLayout_persistsForConnectedUser() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(9));
        when(conceptBlockLayoutService.saveLayout(9, List.of("temporel"), List.of("temporel")))
                .thenReturn(new ConceptBlockLayout(ConceptBlockIds.DEFAULT_ORDER, Set.of("temporel")));

        var response = controller.saveLayout(new ConceptBlockLayoutDto(List.of("temporel"), List.of("temporel")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ConceptBlockIds.DEFAULT_ORDER, response.getBody().order());
        assertEquals(List.of("temporel"), response.getBody().collapsed());
    }
}
