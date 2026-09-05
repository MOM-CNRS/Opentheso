package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWriteMetadataPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptWriteMetadataServiceTest {

    @Mock
    private ConceptWriteMetadataPersistence conceptWriteMetadataPersistence;
    @Mock
    private GroupService groupService;

    private ConceptWriteMetadataService service;

    @BeforeEach
    void setUp() {
        service = new ConceptWriteMetadataService(conceptWriteMetadataPersistence, groupService);
    }

    @Test
    void listUsedLanguages_delegatesToPersistence() {
        var langs = List.of(new ConceptWriteLanguage("fr", "Français"));
        when(conceptWriteMetadataPersistence.listUsedLanguages("TH1", "fr")).thenReturn(langs);

        assertEquals(langs, service.listUsedLanguages("TH1", "fr"));
    }

    @Test
    void listNoteTypes_delegatesToPersistence() {
        var types = List.of(new ConceptWriteNoteType("definition"));
        when(conceptWriteMetadataPersistence.listNoteTypes()).thenReturn(types);

        assertEquals(types, service.listNoteTypes());
    }

    @Test
    void listNtRelationTypes_delegatesToPersistence() {
        var types = List.of(new fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType(
                "NT", "spécifique", "narrower"));
        when(conceptWriteMetadataPersistence.listNtRelationTypes()).thenReturn(types);

        assertEquals(types, service.listNtRelationTypes());
    }

    @Test
    void listConceptTypes_returnsEmptyWhenThesaurusBlank() {
        assertTrue(service.listConceptTypes(" ").isEmpty());
    }

    @Test
    void listCollections_returnsEmptyWhenIdsBlank() {
        assertTrue(service.listCollections(null, "fr").isEmpty());
        assertTrue(service.listCollections("TH1", " ").isEmpty());
    }

    @Test
    void listCollections_deduplicatesAndSortsByLabel() {
        when(groupService.getListConceptGroup("TH1", "fr")).thenReturn(Arrays.asList(
                group("G2", "Zèbre"),
                group("G1", "Abeille"),
                group("g2", "Autre zèbre"),
                null
        ));

        var result = service.listCollections("TH1", "fr");

        assertEquals(2, result.size());
        assertEquals("G1", result.get(0).id());
        assertEquals("Abeille", result.get(0).label());
        assertEquals("G2", result.get(1).id());
    }

    @Test
    void listCollections_returnsEmptyWhenGroupsNull() {
        when(groupService.getListConceptGroup("TH1", "fr")).thenReturn(null);

        assertTrue(service.listCollections("TH1", "fr").isEmpty());
    }

    @Test
    void loadNoteDraft_delegatesToPersistence() {
        service.loadNoteDraft("TH1", "C1", "fr", "definition");
        verify(conceptWriteMetadataPersistence).loadNoteDraft("TH1", "C1", "fr", "definition");
    }

    private static NodeGroup group(String id, String label) {
        NodeGroup node = new NodeGroup();
        ConceptGroup conceptGroup = new ConceptGroup();
        conceptGroup.setIdGroup(id);
        node.setConceptGroup(conceptGroup);
        node.setLexicalValue(label);
        return node;
    }
}
