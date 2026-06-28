package fr.cnrs.opentheso.v2.toolbox.mapper;

import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusDetailsRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusLanguage;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageFlag;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;

public final class ToolboxMapper {

    private ToolboxMapper() {
    }

    public static EditionThesaurusSummary toThesaurusSummary(EditionThesaurusRow row) {
        return new EditionThesaurusSummary(
                row.id(),
                row.title(),
                row.privateThesaurus(),
                row.createdAt()
        );
    }

    public static LanguageOption toLanguageOption(LanguageOptionRow row) {
        return new LanguageOption(
                row.code(),
                row.countryCode(),
                row.frenchName(),
                row.englishName()
        );
    }

    public static ProjectOption toProjectOption(ProjectSummaryRow row) {
        return new ProjectOption(row.id(), row.name());
    }

    public static EditionThesaurusDetails toThesaurusDetails(EditionThesaurusDetailsRow row) {
        return new EditionThesaurusDetails(
                row.id(),
                row.title(),
                row.arkId(),
                row.privateThesaurus(),
                row.sourceLang()
        );
    }

    public static EditionThesaurusLanguage toEditionLanguage(ThesaurusLanguageRow row) {
        return new EditionThesaurusLanguage(
                row.code(),
                row.codeFlag(),
                row.labelTheso(),
                row.displayLabel()
        );
    }

    public static LanguageFlag toLanguageFlag(LanguageOptionRow row) {
        return new LanguageFlag(row.code(), row.countryCode());
    }
}
