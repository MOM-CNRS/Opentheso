package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.entites.ProjectDescription;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.repositories.ProjectDescriptionRepository;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectLangOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationProjectHomeServiceTest {

    @Mock
    private ProjectDescriptionRepository projectDescriptionRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    @Mock
    private ConsultationCatalogService consultationCatalogService;

    private ConsultationProjectHomeService service;

    @BeforeEach
    void setUp() {
        service = new ConsultationProjectHomeService(
                projectDescriptionRepository,
                languageRepository,
                thesaurusHomeQueryRepository,
                consultationCatalogService
        );
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void listThesauriWithCounts_mapsCatalogAndCounts() {
        when(consultationCatalogService.listThesauri(1, false, 9, "fr"))
                .thenReturn(List.of(new ConsultationThesaurusOption("TH1", "Pays", "fr")));
        when(thesaurusHomeQueryRepository.countValidConcepts("TH1")).thenReturn(12);

        var items = service.listThesauriWithCounts(1, false, 9, "fr");

        assertEquals(1, items.size());
        assertEquals("TH1", items.get(0).id());
        assertEquals(12, items.get(0).conceptCount());
        assertTrue(items.get(0).displayLabel().contains("12"));
    }

    @Test
    void listDescriptionLanguages_mapsIsoLanguages() {
        when(languageRepository.findLanguagesByProject("9")).thenReturn(List.of(
                LanguageIso639.builder().iso6391("fr").frenchName("Français").englishName("French").codePays("fr").build()
        ));

        var langs = service.listDescriptionLanguages(9);

        assertEquals(1, langs.size());
        assertEquals("fr", langs.get(0).iso6391());
        assertEquals("fr _ Français (French)", langs.get(0).displayLabel());
    }

    @Test
    void listAllLanguages_mapsCatalog() {
        when(languageRepository.findAllOrderByCodePays()).thenReturn(List.of(
                LanguageIso639.builder().iso6391("en").frenchName("Anglais").englishName("English").codePays("gb").build()
        ));

        var langs = service.listAllLanguages();

        assertEquals("en", langs.get(0).iso6391());
        assertEquals("gb", langs.get(0).countryCode());
    }

    @Test
    void findDescription_emptyWhenLangBlank() {
        assertTrue(service.findDescription(9, " ").isEmpty());
        verify(projectDescriptionRepository, never()).findByIdGroupAndLang(any(), any());
    }

    @Test
    void resolveDescription_fallsBackToFirstProjectLanguage() {
        ProjectDescription fallback = ProjectDescription.builder().id(2).idGroup("9").lang("en").description("<p>en</p>").build();
        when(projectDescriptionRepository.findByIdGroupAndLang("9", "de")).thenReturn(Optional.empty());
        when(languageRepository.findLanguagesByProject("9")).thenReturn(List.of(
                LanguageIso639.builder().iso6391("en").frenchName("Anglais").englishName("English").codePays("gb").build()
        ));
        when(projectDescriptionRepository.findByIdGroupAndLang("9", "en")).thenReturn(Optional.of(fallback));

        Optional<ProjectDescription> resolved = service.resolveDescription(9, "de");

        assertTrue(resolved.isPresent());
        assertEquals("en", resolved.get().getLang());
    }

    @Test
    void resolveDescription_usesDefaultWorkLanguageWhenPreferredBlank() {
        ProjectDescription preferred = ProjectDescription.builder().id(1).idGroup("9").lang("fr").build();
        when(projectDescriptionRepository.findByIdGroupAndLang("9", "fr")).thenReturn(Optional.of(preferred));

        assertEquals(preferred, service.resolveDescription(9, null).orElseThrow());
    }

    @Test
    void saveDescription_createsWhenMissing() {
        when(projectDescriptionRepository.findByIdGroupAndLang("9", "fr")).thenReturn(Optional.empty());
        when(projectDescriptionRepository.save(any(ProjectDescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDescription saved = service.saveDescription(9, "fr", "<p>hello</p>");

        assertEquals("9", saved.getIdGroup());
        assertEquals("<p>hello</p>", saved.getDescription());
    }

    @Test
    void deleteDescription_skipsNullId() {
        service.deleteDescription(new ProjectDescription());
        verify(projectDescriptionRepository, never()).delete(any());
    }

    @Test
    void deleteDescription_deletesPersistedEntity() {
        ProjectDescription description = ProjectDescription.builder().id(4).build();
        service.deleteDescription(description);
        verify(projectDescriptionRepository).delete(description);
    }

    @Test
    void resolveCountryCode_matchesIgnoreCase() {
        var langs = List.of(new ConsultationProjectLangOption("fr", "Français", "French", "fr"));
        assertEquals("fr", service.resolveCountryCode("FR", langs));
        assertEquals(null, service.resolveCountryCode("de", langs));
        assertEquals(null, service.resolveCountryCode("fr", null));
    }
}
