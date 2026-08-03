package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.EditionCollectionNode;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusLanguage;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifyThesaurusBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;
    @Mock
    private ModifyThesaurusService modifyThesaurusService;
    @Mock
    private EditionBean editionBean;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private FacesContext facesContext;
    @Mock
    private jakarta.faces.application.Application application;

    private ModifyThesaurusBean bean;

    @BeforeEach
    void setUp() {
        bean = new ModifyThesaurusBean(userSession, thesaurusAccessService, modifyThesaurusService);
    }

    @Test
    void load_initializesStateWhenAccessGranted() {
        stubManageAccess();
        when(modifyThesaurusService.loadDetails("TH1"))
                .thenReturn(new EditionThesaurusDetails("TH1", "Titre", "ark/1", false, "fr"));
        when(modifyThesaurusService.loadLanguages("TH1"))
                .thenReturn(List.of(new EditionThesaurusLanguage("fr", "fr", "Titre", "Français")));
        when(modifyThesaurusService.loadAllLanguages())
                .thenReturn(List.of(new LanguageOption("fr", "fr", "Français", "French")));
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        when(modifyThesaurusService.loadCollectionTree("TH1")).thenReturn(new DefaultTreeNode<>(null, null));
        when(modifyThesaurusService.isMasterThesaurus("TH1")).thenReturn(false);

        bean.load("TH1");

        assertTrue(bean.isFormAvailable());
        assertEquals("TH1", bean.getThesaurusId());
        assertEquals("fr", bean.getSourceLang());
        assertEquals(1, bean.getLanguages().size());
    }

    @Test
    void load_clearsStateWhenAccessDenied() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(false);

        bean.load("TH1");

        assertFalse(bean.isFormAvailable());
        assertTrue(bean.getLanguages().isEmpty());
        verify(modifyThesaurusService, never()).loadDetails(anyString());
    }

    @Test
    void changeSourceLanguage_refreshesDetailsOnSuccess() {
        stubManageAccess();
        bean.setThesaurusId("TH1");
        bean.setSourceLang("en");
        when(modifyThesaurusService.loadDetails("TH1"))
                .thenReturn(new EditionThesaurusDetails("TH1", "Titre", "", false, "en"));
        when(modifyThesaurusService.loadLanguages("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadAllLanguages()).thenReturn(List.of());
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of());
        when(modifyThesaurusService.loadCollectionTree("TH1")).thenReturn(new DefaultTreeNode<>(null, null));
        when(modifyThesaurusService.isMasterThesaurus("TH1")).thenReturn(false);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.changeSourceLanguage();
        }

        verify(modifyThesaurusService).changeSourceLanguage("TH1", "en");
        assertEquals("en", bean.getSourceLang());
    }

    @Test
    void addLanguage_refreshesLanguagesOnSuccess() {
        stubManageAccess();
        bean.setThesaurusId("TH1");
        bean.setAddLanguageEditor(AddLanguageEditor.empty());
        bean.getAddLanguageEditor().setTitle("Label");
        bean.getAddLanguageEditor().setSelectedLanguage("fr");
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(modifyThesaurusService.loadLanguages("TH1"))
                .thenReturn(List.of(new EditionThesaurusLanguage("fr", "fr", "Label", "Français")));

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class);
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             var primeFaces = PrimeFacesTestSupport.open()) {
            stubConsultationShellLookup(faces);
            bean.addLanguage();
        }

        verify(modifyThesaurusService).addLanguage("TH1", "Label", "fr", "admin");
        verify(consultationShellBean).refreshHeaderCatalog();
        assertEquals(1, bean.getLanguages().size());
    }

    @Test
    void updateCollectionStatus_updatesDescendantsInBulk() {
        stubManageAccess();
        bean.setThesaurusId("TH1");

        EditionCollectionNode rootData = new EditionCollectionNode();
        rootData.setId("root");
        rootData.setLabel("Racine");
        rootData.setPrivateCollection(true);

        EditionCollectionNode childData = new EditionCollectionNode();
        childData.setId("child");
        childData.setLabel("Enfant");
        childData.setPrivateCollection(false);

        TreeNode<EditionCollectionNode> root = new DefaultTreeNode<>(null, null);
        TreeNode<EditionCollectionNode> rootNode = new DefaultTreeNode<>(rootData, root);
        new DefaultTreeNode<>(childData, rootNode);
        bean.setCollectionRoot(root);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.updateCollectionStatus(rootData);
        }

        verify(modifyThesaurusService).updateCollectionsVisibility(eq("TH1"), anyList(), eq(true));
        assertTrue(childData.isPrivateCollection());
    }

    @Test
    void onMetadataRowEdit_savesMetadata() {
        stubManageAccess();
        bean.setThesaurusId("TH1");
        EditionMetadata metadata = EditionMetadata.emptyRow();
        metadata.setValue("valeur");
        @SuppressWarnings("unchecked")
        RowEditEvent<EditionMetadata> event = mock(RowEditEvent.class);
        when(event.getObject()).thenReturn(metadata);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.onMetadataRowEdit(event);
        }

        verify(modifyThesaurusService).saveMetadata("TH1", metadata);
    }

    @Test
    void backToList_delegatesToEditionBean() {
        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            stubEditionBeanLookup(faces);

            bean.backToList();
        }

        verify(editionBean).showList();
    }

    private void stubManageAccess() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(true);
    }

    private void stubFacesContext(MockedStatic<FacesContext> faces) {
        faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        when(facesContext.getApplication()).thenReturn(application);
    }

    private void stubEditionBeanLookup(MockedStatic<FacesContext> faces) {
        stubFacesContext(faces);
        when(application.evaluateExpressionGet(eq(facesContext), eq("#{v2EditionBean}"), eq(EditionBean.class)))
                .thenReturn(editionBean);
    }

    private void stubConsultationShellLookup(MockedStatic<FacesContext> faces) {
        stubFacesContext(faces);
        when(application.evaluateExpressionGet(
                eq(facesContext),
                eq("#{v2ConsultationShellBean}"),
                eq(ConsultationShellBean.class)
        )).thenReturn(consultationShellBean);
    }
}
