package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusContextTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ThesaurusContext thesaurusContext;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
    }

    @Test
    void syncFromViewParams_ignoresBlankThesaurusId() {
        thesaurusContext.setIdThesoFromUri("  ");

        thesaurusContext.syncFromViewParams();

        assertNull(thesaurusContext.getCurrentThesaurusId());
        assertFalse(thesaurusContext.isFromUrl());
        verify(thesaurusSelectionService, never()).resolve(anyString());
    }

    @Test
    void syncFromViewParams_setsCurrentThesaurusAndClearsViewParams() {
        thesaurusContext.setIdThesoFromUri(" TH1 ");
        thesaurusContext.setIdConceptFromUri("C1");
        thesaurusContext.setIdGroupFromUri("G1");
        when(thesaurusSelectionService.resolve("TH1"))
                .thenReturn(new ThesaurusSelection("TH1", "Mon thésaurus"));
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");

        thesaurusContext.syncFromViewParams();

        assertTrue(thesaurusContext.isFromUrl());
        assertEquals("TH1", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Mon thésaurus", thesaurusContext.getCurrentThesaurusTitle());
        assertEquals("fr", thesaurusContext.resolveWorkLanguage());
        assertNull(thesaurusContext.getIdThesoFromUri());
        // Conservés pour que l'écran de consultation puisse les consommer
        assertEquals("C1", thesaurusContext.getIdConceptFromUri());
        assertEquals("G1", thesaurusContext.getIdGroupFromUri());
    }

    @Test
    void syncFromViewParams_fallsBackToIdWhenTitleMissing() {
        thesaurusContext.setIdThesoFromUri("TH1");
        when(thesaurusSelectionService.resolve("TH1"))
                .thenReturn(new ThesaurusSelection("TH1", "TH1"));

        thesaurusContext.syncFromViewParams();

        assertEquals("TH1", thesaurusContext.getCurrentThesaurusTitle());
    }

    @Test
    void syncFromViewParams_keepsExistingSelectionWhenNoViewParam() {
        thesaurusContext.setCurrentThesaurusId("TH2");
        thesaurusContext.setCurrentThesaurusTitle("Thésaurus 2");

        thesaurusContext.syncFromViewParams();

        assertFalse(thesaurusContext.isFromUrl());
        assertEquals("TH2", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Thésaurus 2", thesaurusContext.getCurrentThesaurusTitle());
        verify(thesaurusSelectionService, never()).resolve(anyString());
    }

    @Test
    void resolveWorkLanguage_usesCurrentLanguageWhenPresent() {
        thesaurusContext.setCurrentLanguage("en");

        assertEquals("en", thesaurusContext.resolveWorkLanguage());
    }

    @Test
    void clearSelection_resetsContext() {
        thesaurusContext.setCurrentThesaurusId("TH1");
        thesaurusContext.setCurrentThesaurusTitle("Test");
        thesaurusContext.setFromUrl(true);

        thesaurusContext.clearSelection();

        assertNull(thesaurusContext.getCurrentThesaurusId());
        assertNull(thesaurusContext.getCurrentThesaurusTitle());
        assertFalse(thesaurusContext.isFromUrl());
    }

    @Test
    void selectThesaurus_withCatalogData_skipsResolveQueries() {
        thesaurusContext.selectThesaurus("TH1", "Catalogue title", "en");

        assertEquals("TH1", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Catalogue title", thesaurusContext.getCurrentThesaurusTitle());
        assertEquals("en", thesaurusContext.getCurrentLanguage());
        verify(thesaurusSelectionService, never()).resolve(anyString());
        verify(thesaurusWorkLanguageService, never()).resolveForThesaurus(anyString());
    }
}
