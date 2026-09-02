package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptArkWriteService;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptHandleWriteService;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptIdentifierWritePersistenceTest {

    @Mock
    private ConceptArkWriteService conceptArkWriteService;
    @Mock
    private ConceptHandleWriteService conceptHandleWriteService;
    @Mock
    private PreferencesRepository preferencesRepository;

    @InjectMocks
    private ConceptIdentifierWritePersistence persistence;

    @Test
    void generateArk_success_returnsOk() {
        when(preferencesRepository.findByIdThesaurus("TH1"))
                .thenReturn(Optional.of(Preferences.builder().idThesaurus("TH1").build()));
        when(conceptArkWriteService.generateArkIds("TH1", List.of("C1"), "fr")).thenReturn(null);

        var result = persistence.generateArk(new GenerateArkCommand("TH1", "fr", List.of("C1")));

        assertTrue(result.success());
    }

    @Test
    void generateArk_batchSuccess_returnsOk() {
        when(preferencesRepository.findByIdThesaurus("TH1"))
                .thenReturn(Optional.of(Preferences.builder().idThesaurus("TH1").build()));
        when(conceptArkWriteService.generateArkIds("TH1", List.of("C1", "C2"), "fr")).thenReturn(null);

        var result = persistence.generateArk(new GenerateArkCommand("TH1", "fr", List.of("C1", "C2")));

        assertTrue(result.success());
        assertFalse(result.warning());
    }

    @Test
    void generateArk_emptySelection_returnsValidationError() {
        var result = persistence.generateArk(new GenerateArkCommand("TH1", "fr", List.of()));

        assertFalse(result.success());
        assertEquals("Aucune sélection !", result.message());
    }

    @Test
    void generateArk_singleConceptError_returnsFailureMessage() {
        when(preferencesRepository.findByIdThesaurus("TH1"))
                .thenReturn(Optional.of(Preferences.builder().idThesaurus("TH1").build()));
        NodeIdValue error = new NodeIdValue();
        error.setId("C1");
        error.setValue("Erreur pendant la connexion avec le serveur Ark");
        when(conceptArkWriteService.generateArkIds("TH1", List.of("C1"), "fr")).thenReturn(List.of(error));

        var result = persistence.generateArk(new GenerateArkCommand("TH1", "fr", List.of("C1")));

        assertFalse(result.success());
        assertEquals("Erreur pendant la connexion avec le serveur Ark", result.message());
    }
}
