package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLexicalNativeWriteServiceTest {

    @Mock
    private ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    @Mock
    private ConceptSynonymWriteRepository conceptSynonymWriteRepository;
    @Mock
    private ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @InjectMocks
    private ConceptLexicalNativeWriteService service;

    @Test
    void addSynonym_rejectsBlankValue() {
        var command = new AddSynonymCommand("TH1", "C1", "fr", " ", false, 7, "admin", false);

        var result = service.addSynonym(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
    }

    @Test
    void addSynonym_detectsDuplicateLabel() {
        var command = new AddSynonymCommand("TH1", "C1", "fr", "Animal", false, 7, "admin", false);
        when(conceptLexicalWriteRepository.existsPrefLabel("Animal", "fr", "TH1")).thenReturn(true);

        var result = service.addSynonym(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(conceptLexicalWriteRepository, never()).findPreferredTermId(anyString(), anyString());
    }

    @Test
    void addSynonym_insertsWhenValid() {
        var command = new AddSynonymCommand("TH1", "C1", "fr", "Animal", false, 7, "admin", false);
        when(conceptLexicalWriteRepository.existsPrefLabel("Animal", "fr", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.existsAltLabel("Animal", "fr", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.findPreferredTermId("TH1", "C1")).thenReturn(Optional.of("T1"));

        var result = service.addSynonym(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptSynonymWriteRepository).insertSynonym("T1", "TH1", "fr", "Animal", false, 7);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }

    @Test
    void updateSynonym_updatesValueWhenChanged() {
        var command = new UpdateSynonymCommand("TH1", "C1", "fr", "Old", "New", true, 7, "admin", false);
        when(conceptLexicalWriteRepository.findPreferredTermId("TH1", "C1")).thenReturn(Optional.of("T1"));
        when(conceptLexicalWriteRepository.existsPrefLabel("New", "fr", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.existsAltLabel("New", "fr", "TH1")).thenReturn(false);
        when(conceptSynonymWriteRepository.updateSynonym("T1", "TH1", "fr", "Old", "New", true, 7)).thenReturn(true);

        var result = service.updateSynonym(command);

        assertEquals(MutationOutcome.OK, result.outcome());
    }

    @Test
    void deleteSynonym_requiresSelection() {
        var command = new DeleteSynonymCommand("TH1", "C1", "", "value", 7, "admin");

        var result = service.deleteSynonym(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
    }

    @Test
    void addTranslation_rejectsDuplicateTerm() {
        var command = new AddTranslationCommand("TH1", "C1", "en", "Cat", 7, "admin");
        when(conceptLexicalWriteRepository.existsTermIgnoreCase("Cat", "en", "TH1")).thenReturn(true);

        var result = service.addTranslation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptTranslationWriteRepository, never()).insertTranslation(
                anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void addTranslation_insertsWhenValid() {
        var command = new AddTranslationCommand("TH1", "C1", "en", "Cat", 7, "admin");
        when(conceptLexicalWriteRepository.existsTermIgnoreCase("Cat", "en", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.findPreferredTermId("TH1", "C1")).thenReturn(Optional.of("T1"));

        var result = service.addTranslation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptTranslationWriteRepository).insertTranslation("T1", "TH1", "en", "Cat", 7);
    }
}
