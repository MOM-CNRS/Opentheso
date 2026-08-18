package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.preview.service.AlignmentPersistDraft;
import fr.cnrs.opentheso.v2.preview.service.CorpusPersistDraft;
import fr.cnrs.opentheso.v2.preview.service.PreviewThesaurusSettingsPersistService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewThesaurusPreferenceBeanTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private PreviewThesaurusSettingsPersistService persistService;
    @Mock
    private PreviewThesaurusCorpusBean corpusBean;
    @Mock
    private PreviewThesaurusAlignmentBean alignmentBean;

    private PreviewThesaurusPreferenceBean bean;
    private PreviewSettingsAccess access;

    @BeforeEach
    void setUp() {
        ThesaurusContext thesaurusContext = new ThesaurusContext(
                thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        access = new PreviewSettingsAccess(thesaurusContext, userSession, rightsService);
        bean = new PreviewThesaurusPreferenceBean(
                access,
                thesaurusContext,
                thesaurusPreferenceService,
                persistService,
                corpusBean,
                alignmentBean
        );
    }

    @Test
    void loadsGeneralPreferenceFieldsFromService() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());

        assertEquals("TH1", bean.getPreference().getPreferredName());
        assertEquals("/api/theso/TH1", bean.getPreferencePermalink());
    }

    @Test
    void savePreferences_persistsDraftsThroughTransactionalService() {
        grantEdit();
        ThesaurusPreferences saved = SettingTestFixtures.samplePreferences();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(saved);
        when(persistService.saveAll(eq("th17"), eq(2), any(), eq("fr"), any(), any())).thenReturn(saved);
        when(corpusBean.toPersistDraft()).thenReturn(new CorpusPersistDraft(java.util.List.of(), java.util.List.of(), java.util.Map.of()));
        when(alignmentBean.toPersistDraft()).thenReturn(new AlignmentPersistDraft(java.util.List.of(), java.util.List.of(), java.util.Set.of()));

        bean.savePreferences();

        verify(persistService).saveAll(eq("th17"), eq(2), any(), eq("fr"), any(), any());
        verify(corpusBean).load();
        verify(alignmentBean).load();
        assertEquals("Paramètres enregistrés avec succès", bean.getPreferenceSaveMessage());
        assertFalse(bean.isPreferenceSaveError());
    }

    @Test
    void savePreferences_showsErrorWhenPreferredNameExists() {
        grantEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusPreferenceService.isPreferredNameExist("th17", "TH1")).thenReturn(true);

        bean.savePreferences();

        verify(persistService, never()).saveAll(any(), any(), any(), any(), any(), any());
        assertTrue(bean.isPreferenceSaveError());
        assertTrue(bean.getPreferenceSaveMessage().contains("PreferredName"));
    }

    @Test
    void savePreferences_isDeniedWhenUserCannotEdit() {
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.savePreferences();

        verify(persistService, never()).saveAll(any(), any(), any(), any(), any(), any());
        assertTrue(bean.isPreferenceSaveError());
        assertEquals("Action non autorisée", bean.getPreferenceSaveMessage());
    }

    private void grantEdit() {
        lenient().when(userSession.getCurrentUserId()).thenReturn(2);
        lenient().when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
    }
}
