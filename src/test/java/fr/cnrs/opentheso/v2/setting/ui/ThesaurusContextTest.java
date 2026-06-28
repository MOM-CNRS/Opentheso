package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusContextTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private SelectedTheso selectedTheso;

    private ThesaurusContext thesaurusContext;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSelectionService, selectedTheso);
    }

    @Test
    void syncFromViewParams_ignoresBlankThesaurusId() {
        thesaurusContext.setIdThesoFromUri("  ");

        thesaurusContext.syncFromViewParams();

        assertNull(thesaurusContext.getCurrentThesaurusId());
        verify(thesaurusSelectionService, never()).resolve(anyString());
    }

    @Test
    void syncFromViewParams_setsCurrentThesaurusAndClearsViewParams() {
        thesaurusContext.setIdThesoFromUri(" TH1 ");
        thesaurusContext.setIdConceptFromUri("C1");
        thesaurusContext.setIdGroupFromUri("G1");
        when(thesaurusSelectionService.resolve("TH1"))
                .thenReturn(new ThesaurusSelection("TH1", "Mon thésaurus"));

        thesaurusContext.syncFromViewParams();

        assertEquals("TH1", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Mon thésaurus", thesaurusContext.getCurrentThesaurusTitle());
        assertNull(thesaurusContext.getIdThesoFromUri());
        assertNull(thesaurusContext.getIdConceptFromUri());
        assertNull(thesaurusContext.getIdGroupFromUri());
        verify(selectedTheso).setCurrentIdTheso("TH1");
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
    void syncFromViewParams_usesSelectedThesaurusWhenNoViewParam() {
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH2");
        when(thesaurusSelectionService.resolve("TH2"))
                .thenReturn(new ThesaurusSelection("TH2", "Thésaurus 2"));

        thesaurusContext.syncFromViewParams();

        assertEquals("TH2", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Thésaurus 2", thesaurusContext.getCurrentThesaurusTitle());
    }
}
