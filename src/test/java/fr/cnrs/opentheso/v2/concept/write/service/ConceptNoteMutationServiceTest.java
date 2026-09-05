package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptNoteNativeWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptNoteMutationServiceTest {

    @Mock
    private ConceptNoteNativeWriteService nativeWrite;
    @Mock
    private ConceptWriteMetadataService metadataService;

    private ConceptNoteMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptNoteMutationService(nativeWrite, metadataService);
    }

    @Test
    void upsertAndDelete_delegateToNativeWrite() {
        var upsert = mock(UpsertNoteCommand.class);
        var delete = mock(DeleteNoteCommand.class);
        when(nativeWrite.upsertNote(upsert)).thenReturn(MutationResult.ok("ok"));
        when(nativeWrite.deleteNote(delete)).thenReturn(MutationResult.ok("del"));

        assertTrue(service.upsertNote(upsert).success());
        assertTrue(service.deleteNote(delete).success());
    }

    @Test
    void listNoteTypes_delegatesToMetadata() {
        var types = List.of(new ConceptWriteNoteType("definition"));
        when(metadataService.listNoteTypes()).thenReturn(types);

        assertEquals(types, service.listNoteTypes());
    }
}
