package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.service.PlatformHomeReadService;
import fr.cnrs.opentheso.v2.shared.session.SsoSessionBridge;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationShellBeanTest {

    @Mock
    private ConsultationCatalogService consultationCatalogService;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    @Mock
    private PlatformHomeReadService platformHomeReadService;
    @Mock
    private SsoSessionBridge ssoSessionBridge;

    private ConsultationShellBean consultationShellBean;

    @BeforeEach
    void setUp() {
        consultationShellBean = new ConsultationShellBean(
                consultationCatalogService,
                thesaurusContext,
                userSession,
                v2LocaleBean,
                thesaurusSettingsQueryRepository,
                platformHomeReadService,
                ssoSessionBridge
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
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(5, "TH1")).thenReturn(Optional.of(2));

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
}
