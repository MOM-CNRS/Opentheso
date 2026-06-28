package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.fixtures.ToolboxTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageFlagServiceTest {

    @Mock
    private EditionQueryRepository editionQueryRepository;
    @Mock
    private LanguageRepository languageRepository;

    private LanguageFlagService service;

    @BeforeEach
    void setUp() {
        service = new LanguageFlagService(editionQueryRepository, languageRepository);
    }

    @Test
    void listAll_mapsLanguages() {
        when(editionQueryRepository.findAllLanguages())
                .thenReturn(List.of(ToolboxTestFixtures.sampleLanguageRow()));

        var flags = service.listAll();

        assertEquals(1, flags.size());
        assertEquals("fr", flags.get(0).getIso6391());
        assertEquals("fr", flags.get(0).getCountryCode());
    }

    @Test
    void updateCountryCode_persistsChange() {
        var language = new LanguageIso639();
        language.setIso6391("fr");
        when(languageRepository.findByIso6391("fr")).thenReturn(Optional.of(language));

        service.updateCountryCode("fr", " FR ");

        ArgumentCaptor<LanguageIso639> captor = ArgumentCaptor.forClass(LanguageIso639.class);
        verify(languageRepository).save(captor.capture());
        assertEquals("FR", captor.getValue().getCodePays());
    }

    @Test
    void updateCountryCode_rejectsBlankIso() {
        assertThrows(InvalidToolboxDataException.class, () -> service.updateCountryCode(" ", "FR"));
    }

    @Test
    void updateCountryCode_rejectsUnknownLanguage() {
        when(languageRepository.findByIso6391("xx")).thenReturn(Optional.empty());

        assertThrows(InvalidToolboxDataException.class, () -> service.updateCountryCode("xx", "FR"));
    }
}
