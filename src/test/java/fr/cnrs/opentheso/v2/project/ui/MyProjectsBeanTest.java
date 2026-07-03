package fr.cnrs.opentheso.v2.project.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyProjectsBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean localeBean;
    @Mock
    private ProjectAdminService projectAdminService;
    @Mock
    private ProjectManagementService projectManagementService;
    @Mock
    private ProjectMemberService projectMemberService;

    private MyProjectsBean myProjectsBean;

    @BeforeEach
    void setUp() {
        lenient().when(localeBean.getMsg(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(localeBean.getIdLangue()).thenReturn("fr");
        myProjectsBean = new MyProjectsBean(
                userSession,
                localeBean,
                projectAdminService,
                projectManagementService,
                projectMemberService
        );
    }

    @Test
    void load_clearsStateWhenUserNotConnected() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        myProjectsBean.load();

        assertTrue(myProjectsBean.getProjects().isEmpty());
        assertNull(myProjectsBean.getSelectedProjectId());
        assertNull(myProjectsBean.getDashboard());
    }

    @Test
    void load_listsProjectsWithoutDashboardWhenNoneSelected() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(projectAdminService.listAccessibleProjects(5)).thenReturn(
                List.of(new ProjectSummary(1, "Projet A"))
        );

        myProjectsBean.load();

        assertEquals(1, myProjectsBean.getProjects().size());
        assertNull(myProjectsBean.getDashboard());
    }

    @Test
    void onProjectChanged_loadsDashboardForSelectedProject() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myProjectsBean.setSelectedProjectId(3);
        ProjectDashboard dashboard = buildDashboard();
        when(projectAdminService.loadDashboard(5, 3, "fr")).thenReturn(dashboard);

        myProjectsBean.onProjectChanged();

        assertEquals(dashboard, myProjectsBean.getDashboard());
    }

    @Test
    void onProjectChanged_clearsDashboardWhenNoProjectSelected() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myProjectsBean.setSelectedProjectId(null);

        myProjectsBean.onProjectChanged();

        assertNull(myProjectsBean.getDashboard());
        verify(projectAdminService, never()).loadDashboard(anyInt(), anyInt(), anyString());
    }

    @Test
    void createProject_refreshesListAndShowsSuccessMessage() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(projectAdminService.listAccessibleProjects(5)).thenReturn(List.of(new ProjectSummary(9, "Nouveau")));
        myProjectsBean.setNewProjectName("Nouveau");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            myProjectsBean.createProject();
            messages.verify(() -> MessageUtils.showInformationMessage("project.createdSuccess"));
        }

        assertNull(myProjectsBean.getNewProjectName());
        verify(projectManagementService).createProject(5, true, "Nouveau");
    }

    @Test
    void createProject_showsErrorOnInvalidData() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(true);
        myProjectsBean.setNewProjectName("");
        when(projectManagementService.createProject(5, true, ""))
                .thenThrow(new InvalidProjectDataException("Le nom du projet est obligatoire."));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myProjectsBean.createProject();
            messages.verify(() -> MessageUtils.showErrorMessage("Le nom du projet est obligatoire."));
        }
    }

    @Test
    void renameProject_updatesDashboardAndShowsSuccessMessage() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(false);
        myProjectsBean.setSelectedProjectId(3);
        myProjectsBean.setDashboard(buildDashboard());
        myProjectsBean.setRenameProjectLabel("Renommé");
        when(projectAdminService.listAccessibleProjects(5)).thenReturn(List.of(new ProjectSummary(3, "Renommé")));
        when(projectAdminService.loadDashboard(5, 3, "fr")).thenReturn(
                new ProjectDashboard(3, "Renommé", true, 2, List.of(), List.of(), List.of(), List.of())
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            myProjectsBean.renameProject();
            messages.verify(() -> MessageUtils.showInformationMessage("project.renamedSuccess"));
        }

        verify(projectManagementService).renameProject(5, false, 3, "Renommé");
    }

    @Test
    void renameProject_showsErrorOnAccessDenied() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(false);
        myProjectsBean.setSelectedProjectId(3);
        myProjectsBean.setRenameProjectLabel("Renommé");
        when(projectManagementService.renameProject(5, false, 3, "Renommé"))
                .thenThrow(new ProjectAccessDeniedException());

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myProjectsBean.renameProject();
            messages.verify(() -> MessageUtils.showErrorMessage("Accès refusé à ce projet."));
        }
    }

    @Test
    void prepareCreateDialog_resetsProjectName() {
        myProjectsBean.setNewProjectName("Ancien");

        myProjectsBean.prepareCreateDialog();

        assertNull(myProjectsBean.getNewProjectName());
    }

    @Test
    void prepareRenameDialog_prefillsCurrentProjectLabel() {
        myProjectsBean.setDashboard(buildDashboard());

        myProjectsBean.prepareRenameDialog();

        assertEquals("Projet X", myProjectsBean.getRenameProjectLabel());
    }

    @Test
    void jsfHelpers_exposeSessionAndDashboardFlags() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(true);
        myProjectsBean.setDashboard(buildDashboard());

        assertTrue(myProjectsBean.isSuperAdmin());
        assertTrue(myProjectsBean.isProjectAdminScreen());
        assertEquals(5, myProjectsBean.getCurrentUserId());
    }

    @Test
    void jsfHelpers_returnDefaultsWhenUserDisconnected() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        assertEquals(-1, myProjectsBean.getCurrentUserId());
        assertFalse(myProjectsBean.isProjectAdminScreen());
    }

    @Test
    void load_showsErrorWhenDashboardAccessDenied() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myProjectsBean.setSelectedProjectId(3);
        when(projectAdminService.listAccessibleProjects(5)).thenReturn(Collections.emptyList());
        when(projectAdminService.loadDashboard(5, 3, "fr"))
                .thenThrow(new ProjectAccessDeniedException());

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myProjectsBean.load();
            messages.verify(() -> MessageUtils.showErrorMessage("Accès refusé à ce projet."));
        }

        assertNull(myProjectsBean.getDashboard());
    }

    private static ProjectDashboard buildDashboard() {
        return new ProjectDashboard(3, "Projet X", true, 2, List.of(), List.of(), List.of(), List.of());
    }

    private static MockedStatic<PrimeFaces> mockPrimeFaces() {
        MockedStatic<PrimeFaces> primeFaces = mockStatic(PrimeFaces.class);
        PrimeFaces instance = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        primeFaces.when(PrimeFaces::current).thenReturn(instance);
        when(instance.ajax()).thenReturn(ajax);
        return primeFaces;
    }
}
