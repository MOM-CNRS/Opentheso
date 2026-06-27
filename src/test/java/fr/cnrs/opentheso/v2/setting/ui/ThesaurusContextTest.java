package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusContextTest {

    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    @Mock
    private SelectedTheso selectedTheso;

    private ThesaurusContext thesaurusContext;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSettingsQueryRepository, selectedTheso);
        ReflectionTestUtils.setField(thesaurusContext, "workLanguage", "fr");
    }

    @Test
    void syncFromViewParams_ignoresBlankThesaurusId() {
        thesaurusContext.setIdThesoFromUri("  ");

        thesaurusContext.syncFromViewParams();

        assertNull(thesaurusContext.getCurrentThesaurusId());
        verify(thesaurusSettingsQueryRepository, never()).findThesaurusTitle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void syncFromViewParams_setsCurrentThesaurusAndClearsViewParams() {
        thesaurusContext.setIdThesoFromUri(" TH1 ");
        thesaurusContext.setIdConceptFromUri("C1");
        thesaurusContext.setIdGroupFromUri("G1");
        when(thesaurusSettingsQueryRepository.findThesaurusTitle("TH1", "fr"))
                .thenReturn(Optional.of("Mon thésaurus"));

        thesaurusContext.syncFromViewParams();

        assertEquals("TH1", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Mon thésaurus", thesaurusContext.getCurrentThesaurusTitle());
        assertNull(thesaurusContext.getIdThesoFromUri());
        assertNull(thesaurusContext.getIdConceptFromUri());
        assertNull(thesaurusContext.getIdGroupFromUri());
    }

    @Test
    void syncFromViewParams_fallsBackToIdWhenTitleMissing() {
        thesaurusContext.setIdThesoFromUri("TH1");
        when(thesaurusSettingsQueryRepository.findThesaurusTitle("TH1", "fr"))
                .thenReturn(Optional.empty());

        thesaurusContext.syncFromViewParams();

        assertEquals("TH1", thesaurusContext.getCurrentThesaurusTitle());
    }

    @Test
    void syncFromViewParams_usesSelectedThesaurusWhenNoViewParam() {
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH2");
        when(thesaurusSettingsQueryRepository.findThesaurusTitle("TH2", "fr"))
                .thenReturn(Optional.of("Thésaurus 2"));

        thesaurusContext.syncFromViewParams();

        assertEquals("TH2", thesaurusContext.getCurrentThesaurusId());
        assertEquals("Thésaurus 2", thesaurusContext.getCurrentThesaurusTitle());
    }
}
