package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
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
    private ThesaurusContext thesaurusContext;
    @Mock
    private WorkshopImportBean workshopImportBean;

    private WorkshopBean bean;

    @BeforeEach
    void setUp() {
        bean = new WorkshopBean(userSession, thesaurusContext, workshopImportBean);
    }

    @Test
    void screenAvailable_requiresLoginAndThesaurusFromContext() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void screenUnavailableWithoutThesaurus() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void screenUnavailableForGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void actionsAvailable_requiresAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isActionsAvailable());
    }

    @Test
    void actionsUnavailableForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

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
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        bean.load();

        verify(thesaurusContext).syncFromViewParams();
        verify(workshopImportBean).prepare();
    }

    @Test
    void load_skipsBulkImportForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        bean.load();

        verify(workshopImportBean, never()).prepare();
    }

    @Test
    void load_showsErrorWhenThesaurusMissing() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.load();
        }

        verify(workshopImportBean, never()).prepare();
    }

    @Test
    void prepareBulkActions_initializesImportForAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        bean.prepareBulkActions();

        verify(workshopImportBean).prepare();
    }

    @Test
    void prepareBulkActions_skipsImportForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        bean.prepareBulkActions();

        verify(workshopImportBean, never()).prepare();
    }
}
