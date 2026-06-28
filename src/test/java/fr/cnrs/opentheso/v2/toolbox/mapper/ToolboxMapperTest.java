package fr.cnrs.opentheso.v2.toolbox.mapper;

import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusDetailsRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolboxMapperTest {

    @Test
    void toThesaurusSummary_mapsRow() {
        var row = new EditionThesaurusRow("TH1", "Titre", true, LocalDateTime.of(2024, 6, 1, 12, 0));

        var summary = ToolboxMapper.toThesaurusSummary(row);

        assertEquals("TH1", summary.id());
        assertEquals("Titre", summary.title());
        assertEquals(true, summary.privateThesaurus());
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0), summary.createdAt());
        assertEquals("TH1", summary.getId());
        assertEquals("Titre", summary.getTitle());
        assertEquals(true, summary.isPrivateThesaurus());
    }

    @Test
    void toLanguageOption_mapsRow() {
        var row = new LanguageOptionRow("fr", "fr", "Français", "French");

        var language = ToolboxMapper.toLanguageOption(row);

        assertEquals("fr", language.code());
        assertEquals("fr", language.countryCode());
        assertEquals("Français", language.frenchName());
        assertEquals("French", language.englishName());
        assertEquals("fr", language.getCode());
    }

    @Test
    void toProjectOption_mapsRow() {
        var project = ToolboxMapper.toProjectOption(new ProjectSummaryRow(12, "Projet test"));

        assertEquals(12, project.id());
        assertEquals("Projet test", project.name());
        assertEquals("12", project.getIdAsString());
    }

    @Test
    void toThesaurusDetails_mapsRow() {
        var details = ToolboxMapper.toThesaurusDetails(
                new EditionThesaurusDetailsRow("TH1", "Titre", "ark/1", true, "fr")
        );

        assertEquals("TH1", details.id());
        assertEquals("ark/1", details.arkId());
        assertTrue(details.privateThesaurus());
    }

    @Test
    void toEditionLanguage_mapsRow() {
        var language = ToolboxMapper.toEditionLanguage(
                new ThesaurusLanguageRow(1L, "fr", "fr", "Titre FR", "Français")
        );

        assertEquals("fr", language.code());
        assertEquals("Titre FR", language.label());
        assertEquals("Français", language.displayLabel());
    }

    @Test
    void toLanguageFlag_mapsRow() {
        var flag = ToolboxMapper.toLanguageFlag(new LanguageOptionRow("fr", "FR", "Français", "French"));

        assertEquals("fr", flag.getIso6391());
        assertEquals("FR", flag.getCountryCode());
    }
}
