package fr.cnrs.opentheso.v2.toolbox.fixtures;

import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;

import java.time.LocalDateTime;

public final class ToolboxTestFixtures {

    private ToolboxTestFixtures() {
    }

    public static EditionThesaurusSummary sampleThesaurus() {
        return new EditionThesaurusSummary(
                "TH1",
                "Thésaurus test",
                false,
                LocalDateTime.of(2024, 1, 15, 10, 0)
        );
    }

    public static EditionThesaurusSummary privateThesaurus() {
        return new EditionThesaurusSummary(
                "TH2",
                "Thésaurus privé",
                true,
                LocalDateTime.of(2023, 5, 20, 8, 30)
        );
    }

    public static EditionStatistics sampleStatistics() {
        return new EditionStatistics(120, 5, 3);
    }

    public static EditionThesaurusRow sampleThesaurusRow() {
        return new EditionThesaurusRow("TH1", "Thésaurus test", false, LocalDateTime.of(2024, 1, 15, 10, 0));
    }

    public static LanguageOptionRow sampleLanguageRow() {
        return new LanguageOptionRow("fr", "fr", "Français", "French");
    }

    public static ProjectSummaryRow sampleProjectRow() {
        return new ProjectSummaryRow(7, "Projet A");
    }
}
