package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageFlag;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.LanguageFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagManagementBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private LanguageFlagService languageFlagService;

    private FlagManagementBean bean;

    @BeforeEach
    void setUp() {
        bean = new FlagManagementBean(userSession, toolboxAccessPolicy, languageFlagService);
    }

    @Test
    void load_listsFlagsForSuperAdmin() {
        when(toolboxAccessPolicy.canManageLanguageFlags(userSession)).thenReturn(true);
        when(languageFlagService.listAll()).thenReturn(List.of(new LanguageFlag("fr", "fr")));

        bean.load();

        assertEquals(1, bean.getLanguages().size());
        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void load_clearsListWhenAccessDenied() {
        when(toolboxAccessPolicy.canManageLanguageFlags(userSession)).thenReturn(false);

        bean.load();

        assertTrue(bean.getLanguages().isEmpty());
        verify(languageFlagService, never()).listAll();
    }

    @Test
    void onCellEdit_persistsThenReloadsPageLikeLegacy() {
        when(toolboxAccessPolicy.canManageLanguageFlags(userSession)).thenReturn(true);
        var language = new LanguageFlag("fr", "CA");
        when(languageFlagService.listAll()).thenReturn(List.of(language));

        @SuppressWarnings("unchecked")
        CellEditEvent<LanguageFlag> event = mock(CellEditEvent.class);
        when(event.getRowData()).thenReturn(language);
        org.mockito.Mockito.doReturn("CA").when(event).getNewValue();

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        try (MockedStatic<PrimeFaces> pf = mockStatic(PrimeFaces.class)) {
            pf.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.onCellEdit(event);

            verify(languageFlagService).updateCountryCode("fr", "CA");
            verify(primeFaces).executeScript("window.location.reload();");
        }
    }
}
