package fr.cnrs.opentheso.v2.concept.identifier;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptIdentifierAssignmentServiceTest {

    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private ConceptArkWriteService conceptArkWriteService;
    @Mock
    private ConceptHandleWriteService conceptHandleWriteService;

    @InjectMocks
    private ConceptIdentifierAssignmentService service;

    @Test
    void assignIdentifiers_skipsWhenNoPreferences() {
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.empty());

        service.assignIdentifiers("TH1", "C1", "fr");

        verify(conceptArkWriteService, never()).assignIdentifiersOnCreation("TH1", "C1", "fr");
        verify(conceptHandleWriteService, never()).assignHandleOnCreation("C1", "TH1");
    }

    @Test
    void assignIdentifiers_assignsHandleThenArk() {
        var preferences = Preferences.builder()
                .idThesaurus("TH1")
                .useHandle(true)
                .useArk(true)
                .build();
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(preferences));
        when(conceptHandleWriteService.assignHandleOnCreation("C1", "TH1")).thenReturn(true);

        service.assignIdentifiers("TH1", "C1", "fr");

        verify(conceptHandleWriteService).assignHandleOnCreation("C1", "TH1");
        verify(conceptArkWriteService).assignIdentifiersOnCreation("TH1", "C1", "fr");
    }

    @Test
    void assignIdentifiers_throwsWhenHandleCreationFails() {
        var preferences = Preferences.builder()
                .idThesaurus("TH1")
                .useHandle(true)
                .build();
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(preferences));
        when(conceptHandleWriteService.assignHandleOnCreation("C1", "TH1")).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.assignIdentifiers("TH1", "C1", "fr"));
    }
}
