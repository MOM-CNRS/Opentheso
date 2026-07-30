package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewThesaurusBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private NewThesaurusService newThesaurusService;
    @Mock
    private EditionBean editionBean;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private FacesContext facesContext;
    @Mock
    private jakarta.faces.application.Application application;

    private NewThesaurusBean bean;

    @BeforeEach
    void setUp() {
        bean = new NewThesaurusBean(userSession, toolboxAccessPolicy, newThesaurusService);
    }

    @Test
    void prepareForm_initializesEditorAndSelectsSingleProject() {
        stubPrepareFormAccess();
        when(newThesaurusService.loadFormOptions(2, false))
                .thenReturn(new NewThesaurusFormOptions(
                        List.of(new LanguageOption("fr", "fr", "Français", "French")),
                        List.of(new ProjectOption(7, "Projet")),
                        false
                ));

        bean.prepareForm();

        assertEquals("", bean.getEditor().getTitle());
        assertEquals("7", bean.getEditor().getSelectedProjectId());
        assertEquals(1, bean.getLanguages().size());
        assertFalse(bean.isSuperAdmin());
    }

    @Test
    void prepareForm_doesNotAutoSelectProjectForSuperAdmin() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(newThesaurusService.loadFormOptions(2, true))
                .thenReturn(new NewThesaurusFormOptions(
                        List.of(),
                        List.of(new ProjectOption(7, "Projet")),
                        true
                ));

        bean.prepareForm();

        assertEquals("", bean.getEditor().getSelectedProjectId());
        assertTrue(bean.isSuperAdmin());
    }

    @Test
    void prepareForm_deniedWhenUserCannotCreate() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(false);

        bean.prepareForm();

        assertFalse(bean.isFormAvailable());
        assertEquals(0, bean.getLanguages().size());
    }

    @Test
    void create_returnsToListOnSuccess() {
        stubCreateAccessWithUsername();
        bean.getEditor().setTitle("Nouveau");
        bean.getEditor().setSelectedLanguage("fr");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class);
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             var primeFaces = PrimeFacesTestSupport.open()) {
            stubFacesLookups(faces);
            when(newThesaurusService.create(any(), eq("admin"))).thenReturn("th1");

            bean.create();
        }

        verify(editionBean).showList();
        verify(consultationShellBean).refreshHeaderCatalog();
        verify(newThesaurusService).create(any(), eq("admin"));
    }

    @Test
    void create_showsErrorWhenServiceFails() {
        stubCreateAccessWithUsername();
        bean.getEditor().setTitle("Nouveau");
        bean.getEditor().setSelectedLanguage("fr");
        doThrow(new InvalidToolboxDataException("La langue est obligatoire"))
                .when(newThesaurusService).create(any(), eq("admin"));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.create();
        }

        verify(newThesaurusService).create(any(), eq("admin"));
    }

    @Test
    void create_skipsWhenAccessDenied() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(false);

        bean.create();

        verify(newThesaurusService, org.mockito.Mockito.never()).create(any(), any());
    }

    @Test
    void cancel_resetsEditorAndReturnsToList() {
        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            stubEditionBeanLookup(faces);
            bean.getEditor().setTitle("Test");

            bean.cancel();
        }

        assertEquals("", bean.getEditor().getTitle());
        verify(editionBean).showList();
    }

    private void stubPrepareFormAccess() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
    }

    private void stubCreateAccessWithUsername() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUsername()).thenReturn("admin");
    }

    private void stubEditionBeanLookup(MockedStatic<FacesContext> faces) {
        faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        when(facesContext.getApplication()).thenReturn(application);
        when(application.evaluateExpressionGet(eq(facesContext), eq("#{v2EditionBean}"), eq(EditionBean.class)))
                .thenReturn(editionBean);
    }

    private void stubFacesLookups(MockedStatic<FacesContext> faces) {
        stubEditionBeanLookup(faces);
        when(application.evaluateExpressionGet(
                eq(facesContext),
                eq("#{v2ConsultationShellBean}"),
                eq(ConsultationShellBean.class)
        )).thenReturn(consultationShellBean);
    }
}
