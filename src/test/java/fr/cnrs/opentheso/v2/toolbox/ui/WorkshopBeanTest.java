package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.importexport.ImportFileBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.toolbox.atelier.AtelierThesBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.service.WorkshopService;
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
    private SelectedTheso selectedTheso;
    @Mock
    private ImportFileBean importFileBean;
    @Mock
    private AtelierThesBean atelierThesBean;

    private WorkshopBean bean;

    @BeforeEach
    void setUp() {
        bean = new WorkshopBean(
                userSession,
                thesaurusContext,
                selectedTheso,
                importFileBean,
                atelierThesBean,
                new WorkshopService()
        );
    }

    @Test
    void screenAvailable_requiresLoginAndThesaurusFromContext() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void screenAvailable_usesSelectedThesaurusFallback() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH2");

        assertTrue(bean.isScreenAvailable());
        assertEquals("TH2", bean.getThesaurusId());
    }

    @Test
    void screenUnavailableWithoutThesaurus() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("");

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
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isActionsAvailable());
    }

    @Test
    void actionsUnavailableForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        assertFalse(bean.isActionsAvailable());
    }

    @Test
    void getThesaurusTitle_prefersContextTitle() {
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Thésaurus A");
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        assertEquals("Thésaurus A", bean.getThesaurusTitle());
    }

    @Test
    void getThesaurusTitle_fallsBackToSelectedThesaurusName() {
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("");
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");
        when(selectedTheso.getThesoName()).thenReturn("Thésaurus B");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH2");

        assertEquals("Thésaurus B", bean.getThesaurusTitle());
    }

    @Test
    void load_initializesWorkshopWhenAccessGranted() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        bean.load();

        verify(thesaurusContext).syncFromViewParams();
        verify(atelierThesBean).init();
        verify(importFileBean).init();
    }

    @Test
    void load_initializesAtelierOnlyForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        bean.load();

        verify(atelierThesBean).init();
        verify(importFileBean, never()).init();
    }

    @Test
    void load_showsErrorWhenThesaurusMissing() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.load();
        }

        verify(atelierThesBean, never()).init();
        verify(importFileBean, never()).init();
    }

    @Test
    void prepareBulkActions_initializesImportFileBeanForAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        bean.prepareBulkActions();

        verify(importFileBean).init();
    }

    @Test
    void prepareBulkActions_skipsImportForNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");

        bean.prepareBulkActions();

        verify(importFileBean, never()).init();
    }
}
