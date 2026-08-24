package fr.cnrs.opentheso.v2.collection.api;

import fr.cnrs.opentheso.v2.collection.model.CollectionDetail;
import fr.cnrs.opentheso.v2.collection.model.CollectionTreeNode;
import fr.cnrs.opentheso.v2.collection.read.CollectionTreeConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionTreeApiControllerTest {

    @Mock
    private CollectionTreeConsultationService collectionTreeConsultationService;

    private CollectionTreeApiController controller;

    @BeforeEach
    void setUp() {
        controller = new CollectionTreeApiController(collectionTreeConsultationService);
    }

    @Test
    void roots_usesQueryParams() {
        var expected = List.of(new CollectionTreeNode("G1", "Matière", "01", "group", true, ""));
        when(collectionTreeConsultationService.loadRoots("TH1", "en", true)).thenReturn(expected);

        var response = controller.roots("TH1", "en", true);

        assertEquals(expected, response);
        verify(collectionTreeConsultationService).loadRoots("TH1", "en", true);
    }

    @Test
    void roots_defaultsLangToFr() {
        when(collectionTreeConsultationService.loadRoots("TH1", "fr", false)).thenReturn(List.of());

        controller.roots("TH1", null, false);

        verify(collectionTreeConsultationService).loadRoots("TH1", "fr", false);
    }

    @Test
    void roots_returnsEmptyWhenNoThesaurus() {
        var response = controller.roots(" ", "fr", false);

        assertTrue(response.isEmpty());
        verifyNoInteractions(collectionTreeConsultationService);
    }

    @Test
    void list_usesQueryParams() {
        var expected = List.of(new CollectionTreeNode("G1", "Matière", "01", "group", false, ""));
        when(collectionTreeConsultationService.loadAll("TH1", "en", true)).thenReturn(expected);

        var response = controller.list("TH1", "en", true);

        assertEquals(expected, response);
        verify(collectionTreeConsultationService).loadAll("TH1", "en", true);
    }

    @Test
    void list_returnsEmptyWhenNoThesaurus() {
        assertTrue(controller.list(" ", "fr", false).isEmpty());
        verifyNoInteractions(collectionTreeConsultationService);
    }

    @Test
    void children_requiresParentAndThesaurus() {
        assertTrue(controller.children(" ", "TH1", "fr", false).isEmpty());
        assertTrue(controller.children("G1", " ", "fr", false).isEmpty());
        verifyNoInteractions(collectionTreeConsultationService);
    }

    @Test
    void detail_returnsEmptyWhenMissingIds() {
        CollectionDetail response = controller.detail(" ", "TH1", "fr");

        assertEquals("", response.groupId());
        verifyNoInteractions(collectionTreeConsultationService);
    }
}
