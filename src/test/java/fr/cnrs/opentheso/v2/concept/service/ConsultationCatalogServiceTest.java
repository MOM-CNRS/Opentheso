package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.shared.repository.AdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConsultationCatalogQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationCatalogServiceTest {

    @Mock
    private ConsultationCatalogQueryRepository consultationCatalogQueryRepository;
    @Mock
    private ProjectAdminService projectAdminService;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private AdminQueryRepository adminQueryRepository;

    private ConsultationCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ConsultationCatalogService(
                consultationCatalogQueryRepository,
                projectAdminService,
                projectAdminQueryRepository,
                adminQueryRepository
        );
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void listThesauri_whenProjectSelected_returnsAllThesauriOfProject() {
        when(projectAdminQueryRepository.findThesauriOfProject(7, "fr")).thenReturn(List.of(
                new ProjectThesaurusRow("TH1", "Alpha", false),
                new ProjectThesaurusRow("TH2", "Beta", true)
        ));

        List<ConsultationThesaurusOption> result = service.listThesauri(3, true, 7, "fr");

        assertEquals(2, result.size());
        assertEquals("TH1", result.get(0).id());
        assertEquals("Alpha", result.get(0).title());
        verify(projectAdminQueryRepository).findThesauriOfProject(7, "fr");
        verifyNoInteractions(adminQueryRepository);
        verifyNoInteractions(consultationCatalogQueryRepository);
    }

    @Test
    void listThesauri_whenAllProjectsAndSuperAdmin_returnsAllThesauri() {
        when(adminQueryRepository.findAllThesauri("fr")).thenReturn(List.of());

        service.listThesauri(1, true, -1, "fr");

        verify(adminQueryRepository).findAllThesauri("fr");
        verifyNoInteractions(projectAdminQueryRepository);
    }
}
