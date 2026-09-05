package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLexicalNativeWriteService;
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
class ConceptLexicalMutationServiceTest {

    @Mock
    private ConceptLexicalNativeWriteService nativeWrite;
    @Mock
    private ConceptWriteMetadataService metadataService;

    private ConceptLexicalMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptLexicalMutationService(nativeWrite, metadataService);
    }

    @Test
    void mutations_delegateToNativeWrite() {
        var addSynonym = mock(AddSynonymCommand.class);
        var updateSynonym = mock(UpdateSynonymCommand.class);
        var deleteSynonym = mock(DeleteSynonymCommand.class);
        var addTranslation = mock(AddTranslationCommand.class);
        var updateTranslation = mock(UpdateTranslationCommand.class);
        var deleteTranslation = mock(DeleteTranslationCommand.class);
        when(nativeWrite.addSynonym(addSynonym)).thenReturn(MutationResult.ok("s"));
        when(nativeWrite.updateSynonym(updateSynonym)).thenReturn(MutationResult.ok("u"));
        when(nativeWrite.deleteSynonym(deleteSynonym)).thenReturn(MutationResult.ok("d"));
        when(nativeWrite.addTranslation(addTranslation)).thenReturn(MutationResult.ok("at"));
        when(nativeWrite.updateTranslation(updateTranslation)).thenReturn(MutationResult.ok("ut"));
        when(nativeWrite.deleteTranslation(deleteTranslation)).thenReturn(MutationResult.ok("dt"));

        assertTrue(service.addSynonym(addSynonym).success());
        assertTrue(service.updateSynonym(updateSynonym).success());
        assertTrue(service.deleteSynonym(deleteSynonym).success());
        assertTrue(service.addTranslation(addTranslation).success());
        assertTrue(service.updateTranslation(updateTranslation).success());
        assertTrue(service.deleteTranslation(deleteTranslation).success());
    }

    @Test
    void listUsedLanguages_delegatesToMetadata() {
        var langs = List.of(new ConceptWriteLanguage("fr", "Français"));
        when(metadataService.listUsedLanguages("TH1", "fr")).thenReturn(langs);

        assertEquals(langs, service.listUsedLanguages("TH1", "fr"));
    }
}
