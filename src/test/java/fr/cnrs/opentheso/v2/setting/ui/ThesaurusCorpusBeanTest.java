package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusCorpusBeanTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;

    private ThesaurusCorpusBean bean;

    @BeforeEach
    void setUp() {
        ThesaurusContext thesaurusContext = new ThesaurusContext(
                thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        SettingsAccess access = new SettingsAccess(thesaurusContext, userSession, rightsService);
        bean = new ThesaurusCorpusBean(access, thesaurusCorpusService);
    }

    @Test
    void getCorpusList_loadsAllCorpusForCurrentThesaurus() {
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of(SettingTestFixtures.sampleCorpus()));

        assertEquals(1, bean.getCorpusList().size());
        assertEquals("Corpus A", bean.getCorpusList().get(0).getCorpusName());
    }

    @Test
    void pagedCorpusList_showsTenRowsThenNextPage() {
        List<ThesaurusCorpus> all = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            all.add(new ThesaurusCorpus("C" + i, "http://l", null, true, true, false, i));
        }
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(all);

        assertEquals(10, bean.getPagedCorpusList().size());
        bean.nextCorpusPage();
        assertEquals(2, bean.getPagedCorpusList().size());
        assertEquals("C11", bean.getPagedCorpusList().get(0).getCorpusName());
    }

    @Test
    void createCorpus_keepsChangeInDraftUntilSave() {
        grantEdit();
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        bean.setCorpusEditor(CorpusEditor.from(
                new ThesaurusCorpus("New", "http://link", null, true, true, false, null)
        ));

        bean.createCorpus();

        verify(thesaurusCorpusService, never()).createCorpus(any(), any());
        assertEquals("New", bean.getCorpusList().get(0).getCorpusName());
        assertTrue(bean.getCorpusMessage().contains("Enregistrer"));
    }

    @Test
    void toggleCorpusActive_doesNotPersist() {
        grantEdit();
        ReflectionTestUtils.setField(bean, "corpusList", new ArrayList<>(List.of(SettingTestFixtures.sampleCorpus())));
        ReflectionTestUtils.setField(bean, "corpusLoaded", true);
        ReflectionTestUtils.setField(bean, "corpusLoadedForThesaurus", "th17");

        bean.toggleCorpusActive("Corpus A");

        verify(thesaurusCorpusService, never()).setCorpusActive(any(), any(), anyBoolean());
        assertFalse(bean.getCorpusList().get(0).active());
    }

    @Test
    void prepareCreateCorpus_isDeniedWhenUserCannotEdit() {
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.prepareCreateCorpus();

        assertFalse(bean.isCorpusDialogOpen());
        assertEquals("Action non autorisée", bean.getCorpusMessage());
    }

    @Test
    void prepareEditCorpus_setsEditorFromModel() {
        grantEdit();

        bean.prepareEditCorpus(SettingTestFixtures.sampleCorpus());

        assertEquals("Corpus A", bean.getCorpusEditor().getCorpusName());
        assertTrue(bean.isCorpusFormDialog());
    }

    @Test
    void prepareEditCorpus_ignoresMissingRow() {
        grantEdit();

        bean.prepareEditCorpus(null);

        assertFalse(bean.isCorpusDialogOpen());
        assertNull(bean.getEditingCorpusName());
    }

    @Test
    void deleteCorpus_keepsRemovalInDraft() {
        grantEdit();
        ReflectionTestUtils.setField(bean, "corpusList", new ArrayList<>(List.of(SettingTestFixtures.sampleCorpus())));
        ReflectionTestUtils.setField(bean, "corpusLoaded", true);
        ReflectionTestUtils.setField(bean, "corpusLoadedForThesaurus", "th17");
        bean.prepareDeleteCorpus(SettingTestFixtures.sampleCorpus());

        bean.deleteCorpus();

        verify(thesaurusCorpusService, never()).deleteCorpus(any(), any());
        assertTrue(bean.getCorpusList().isEmpty());
    }

    private void grantEdit() {
        lenient().when(userSession.getCurrentUserId()).thenReturn(2);
        lenient().when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
    }
}
