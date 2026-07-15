package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptNoteNativeWriteServiceTest {

    @Mock
    private ConceptNoteWriteRepository conceptNoteWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @InjectMocks
    private ConceptNoteNativeWriteService service;

    @Test
    void upsertNote_rejectsEmptyValue() {
        var command = new UpsertNoteCommand("TH1", "C1", "fr", "note", "  ", "src", 7, "admin");

        var result = service.upsertNote(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptNoteWriteRepository, never()).insertNote(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void upsertNote_insertsNewNote() {
        var command = new UpsertNoteCommand("TH1", "C1", "fr", "note", "<p>Hello</p>", "src", 7, "admin");
        when(conceptNoteWriteRepository.findNoteId("C1", "TH1", "fr", "note")).thenReturn(Optional.empty());
        when(conceptNoteWriteRepository.existsWithValue("C1", "TH1", "fr", "note", "Hello")).thenReturn(false);

        var result = service.upsertNote(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptNoteWriteRepository).insertNote(
                eq("C1"), eq("TH1"), eq("fr"), eq("note"), eq("Hello"), eq("src"), eq(7));
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
        verify(conceptWritePostMutationRepository).saveContributorDcTerm("TH1", "C1", "admin");
    }

    @Test
    void upsertNote_updatesExistingNote() {
        var command = new UpsertNoteCommand("TH1", "C1", "fr", "note", "Updated", "src", 7, "admin");
        when(conceptNoteWriteRepository.findNoteId("C1", "TH1", "fr", "note")).thenReturn(Optional.of(42));
        when(conceptNoteWriteRepository.updateNote(42, "TH1", "Updated", "src")).thenReturn(true);

        var result = service.upsertNote(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptNoteWriteRepository).insertNoteHistory(
                "C1", "TH1", "fr", "note", "Updated", "update", 7);
        verify(conceptNoteWriteRepository, never()).insertNote(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void deleteNote_rejectsMissingId() {
        var command = new DeleteNoteCommand("TH1", "C1", 0, "fr", "note", 7, "admin");

        var result = service.deleteNote(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptNoteWriteRepository, never()).deleteNote(anyInt(), anyString());
    }

    @Test
    void deleteNote_deletesAndRecordsHistory() {
        var command = new DeleteNoteCommand("TH1", "C1", 42, "fr", "note", 7, "admin");
        when(conceptNoteWriteRepository.findNoteLexicalValue("C1", "TH1", "fr", "note"))
                .thenReturn(Optional.of("Old note"));

        var result = service.deleteNote(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptNoteWriteRepository).deleteNote(42, "TH1");
        verify(conceptNoteWriteRepository).insertNoteHistory(
                "C1", "TH1", "fr", "note", "Old note", "delete", 7);
    }
}
