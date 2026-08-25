package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ThesaurusMaintenanceService thesaurusMaintenanceService;
    @Mock
    private ThesaurusViewBean thesaurusViewBean;

    private MaintenanceBean bean;

    @BeforeEach
    void setUp() {
        bean = new MaintenanceBean(
                userSession,
                toolboxAccessPolicy,
                thesaurusContext,
                thesaurusMaintenanceService,
                thesaurusViewBean
        );
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
            messages.verify(() -> MessageUtils.showInformationMessage("Correction réussie, concepts affectés : 4"));
        }

        verify(thesaurusMaintenanceService).correctDisplayTopTerm("TH1");
        verify(thesaurusViewBean).reloadTree();
        assertEquals("à l'instant", bean.getTopTermLastRunLabel());
        assertTrue(bean.isLastOk());
    }

    @Test
    void reorganizeHierarchy_reloadsTreeWhenSuccessful() {
        stubAccess();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.reorganizeHierarchy();
            messages.verify(() -> MessageUtils.showInformationMessage("Correction réussie !!!"));
        }

        verify(thesaurusMaintenanceService).reorganizeHierarchy("TH1");
        verify(thesaurusViewBean).reloadTree();
        assertEquals("à l'instant", bean.getRestructureLastRunLabel());
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
        assertFalse(bean.isLastOk());
        assertEquals("jamais", bean.getCollectionsLastRunLabel());
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

        verify(thesaurusViewBean).reloadTree();
        assertEquals("à l'instant", bean.getCollectionsLastRunLabel());
    }

    @Test
    void switchRolesFromTermToConcept_doesNotReloadTree() {
        stubAccess();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.switchRolesFromTermToConcept();
            messages.verify(() -> MessageUtils.showInformationMessage("Correction réussie !!!"));
        }

        verify(thesaurusMaintenanceService).switchRolesFromTermToConcept("TH1");
        verify(thesaurusViewBean, never()).reloadTree();
        assertEquals("à l'instant", bean.getRolesLastRunLabel());
    }

    @Test
    void generateArkFromConceptId_requiresNaan() {
        stubAccess();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.generateArkFromConceptId();
            messages.verify(() -> MessageUtils.showErrorMessage("Le NAAN est obligatoire"));
        }

        verify(thesaurusMaintenanceService, never()).generateArkFromConceptId(
                anyString(), anyString(), anyString(), anyBoolean());
        assertFalse(bean.isLastOk());
        assertEquals("jamais", bean.getArkLastRunLabel());
    }

    @Test
    void generateArkFromConceptId_reportsChangedCount() {
        stubAccess();
        bean.getArkEditor().setNaan("66666");
        bean.getArkEditor().setPrefix("ndp");
        bean.getArkEditor().setOverwrite(true);
        when(thesaurusMaintenanceService.generateArkFromConceptId("TH1", "ndp", "66666", true)).thenReturn(12);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.generateArkFromConceptId();
            messages.verify(() -> MessageUtils.showInformationMessage("Concepts changés: 12"));
        }

        verify(thesaurusViewBean, never()).reloadTree();
        assertEquals("à l'instant", bean.getArkLastRunLabel());
    }

    @Test
    void generateLocalArk_reportsChangedCount() {
        stubAccess();
        bean.getArkEditor().setOverwriteLocalArk(true);
        when(thesaurusMaintenanceService.generateLocalArk("TH1", true)).thenReturn(9);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.generateLocalArk();
            messages.verify(() -> MessageUtils.showInformationMessage("Concepts changés: 9"));
        }

        verify(thesaurusViewBean, never()).reloadTree();
        assertEquals("à l'instant", bean.getArkLastRunLabel());
    }

    @Test
    void screenUnavailableWithoutThesaurus() {
        when(toolboxAccessPolicy.canAccessMaintenance(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.hasSelectedThesaurus("")).thenReturn(false);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");

        assertFalse(bean.isScreenAvailable());
    }

    private void stubAccess() {
        when(toolboxAccessPolicy.canAccessMaintenance(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.hasSelectedThesaurus("TH1")).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
    }
}
