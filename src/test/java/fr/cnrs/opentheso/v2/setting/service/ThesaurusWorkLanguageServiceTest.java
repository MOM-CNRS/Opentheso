package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusWorkLanguageServiceTest {

    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    private ThesaurusWorkLanguageService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusWorkLanguageService(thesaurusSettingsQueryRepository);
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void resolveForThesaurus_returnsDefaultWhenThesaurusBlank() {
        assertEquals("fr", service.resolveForThesaurus(" "));
    }

    @Test
    void resolveForThesaurus_returnsRepositoryLanguageWhenPresent() {
        when(thesaurusSettingsQueryRepository.findSourceLanguage("TH1")).thenReturn(Optional.of("en"));

        assertEquals("en", service.resolveForThesaurus("TH1"));
    }

    @Test
    void resolveForThesaurus_fallsBackToDefaultWhenMissing() {
        when(thesaurusSettingsQueryRepository.findSourceLanguage("TH1")).thenReturn(Optional.empty());

        assertEquals("fr", service.resolveForThesaurus("TH1"));
    }

    @Test
    void getDefaultWorkLanguage_exposesConfiguredValue() {
        assertEquals("fr", service.getDefaultWorkLanguage());
    }
}
