package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.repository.UserRoleQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserThesaurusRoleRow;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleOverviewServiceTest {

    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;

    @InjectMocks
    private UserRoleOverviewService userRoleOverviewService;

    @Test
    void loadProjectRoles_returnsEmptyListForSuperAdmin() {
        assertTrue(userRoleOverviewService.loadProjectRoles(1, true).isEmpty());
    }

    @Test
    void loadProjectRoles_groupsBulkQueryResultsByProject() {
        when(userRoleQueryRepository.findAllThesaurusRolesForUser(10))
                .thenReturn(List.of(new UserThesaurusRoleRow(3, "Projet A", "THESO1", "Mon thésaurus", 3)));

        List<ProjectRoleOverview> roles = userRoleOverviewService.loadProjectRoles(10, false);

        assertEquals(1, roles.size());
        assertEquals("Projet A", roles.get(0).projectName());
        assertEquals("manager", roles.get(0).thesaurusRoles().get(0).roleName());
        assertEquals("Mon thésaurus", roles.get(0).thesaurusRoles().get(0).thesaurusName());
    }

    @Test
    void loadProjectRoles_groupsMultipleProjectsFromSingleQuery() {
        when(userRoleQueryRepository.findAllThesaurusRolesForUser(10))
                .thenReturn(List.of(
                        new UserThesaurusRoleRow(3, "Projet A", "THESO1", "Mon thésaurus", 2),
                        new UserThesaurusRoleRow(4, "Projet B", "THESO2", "Autre thésaurus", 2)
                ));

        List<ProjectRoleOverview> roles = userRoleOverviewService.loadProjectRoles(10, false);

        assertEquals(2, roles.size());
        assertEquals("Projet A", roles.get(0).projectName());
        assertEquals("Projet B", roles.get(1).projectName());
        assertEquals(1, roles.get(0).thesaurusRoles().size());
        assertEquals(1, roles.get(1).thesaurusRoles().size());
    }

    @Test
    void loadProjectRoles_usesThesaurusIdAsFallbackName() {
        when(userRoleQueryRepository.findAllThesaurusRolesForUser(10))
                .thenReturn(List.of(new UserThesaurusRoleRow(3, "Projet A", "THESO1", "THESO1", 4)));

        List<ProjectRoleOverview> roles = userRoleOverviewService.loadProjectRoles(10, false);

        assertEquals("THESO1", roles.get(0).thesaurusRoles().get(0).thesaurusName());
        assertEquals("contributor", roles.get(0).thesaurusRoles().get(0).roleName());
    }
}
