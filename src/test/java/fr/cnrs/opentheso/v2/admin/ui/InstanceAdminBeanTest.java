package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceAdminBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private AdminCatalogService adminCatalogService;

    private InstanceAdminBean bean;

    @BeforeEach
    void setUp() {
        bean = new InstanceAdminBean(userSession, v2LocaleBean, adminCatalogService);
    }

    @Test
    void init_loadsAccountsAndThesauriForSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of(
                new InstanceAdminAccount(1, "admin", "a@x.fr", "CNRS", LocalDateTime.now(), "super_admin", "Super admin")
        ));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 1, "Frantiq", false, null)
        ));

        bean.init();

        assertEquals(1, bean.getAccounts().size());
        assertEquals(1, bean.getThesauri().size());
        assertTrue(bean.isHome());
    }

    @Test
    void init_clearsWhenNotSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(false);

        bean.init();

        assertTrue(bean.getAccounts().isEmpty());
        verifyNoInteractions(adminCatalogService);
    }

    @Test
    void openSection_users_reloadsAndSetsSection() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());

        bean.openSection(InstanceAdminBean.SECTION_USERS);

        assertTrue(bean.isUsersSection());
        verify(adminCatalogService).listInstanceAccounts(true);
    }

    @Test
    void goBack_fromMembers_returnsToThesauriList() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        bean.setSection(InstanceAdminBean.SECTION_THESAURI);
        bean.setOpenThesaurusId("th1");

        bean.goBack();

        assertTrue(bean.isThesauriSection());
    }
}
