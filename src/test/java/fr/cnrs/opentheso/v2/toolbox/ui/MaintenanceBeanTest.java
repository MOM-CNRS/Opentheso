package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ThesaurusMaintenanceService thesaurusMaintenanceService;

    private MaintenanceBean bean;

    @BeforeEach
    void setUp() {
        bean = new MaintenanceBean(userSession, thesaurusContext, thesaurusMaintenanceService);
    }

    @Test
    void load_loadsLocalArkSettingsWhenAccessGranted() {
        stubAccess();
        when(thesaurusMaintenanceService.loadLocalArkSettings("TH1"))
                .thenReturn(new LocalArkSettings("12345", "ark", 8));

        bean.load();

        assertEquals("12345", bean.getLocalArkSettings().getNaan());
        verify(thesaurusContext).syncFromViewParams();
    }

    @Test
    void correctDisplayTopTerm_showsMessageWhenSuccessful() {
        stubAccess();
        when(thesaurusMaintenanceService.correctDisplayTopTerm("TH1")).thenReturn(4);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.correctDisplayTopTerm();
        }

        verify(thesaurusMaintenanceService).correctDisplayTopTerm("TH1");
    }

    @Test
    void reorganizeConceptsAndCollections_requiresSuperAdmin() {
        stubAccess();
        when(userSession.isSuperAdmin()).thenReturn(false);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.reorganizeConceptsAndCollections();
            messages.verify(() -> MessageUtils.showWarnMessage("Action réservée aux super-administrateurs"));
        }

        verify(thesaurusMaintenanceService, never()).reorganizeConceptsAndCollections("TH1");
    }

    @Test
    void reorganizeConceptsAndCollections_reportsCleanedCountForSuperAdmin() {
        stubAccess();
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(thesaurusMaintenanceService.reorganizeConceptsAndCollections("TH1")).thenReturn(7);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.reorganizeConceptsAndCollections();
            messages.verify(() -> MessageUtils.showInformationMessage(
                    "Correction réussie !!! Liens collection/concept nettoyés : 7"));
        }
    }

    @Test
    void screenUnavailableWithoutThesaurus() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");

        assertFalse(bean.isScreenAvailable());
    }

    private void stubAccess() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
    }
}
