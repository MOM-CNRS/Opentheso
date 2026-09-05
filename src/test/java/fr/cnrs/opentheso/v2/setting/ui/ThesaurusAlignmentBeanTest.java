package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusAlignmentBeanTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ConceptAlignmentAdminService conceptAlignmentAdminService;

    private ThesaurusAlignmentBean bean;

    @BeforeEach
    void setUp() {
        ThesaurusContext thesaurusContext = new ThesaurusContext(
                thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        SettingsAccess access = new SettingsAccess(thesaurusContext, userSession, rightsService);
        bean = new ThesaurusAlignmentBean(access, conceptAlignmentAdminService);
    }

    @Test
    void getAlignmentSources_loadsAllSourcesWithActivationForCurrentThesaurus() {
        when(conceptAlignmentAdminService.listSourcesForManagement("th17")).thenReturn(List.of(
                new AlignmentSourceItem(1, "Wikidata", "Wiki", true, true, "wikidata", "https://www.wikidata.org/")
        ));

        assertEquals(1, bean.getAlignmentSources().size());
        assertTrue(bean.getAlignmentSources().get(0).isSelected());
    }

    @Test
    void toggleAlignmentSource_staysInDraft() {
        grantEdit();
        when(conceptAlignmentAdminService.listSourcesForManagement("th17")).thenReturn(List.of(
                new AlignmentSourceItem(1, "Wikidata", "Wiki", false, true, "wikidata", "https://www.wikidata.org/")
        ));

        bean.toggleAlignmentSource(1);

        assertTrue(bean.getAlignmentSources().get(0).isSelected());
        verify(conceptAlignmentAdminService, never()).setSourceSelected(org.mockito.ArgumentMatchers.any(), anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void createAlignmentSource_keepsOpenthesoSourceInDraft() {
        grantEdit();
        when(conceptAlignmentAdminService.listSourcesForManagement("th17")).thenReturn(List.of());
        when(conceptAlignmentAdminService.validateOpenthesoSource(
                "Pactols", "https://opentheso.example", "th1")).thenReturn(null);
        bean.setNewAlignmentSourceName("Pactols");
        bean.setNewAlignmentSourceUri("https://opentheso.example");
        bean.setNewAlignmentSourceThesaurusId("th1");
        bean.setNewAlignmentSourceDescription("Thésaurus distant");

        bean.createAlignmentSource();

        verify(conceptAlignmentAdminService, never()).addLocalSource(
                org.mockito.ArgumentMatchers.any(), anyInt(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
        assertEquals(1, bean.getAlignmentSources().size());
        assertEquals(
                "https://opentheso.example/api/search?q=##value##&lang=##lang##&theso=th1",
                bean.getAlignmentSources().get(0).getUrl());
        assertTrue(bean.getAlignmentMessage().contains("Enregistrer"));
    }

    @Test
    void createAlignmentSource_keepsDialogOpenWhenServiceRejects() {
        grantEdit();
        when(conceptAlignmentAdminService.validateOpenthesoSource(
                "Pactols", "https://opentheso.example", "th1")).thenReturn("Ping impossible");
        bean.setNewAlignmentSourceName("Pactols");
        bean.setNewAlignmentSourceUri("https://opentheso.example");
        bean.setNewAlignmentSourceThesaurusId("th1");
        bean.prepareCreateAlignmentSource();
        bean.setNewAlignmentSourceName("Pactols");
        bean.setNewAlignmentSourceUri("https://opentheso.example");
        bean.setNewAlignmentSourceThesaurusId("th1");

        bean.createAlignmentSource();

        assertTrue(bean.isAlignmentDialogOpen());
        assertEquals("Ping impossible", bean.getAlignmentMessage());
    }

    @Test
    void prepareEditAlignmentSource_ignoresGlobalSourceForThesaurusAdmin() {
        grantEdit();
        when(userSession.isSuperAdmin()).thenReturn(false);
        AlignmentSourceItem global = new AlignmentSourceItem(
                8, "Wikidata", "", true, true, "wikidata", "https://wikidata.org/", "other");

        bean.prepareEditAlignmentSource(global);

        assertFalse(bean.isAlignmentDialogOpen());
    }

    @Test
    void prepareEditAlignmentSource_opensGlobalSourceWhenSuperAdmin() {
        grantEdit();
        when(userSession.isSuperAdmin()).thenReturn(true);
        AlignmentSourceItem global = new AlignmentSourceItem(
                8, "Wikidata", "", true, true, "wikidata", "https://wikidata.org/", "other");

        bean.prepareEditAlignmentSource(global);

        assertTrue(bean.isAlignmentEditDialog());
        assertEquals("Wikidata", bean.getNewAlignmentSourceName());
    }

    @Test
    void deleteAlignmentSource_keepsRemovalInDraft() {
        grantEdit();
        when(conceptAlignmentAdminService.listSourcesForManagement("th17")).thenReturn(List.of(
                new AlignmentSourceItem(2, "Local", "Opentheso local", true, false, "Opentheso", "https://ex.org/")
        ));
        bean.prepareDeleteAlignmentSource(bean.getAlignmentSources().get(0));

        bean.deleteAlignmentSource();

        verify(conceptAlignmentAdminService, never()).deleteLocalSource(anyInt());
        assertTrue(bean.getAlignmentSources().isEmpty());
        assertEquals(1, bean.toPersistDraft().idsToDelete().size());
    }

    private void grantEdit() {
        lenient().when(userSession.getCurrentUserId()).thenReturn(2);
        lenient().when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
    }
}
