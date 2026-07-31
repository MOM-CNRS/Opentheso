package fr.cnrs.opentheso.v2.collection.identifier;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionIdentifierAssignmentServiceTest {

    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private CollectionArkWriteService collectionArkWriteService;
    @Mock
    private CollectionHandleWriteService collectionHandleWriteService;

    @InjectMocks
    private CollectionIdentifierAssignmentService service;

    @Test
    void assignOnCreation_skipsWhenNoPreferences() {
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.empty());

        service.assignOnCreation("TH1", "g1", "Label");

        verify(collectionArkWriteService, never()).assignArkOnCreation("TH1", "g1", "Label");
        verify(collectionHandleWriteService, never()).assignHandleOnCreation("g1", "TH1");
    }

    @Test
    void assignOnCreation_assignsArkAndHandle() {
        var preferences = Preferences.builder()
                .idThesaurus("TH1")
                .useArk(true)
                .useHandle(true)
                .generateHandle(false)
                .build();
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(preferences));
        when(collectionHandleWriteService.assignHandleOnCreation("g1", "TH1")).thenReturn(true);

        service.assignOnCreation("TH1", "g1", "Label");

        verify(collectionArkWriteService).assignArkOnCreation("TH1", "g1", "Label");
        verify(collectionHandleWriteService).assignHandleOnCreation("g1", "TH1");
    }

    @Test
    void assignOnCreation_skipsHandleWhenGeneratedFromArk() {
        var preferences = Preferences.builder()
                .idThesaurus("TH1")
                .useArk(true)
                .useHandle(true)
                .generateHandle(true)
                .build();
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(preferences));

        service.assignOnCreation("TH1", "g1", "Label");

        verify(collectionArkWriteService).assignArkOnCreation("TH1", "g1", "Label");
        verify(collectionHandleWriteService, never()).assignHandleOnCreation("g1", "TH1");
    }

    @Test
    void assignOnCreation_logsWhenHandleCreationFails() {
        var preferences = Preferences.builder()
                .idThesaurus("TH1")
                .useHandle(true)
                .generateHandle(false)
                .build();
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(preferences));
        when(collectionHandleWriteService.assignHandleOnCreation("g1", "TH1")).thenReturn(false);

        service.assignOnCreation("TH1", "g1", "Label");

        verify(collectionHandleWriteService).assignHandleOnCreation("g1", "TH1");
    }
}
