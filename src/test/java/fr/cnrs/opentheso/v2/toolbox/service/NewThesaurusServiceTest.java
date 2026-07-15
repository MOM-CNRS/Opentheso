package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        assertThrows(
                InvalidToolboxDataException.class,
                () -> service.create(new NewThesaurusRequest(" ", "fr", null), "admin")
        );
    }

    @Test
    void create_validatesLanguage() {
        assertThrows(
                InvalidToolboxDataException.class,
                () -> service.create(new NewThesaurusRequest("Titre", " ", null), "admin")
        );
    }

    @Test
    void create_failsWhenThesaurusCannotBeCreated() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn(null);

        assertThrows(
                InvalidToolboxDataException.class,
                () -> service.create(new NewThesaurusRequest("Test", "fr", null), "admin")
        );
    }

    @Test
    void create_linksProjectWhenProvided() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th99");

        String createdId = service.create(new NewThesaurusRequest("Test", "fr", 5), "admin");

        assertEquals("th99", createdId);
        ArgumentCaptor<fr.cnrs.opentheso.entites.UserGroupThesaurus> captor =
                ArgumentCaptor.forClass(fr.cnrs.opentheso.entites.UserGroupThesaurus.class);
        verify(toolboxThesaurusPersistence).linkToProject(captor.capture());
        assertEquals(5, captor.getValue().getIdGroup());
        verify(toolboxPreferencePersistence).initPreferences("th99", "fr");
        verify(toolboxThesaurusPersistence).addTranslation(any());
    }

    @Test
    void create_skipsProjectLinkWhenProjectIsNull() {
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th100");

        service.create(new NewThesaurusRequest("Test", "en", null), "creator");

        verify(toolboxThesaurusPersistence, never()).linkToProject(any());
        verify(toolboxPreferencePersistence).initPreferences("th100", "en");
    }
}
