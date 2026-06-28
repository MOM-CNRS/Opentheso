package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.fixtures.ToolboxTestFixtures;
import fr.cnrs.opentheso.v2.toolbox.model.EditionView;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditionBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private SelectedTheso selectedTheso;
    @Mock
    private LanguageBean languageBean;
    @Mock
    private EditionThesaurusService editionThesaurusService;
    @Mock
    private NewThesaurusBean newThesaurusBean;
    @Mock
    private ModifyThesaurusBean modifyThesaurusBean;
    @Mock
    private FacesContext facesContext;

    private EditionBean bean;

    @BeforeEach
    void setUp() {
        bean = new EditionBean(
                userSession,
                currentUser,
                thesaurusContext,
                selectedTheso,
                languageBean,
                editionThesaurusService,
                newThesaurusBean,
                modifyThesaurusBean
        );
    }

    @Test
    void load_listsThesauriWhenAccessGranted() {
        stubListAccess();
        when(editionThesaurusService.listAdminThesauri(2, false))
                .thenReturn(List.of(ToolboxTestFixtures.sampleThesaurus()));

        bean.load();

        assertEquals(1, bean.getThesaurusList().size());
        assertTrue(bean.isScreenAvailable());
        assertEquals(EditionView.LIST, bean.getCurrentView());
        verify(thesaurusContext).syncFromViewParams();
    }

    @Test
    void load_clearsListWhenAccessDenied() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(List.of());

        bean.load();

        assertTrue(bean.getThesaurusList().isEmpty());
        verify(editionThesaurusService, never()).listAdminThesauri(anyInt(), anyBoolean());
    }

    @Test
    void showList_refreshesThesaurusList() {
        stubListAccess();
        bean.setCurrentView(EditionView.NEW);
        when(editionThesaurusService.listAdminThesauri(2, false))
                .thenReturn(List.of(ToolboxTestFixtures.sampleThesaurus()));

        bean.showList();

        assertEquals(EditionView.LIST, bean.getCurrentView());
        assertEquals(1, bean.getThesaurusList().size());
        assertNull(bean.getSelectedThesaurusForAction());
    }

    @Test
    void showList_skipsRefreshWhenAccessDenied() {
        bean.setCurrentView(EditionView.NEW);

        bean.showList();

        verify(editionThesaurusService, never()).listAdminThesauri(anyInt(), anyBoolean());
    }

    @Test
    void showNewThesaurus_preparesDedicatedBean() {
        stubCreateAccess();

        bean.showNewThesaurus();

        verify(newThesaurusBean).prepareForm();
        assertEquals(EditionView.NEW, bean.getCurrentView());
        assertTrue(bean.isNewView());
    }

    @Test
    void showNewThesaurus_deniedWhenUserCannotCreate() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(List.of());

        bean.showNewThesaurus();

        assertEquals(EditionView.LIST, bean.getCurrentView());
        assertFalse(bean.isCanCreateOrImport());
        verify(newThesaurusBean, never()).prepareForm();
    }

    @Test
    void showImportSkos_navigatesWhenAllowed() {
        stubCreateAccess();

        bean.showImportSkos();

        assertEquals(EditionView.IMPORT_SKOS, bean.getCurrentView());
        assertTrue(bean.isImportSkosView());
    }

    @Test
    void showImportCsv_navigatesWhenAllowed() {
        stubCreateAccess();

        bean.showImportCsv();

        assertEquals(EditionView.IMPORT_CSV, bean.getCurrentView());
        assertTrue(bean.isImportCsvView());
    }

    @Test
    void showImportCsvStructure_navigatesWhenAllowed() {
        stubCreateAccess();

        bean.showImportCsvStructure();

        assertEquals(EditionView.IMPORT_CSV_STRUCTURE, bean.getCurrentView());
        assertTrue(bean.isImportCsvStructureView());
    }

    @Test
    void showModifyThesaurus_setsSelectedThesaurus() {
        stubScreenAccess();
        var thesaurus = ToolboxTestFixtures.sampleThesaurus();

        bean.showModifyThesaurus(thesaurus);

        verify(modifyThesaurusBean).load("TH1");
        assertEquals(EditionView.MODIFY, bean.getCurrentView());
        assertTrue(bean.isModifyView());
        assertEquals(thesaurus, bean.getSelectedThesaurusForAction());
    }

    @Test
    void showModifyThesaurus_skipsWhenAccessDenied() {
        var thesaurus = ToolboxTestFixtures.sampleThesaurus();

        bean.showModifyThesaurus(thesaurus);

        verify(modifyThesaurusBean, never()).load(anyString());
        assertEquals(EditionView.LIST, bean.getCurrentView());
    }

    @Test
    void showExport_withEnum_setsView() {
        var thesaurus = ToolboxTestFixtures.sampleThesaurus();

        bean.showExport(thesaurus, EditionView.EXPORT_PDF);

        assertEquals(EditionView.EXPORT_PDF, bean.getCurrentView());
        assertTrue(bean.isExportView());
    }

    @Test
    void showExport_withName_setsView() {
        var thesaurus = ToolboxTestFixtures.sampleThesaurus();

        bean.showExport(thesaurus, "EXPORT_CSV");

        assertEquals(EditionView.EXPORT_CSV, bean.getCurrentView());
        assertTrue(bean.isExportView());
    }

    @Test
    void prepareDelete_setsDeleteState() {
        var thesaurus = ToolboxTestFixtures.sampleThesaurus();

        bean.prepareDelete(thesaurus);

        assertEquals("TH1", bean.getThesaurusIdToDelete());
        assertEquals("Thésaurus test", bean.getThesaurusTitleToDelete());
        assertFalse(bean.isDeletePerennialIdentifiers());
    }

    @Test
    void deleteThesaurus_clearsSelectedThesaurusWhenDeleted() throws Exception {
        stubListAccess();
        bean.setThesaurusIdToDelete("TH1");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH1");
        doAnswer(invocation -> null).when(selectedTheso).setSelectedTheso();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             var primeFaces = PrimeFacesTestSupport.open()) {
            bean.deleteThesaurus();
        }

        verify(editionThesaurusService).deleteThesaurus("TH1", false);
        verify(selectedTheso).setSelectedIdTheso("");
        verify(thesaurusContext).setCurrentThesaurusId(null);
    }

    @Test
    void deleteThesaurus_skipsWhenIdMissing() {
        stubScreenAccess();

        bean.deleteThesaurus();

        verify(editionThesaurusService, never()).deleteThesaurus(anyString(), anyBoolean());
    }

    @Test
    void deleteThesaurus_showsErrorWhenServiceFails() {
        stubScreenAccess();
        bean.setThesaurusIdToDelete("TH1");
        doThrow(new InvalidToolboxDataException("erreur")).when(editionThesaurusService)
                .deleteThesaurus("TH1", false);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.deleteThesaurus();
        }

        verify(editionThesaurusService).deleteThesaurus("TH1", false);
    }

    @Test
    void showThesaurusStatistics_addsFacesMessage() {
        when(editionThesaurusService.loadStatistics("TH1")).thenReturn(ToolboxTestFixtures.sampleStatistics());
        when(languageBean.getMsg("info")).thenReturn("Info");
        when(languageBean.getMsg("candidat.total_concepts")).thenReturn("Concepts");
        when(languageBean.getMsg("candidat.titre")).thenReturn("Candidats");
        when(languageBean.getMsg("search.deprecated")).thenReturn("Dépréciés");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            bean.showThesaurusStatistics(ToolboxTestFixtures.sampleThesaurus());
        }

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(eq(null), captor.capture());
        assertEquals(FacesMessage.SEVERITY_INFO, captor.getValue().getSeverity());
        assertTrue(captor.getValue().getDetail().contains("120"));
    }

    private void stubScreenAccess() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
    }

    private void stubListAccess() {
        stubScreenAccess();
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
    }

    private void stubCreateAccess() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(List.of(new NodeUserRoleGroup()));
    }
}
