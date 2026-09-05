package fr.cnrs.opentheso.v2.admin.mapper;

import fr.cnrs.opentheso.v2.shared.repository.projection.AdminThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminUserRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminMapperTest {

    @Test
    void toUserMembership_copiesRow() {
        var mapped = AdminMapper.toUserMembership(
                new AdminUserRow(3, "ada", 9, "Projet", 2, "manager"));

        assertEquals(3, mapped.userId());
        assertEquals("ada", mapped.username());
        assertEquals(9, mapped.projectId());
        assertEquals("manager", mapped.roleName());
    }

    @Test
    void toThesaurus_copiesRow() {
        var created = LocalDateTime.of(2024, Month.JUNE, 15, 12, 0);
        var mapped = AdminMapper.toThesaurus(
                new AdminThesaurusRow("TH1", "Animaux", 4, "Projet", true, created));

        assertEquals("TH1", mapped.id());
        assertEquals("Animaux", mapped.title());
        assertTrue(mapped.privateThesaurus());
        assertEquals(created, mapped.createdAt());
    }

    @Test
    void toProjectSummary_andThesaurusOption() {
        var project = AdminMapper.toProjectSummary(new ProjectSummaryRow(8, "P8"));
        var option = AdminMapper.toThesaurusOption(new ProjectThesaurusRow("TH2", "Flore", false));

        assertEquals(8, project.id());
        assertEquals("P8", project.name());
        assertEquals("TH2", option.id());
        assertEquals("Flore", option.title());
    }
}
