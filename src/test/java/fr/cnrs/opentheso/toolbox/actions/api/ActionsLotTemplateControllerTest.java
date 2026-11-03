package fr.cnrs.opentheso.toolbox.actions.api;

import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotAlignmentService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotAltLabelService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotArkService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotCollectionService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotImageService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotNotationService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotNoteService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionsLotTemplateControllerTest {

    @Test
    void collectionTemplate_downloadsTheSampleCsv() {
        ActionsLotCollectionService collections = mock(ActionsLotCollectionService.class);
        byte[] csv = "localId,skos:member\n26678,GR1\n".getBytes(StandardCharsets.UTF_8);
        when(collections.templateBytes()).thenReturn(csv);

        ResponseEntity<byte[]> response = controller(collections).template("collections", "import");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("modele-collections.csv"));
        assertArrayEquals(csv, response.getBody());
    }

    @Test
    void alignmentDeleteTemplate_usesTheDeleteFile() {
        ActionsLotAlignmentService alignments = mock(ActionsLotAlignmentService.class);
        byte[] csv = "localId,URI\n".getBytes(StandardCharsets.UTF_8);
        when(alignments.deleteTemplateBytes()).thenReturn(csv);
        ActionsLotTemplateController controller = new ActionsLotTemplateController(
                mock(ActionsLotCollectionService.class),
                mock(ActionsLotNotationService.class),
                mock(ActionsLotImageService.class),
                mock(ActionsLotArkService.class),
                mock(ActionsLotNoteService.class),
                mock(ActionsLotAltLabelService.class),
                alignments
        );

        ResponseEntity<byte[]> response = controller.template("alignements", "delete");

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("modele-alignements-suppression.csv"));
        assertArrayEquals(csv, response.getBody());
    }

    @Test
    void relationsTemplate_matchesTheExampleTable() {
        ResponseEntity<byte[]> response = controller(mock(ActionsLotCollectionService.class))
                .template("relations", "import");

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("modele-relations.csv"));
        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains("localId,localId cible"));
        assertTrue(body.contains("26678,300"));
    }

    @Test
    void unknownKind_isNotFound() {
        assertEquals(404, controller(mock(ActionsLotCollectionService.class))
                .template("synonymes", "autre")
                .getStatusCode()
                .value());
    }

    private static ActionsLotTemplateController controller(ActionsLotCollectionService collections) {
        return new ActionsLotTemplateController(
                collections,
                mock(ActionsLotNotationService.class),
                mock(ActionsLotImageService.class),
                mock(ActionsLotArkService.class),
                mock(ActionsLotNoteService.class),
                mock(ActionsLotAltLabelService.class),
                mock(ActionsLotAlignmentService.class)
        );
    }
}
