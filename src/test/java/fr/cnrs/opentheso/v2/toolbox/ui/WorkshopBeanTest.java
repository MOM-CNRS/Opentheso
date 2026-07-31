package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private WorkshopImportBean workshopImportBean;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;

    private WorkshopBean bean;

    @BeforeEach
    void setUp() {
        bean = new WorkshopBean(userSession, toolboxAccessPolicy, thesaurusContext, workshopImportBean, thesaurusAccessService);
    }

    @Test
    void screenAvailable_requiresLoginAndThesaurusFromContext() {
        stubScreenAccess();

        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void screenUnavailableWithoutThesaurus() {
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.hasSelectedThesaurus("")).thenReturn(false);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void screenUnavailableForGuest() {
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(false);

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void actionsAvailable_requiresAdminOnCurrentThesaurus() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(7, false, "TH1")).thenReturn(true);

        assertTrue(bean.isActionsAvailable());
    }

    @Test
    void actionsUnavailableWithoutThesaurusAdmin() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(7, false, "TH1")).thenReturn(false);

        assertFalse(bean.isActionsAvailable());
    }

    @Test
    void getThesaurusTitle_prefersContextTitle() {
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Thésaurus A");

        assertEquals("Thésaurus A", bean.getThesaurusTitle());
    }

    @Test
    void getThesaurusTitle_fallsBackToId() {
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn(null);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH2");

        assertEquals("TH2", bean.getThesaurusTitle());
    }

    @Test
    void load_preparesBulkImportWhenAccessGranted() {
        stubAdminAccess();

        bean.load();

        verify(thesaurusContext).syncFromViewParams();
        verify(workshopImportBean).prepare();
    }

    @Test
    void load_skipsBulkImportForNonAdmin() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(7, false, "TH1")).thenReturn(false);

        bean.load();

        verify(workshopImportBean, never()).prepare();
    }

    @Test
    void load_showsErrorWhenThesaurusMissing() {
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.hasSelectedThesaurus("")).thenReturn(false);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.load();
        }

        verify(workshopImportBean, never()).prepare();
    }

    @Test
    void prepareBulkActions_initializesImportForAdmin() {
        stubAdminAccess();

        bean.prepareBulkActions();

        verify(workshopImportBean).prepare();
    }

    @Test
    void prepareBulkActions_skipsImportForNonAdmin() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(7, false, "TH1")).thenReturn(false);

        bean.prepareBulkActions();

        verify(workshopImportBean, never()).prepare();
    }

    private void stubScreenAccess() {
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.hasSelectedThesaurus("TH1")).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
    }

    private void stubAdminAccess() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(7, false, "TH1")).thenReturn(true);
    }
}
