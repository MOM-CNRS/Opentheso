package fr.cnrs.opentheso.toolbox.actions.api;

import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotConceptService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionsLotConceptTemplateControllerTest {

    @Test
    void addTemplate_downloadsTheSampleCsv() {
        ActionsLotConceptService service = mock(ActionsLotConceptService.class);
        byte[] csv = "URI,skos:prefLabel@fr\n20,France\n".getBytes(StandardCharsets.UTF_8);
        when(service.addTemplateBytes()).thenReturn(csv);
        ActionsLotConceptTemplateController controller = new ActionsLotConceptTemplateController(service);

        ResponseEntity<byte[]> response = controller.template("add");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("modele-concepts-ajout.csv"));
        assertArrayEquals(csv, response.getBody());
    }

    @Test
    void unknownKind_isNotFound() {
        ActionsLotConceptTemplateController controller =
                new ActionsLotConceptTemplateController(mock(ActionsLotConceptService.class));

        assertEquals(404, controller.template("autre").getStatusCode().value());
    }
}
