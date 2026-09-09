package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.fixtures.ToolboxTestFixtures;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusRequest;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewThesaurusServiceTest {

    @Mock
    private EditionQueryRepository editionQueryRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private ProjectMembershipRepository projectMembershipRepository;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ThesaurusDcTermRepository thesaurusDcTermRepository;

    @InjectMocks
    private NewThesaurusService service;

    @Test
    void loadFormOptions_returnsLanguagesAndProjectsForSuperAdmin() {
        when(editionQueryRepository.findAllLanguages())
                .thenReturn(List.of(ToolboxTestFixtures.sampleLanguageRow()));
        when(projectAdminQueryRepository.findAllProjects())
                .thenReturn(List.of(ToolboxTestFixtures.sampleProjectRow()));

        var options = service.loadFormOptions(1, true);

        assertEquals(1, options.languages().size());
        assertEquals(1, options.projects().size());
        assertEquals(true, options.superAdmin());
    }

    @Test
    void loadFormOptions_usesAdminProjectsForRegularUser() {
        when(editionQueryRepository.findAllLanguages()).thenReturn(List.of());
        when(editionQueryRepository.findAdminProjectsForUser(5))
                .thenReturn(List.of(ToolboxTestFixtures.sampleProjectRow()));

        var options = service.loadFormOptions(5, false);

        assertEquals(1, options.projects().size());
        assertEquals(false, options.superAdmin());
        verify(projectAdminQueryRepository, never()).findAllProjects();
    }

    @Test
    void create_validatesTitle() {
        var request = new NewThesaurusRequest(" ", "", "fr", null, false, "");
        assertThrows(InvalidToolboxDataException.class, () -> service.create(request, "admin"));
    }

    @Test
    void create_validatesLanguage() {
        var request = new NewThesaurusRequest("Titre", "", " ", null, false, "");
        assertThrows(InvalidToolboxDataException.class, () -> service.create(request, "admin"));
    }

    @Test
    void create_failsWhenThesaurusCannotBeCreated() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn(null);
        var request = new NewThesaurusRequest("Test", "", "fr", null, false, "");

        assertThrows(InvalidToolboxDataException.class, () -> service.create(request, "admin"));
    }

    @Test
    void create_linksProjectWhenProvided() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th99");

        String createdId = service.create(new NewThesaurusRequest("Test", "","fr", 5, false, "CNRS"), "admin");

        assertEquals("th99", createdId);
        ArgumentCaptor<fr.cnrs.opentheso.entites.UserGroupThesaurus> captor =
                ArgumentCaptor.forClass(fr.cnrs.opentheso.entites.UserGroupThesaurus.class);
        verify(toolboxThesaurusPersistence).linkToProject(captor.capture());
        assertEquals(5, captor.getValue().getIdGroup());
        verify(toolboxPreferencePersistence).initPreferences("th99", "fr");
        ArgumentCaptor<fr.cnrs.opentheso.models.thesaurus.Thesaurus> thesaurusCaptor =
                ArgumentCaptor.forClass(fr.cnrs.opentheso.models.thesaurus.Thesaurus.class);
        verify(toolboxThesaurusPersistence).addTranslation(thesaurusCaptor.capture());
        assertEquals("CNRS", thesaurusCaptor.getValue().getPublisher());
        verify(toolboxThesaurusPersistence).setVisibility("th99", false);
        verify(projectMembershipRepository, never()).assignLimitedRole(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void create_assignsLimitedAdminWhenCreatorHasNoProjectRole() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th101");
        when(projectAdminQueryRepository.findCallerRoleOnProject(9, 5)).thenReturn(Optional.empty());
        when(projectMembershipRepository.hasLimitedRoleOnThesaurus(9, "th101")).thenReturn(false);

        service.create(new NewThesaurusRequest("Test", "", "fr", 5, false, ""), "admin", 9);

        verify(projectMembershipRepository).assignLimitedRole(9, 2, 5, "th101");
    }

    @Test
    void create_skipsLimitedRoleWhenCreatorAlreadyHasProjectRole() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th102");
        when(projectAdminQueryRepository.findCallerRoleOnProject(9, 5)).thenReturn(Optional.of(2));

        service.create(new NewThesaurusRequest("Test", "", "fr", 5, false, ""), "admin", 9);

        verify(projectMembershipRepository, never()).assignLimitedRole(anyInt(), anyInt(), anyInt(), any());
        verify(projectMembershipRepository, never()).hasLimitedRoleOnThesaurus(anyInt(), any());
    }

    @Test
    void create_skipsProjectLinkWhenProjectIsNull() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th100");

        service.create(new NewThesaurusRequest("Test", "","en", null, true, ""), "creator", 3);

        verify(toolboxThesaurusPersistence, never()).linkToProject(any());
        verify(toolboxPreferencePersistence).initPreferences("th100", "en");
        verify(toolboxThesaurusPersistence).setVisibility("th100", true);
        verify(projectMembershipRepository, never()).assignLimitedRole(anyInt(), anyInt(), anyInt(), any());
    }
}
