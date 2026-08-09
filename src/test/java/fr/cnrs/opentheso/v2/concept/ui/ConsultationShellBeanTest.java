package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.service.PlatformHomeReadService;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import fr.cnrs.opentheso.v2.shared.session.SsoSessionBridge;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.primefaces.PrimeFaces;

@ExtendWith(MockitoExtension.class)
class ConsultationShellBeanTest {

    @Mock
    private ConsultationCatalogService consultationCatalogService;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private PlatformHomeReadService platformHomeReadService;
    @Mock
    private SsoSessionBridge ssoSessionBridge;
    @Mock
    private SessionLifecycleService sessionLifecycleService;
    @Mock
    private RightsService rightsService;
    @Mock
    private FacesContext facesContext;
    @Mock
    private ExternalContext externalContext;

    private ConsultationShellBean consultationShellBean;

    @BeforeEach
    void setUp() {
        consultationShellBean = new ConsultationShellBean(
                consultationCatalogService,
                thesaurusContext,
                conceptSelectionContext,
                userSession,
                v2LocaleBean,
                platformHomeReadService,
                ssoSessionBridge,
                sessionLifecycleService,
                rightsService
        );
    }

    @Test
    void load_consumesSsoAndRefreshesCatalog() {
        when(ssoSessionBridge.consumePendingThesaurusId()).thenReturn("TH1");
        when(ssoSessionBridge.consumePendingConceptId()).thenReturn("C1");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(userSession.isLoggedIn()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(consultationCatalogService.listProjects(null, false)).thenReturn(List.of());
        when(consultationCatalogService.listThesauri(null, false, -1, "fr")).thenReturn(List.of());
        when(platformHomeReadService.loadHomePageHtml("fr")).thenReturn("<p>home</p>");

        consultationShellBean.load();

        verify(ssoSessionBridge).consumePendingSsoLogin();
        verify(thesaurusContext).setIdThesoFromUri("TH1");
        verify(thesaurusContext).setIdConceptFromUri("C1");
        assertEquals("<p>home</p>", consultationShellBean.getPlatformHomeHtml());
    }

    @Test
    void hasSelectedThesaurus_reflectsContext() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        assertTrue(consultationShellBean.hasSelectedThesaurus());
    }

    @Test
    void isAdminOnCurrentThesaurus_checksRole() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(rightsService.canOnThesaurus(5, fr.cnrs.opentheso.v2.rights.Permission.MANAGE_THESAURUS, "TH1"))
                .thenReturn(true);

        assertTrue(consultationShellBean.isAdminOnCurrentThesaurus());
    }

    @Test
    void isAdminOnCurrentThesaurus_deniesWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(consultationShellBean.isAdminOnCurrentThesaurus());
    }

    @Test
    void getCurrentThesaurusTitle_usesContextTitleFirst() {
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Mon thésaurus");

        assertEquals("Mon thésaurus", consultationShellBean.getCurrentThesaurusTitle());
    }

    @Test
    void getSearchableThesaurusIds_delegatesToCatalog() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(3);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(consultationCatalogService.listSearchableThesaurusIds(3, false, -1, "fr"))
                .thenReturn(List.of("TH1", "TH2"));

        assertEquals(List.of("TH1", "TH2"), consultationShellBean.getSearchableThesaurusIds());
    }

    @Test
    void afterLogout_clearsUnavailableThesaurus() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        consultationShellBean.setThesaurusOptions(List.of(new ConsultationThesaurusOption("TH2", "Other", "fr")));
        when(userSession.isLoggedIn()).thenReturn(false);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(consultationCatalogService.listProjects(null, false)).thenReturn(List.of());
        when(consultationCatalogService.listThesauri(null, false, -1, "fr")).thenReturn(List.of());

        consultationShellBean.afterLogout();

        verify(thesaurusContext).clearSelection();
    }

    @Test
    void clearSession_clearsSelectionThenDelegatesToLifecycle() throws Exception {
        consultationShellBean.clearSession();

        verify(thesaurusContext).clearSelection();
        verify(sessionLifecycleService).clearAndRedirectFromFaces();
    }

    @Test
    void load_showsWarningWhenSessionExpiredParamPresent() {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestParameterMap())
                .thenReturn(Map.of(SessionLifecycleService.PARAM_SESSION_EXPIRED, "1"));
        when(v2LocaleBean.getMsg("session.expired")).thenReturn("Session expirée");
        when(ssoSessionBridge.consumePendingThesaurusId()).thenReturn(null);
        when(ssoSessionBridge.consumePendingConceptId()).thenReturn(null);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(userSession.isLoggedIn()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(consultationCatalogService.listProjects(null, false)).thenReturn(List.of());
        when(consultationCatalogService.listThesauri(null, false, -1, "fr")).thenReturn(List.of());
        when(platformHomeReadService.loadHomePageHtml("fr")).thenReturn("");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class);
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            consultationShellBean.load();

            messages.verify(() -> MessageUtils.showWarnMessage("Session expirée"));
        }
    }

    @Test
    void load_showsInfoWhenLogoutParamPresent() {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestParameterMap())
                .thenReturn(Map.of(SessionLifecycleService.PARAM_LOGOUT, "1"));
        when(v2LocaleBean.getMsg("connect.goodbye")).thenReturn("Au revoir");
        when(ssoSessionBridge.consumePendingThesaurusId()).thenReturn(null);
        when(ssoSessionBridge.consumePendingConceptId()).thenReturn(null);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(userSession.isLoggedIn()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(consultationCatalogService.listProjects(null, false)).thenReturn(List.of());
        when(consultationCatalogService.listThesauri(null, false, -1, "fr")).thenReturn(List.of());
        when(platformHomeReadService.loadHomePageHtml("fr")).thenReturn("");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class);
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            consultationShellBean.load();

            messages.verify(() -> MessageUtils.showInformationMessage("Au revoir"));
        }
    }

    @Test
    void reloadThesaurus_clearsConceptAndReloadsWhenBrowseView() throws Exception {
        consultationShellBean.setSelectedThesaurusId("TH1");
        consultationShellBean.setThesaurusOptions(List.of(new ConsultationThesaurusOption("TH1", "Title", "fr")));
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(consultationCatalogService.listProjects(null, false)).thenReturn(List.of());
        when(consultationCatalogService.listThesauri(null, false, -1, "fr"))
                .thenReturn(List.of(new ConsultationThesaurusOption("TH1", "Title", "fr")));

        jakarta.faces.component.UIViewRoot viewRoot = mock(jakarta.faces.component.UIViewRoot.class);
        when(facesContext.getViewRoot()).thenReturn(viewRoot);
        when(viewRoot.getViewId()).thenReturn("/v2/thesaurus/browse.xhtml");
        when(facesContext.getApplication()).thenReturn(mock(jakarta.faces.application.Application.class));

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class);
             MockedStatic<PrimeFaces> primeFaces = mockStatic(PrimeFaces.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            PrimeFaces primeFacesInstance = mock(PrimeFaces.class);
            primeFaces.when(PrimeFaces::current).thenReturn(primeFacesInstance);

            consultationShellBean.reloadThesaurus();

            verify(thesaurusContext).setFromUrl(false);
            verify(thesaurusContext).setIdConceptFromUri(null);
            verify(thesaurusContext).setIdGroupFromUri(null);
            verify(thesaurusContext).setIdFacetFromUri(null);
            verify(thesaurusContext).selectThesaurus("TH1", "Title", "fr");
            verify(conceptSelectionContext).clear();
            verify(primeFacesInstance).executeScript(org.mockito.ArgumentMatchers.contains("window.location.reload()"));
        }
    }
}
