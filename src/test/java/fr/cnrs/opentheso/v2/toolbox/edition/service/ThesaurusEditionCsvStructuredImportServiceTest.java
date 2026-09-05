package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredImportPersistence;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvStructuredImportServiceTest {

    @Mock
    private ThesaurusEditionCsvStructuredImportPersistence persistence;
    @Mock
    private NewThesaurusService newThesaurusService;

    private ThesaurusEditionCsvStructuredImportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvStructuredImportService(persistence, newThesaurusService);
    }

    @Test
    void loadCsvFile_wrapsParseResult() {
        var root = new NodeTree("C1", "Chat");
        when(persistence.parse(new byte[]{1}, ';'))
                .thenReturn(new ThesaurusEditionStructuredParseResult(root, 4, null));

        var loaded = service.loadCsvFile(new byte[]{1}, ';');

        assertTrue(loaded.success());
        assertEquals(4, loaded.totalConcepts());
        assertEquals(root, loaded.root());
    }

    @Test
    void importNewThesaurus_picksSingleProjectWhenNotSuperAdmin() {
        var root = new NodeTree("C1", "Chat");
        when(newThesaurusService.loadFormOptions(7, false))
                .thenReturn(new NewThesaurusFormOptions(List.of(), List.of(new ProjectOption(12, "Projet")), false));
        when(persistence.importNewThesaurus("Animaux", "fr", 12, 7, "admin", root))
                .thenReturn(new ThesaurusEditionStructuredImportResult("TH9", 3, "ok"));

        var outcome = service.importNewThesaurus("Animaux", " ", 7, "admin", false, null, root);

        assertEquals("TH9", outcome.thesaurusId());
        assertEquals(3, outcome.importedConcepts());
        verify(persistence).importNewThesaurus("Animaux", "fr", 12, 7, "admin", root);
    }

    @Test
    void importNewThesaurus_throwsWhenPersistenceFails() {
        var root = new NodeTree("C1", "Chat");
        when(persistence.importNewThesaurus(eq("Animaux"), eq("en"), isNull(), eq(1), eq("u"), eq(root)))
                .thenReturn(ThesaurusEditionStructuredImportResult.error("échec"));

        assertThrowsExactly(IllegalStateException.class, () ->
                service.importNewThesaurus("Animaux", "en", 1, "u", true, null, root));
    }
}
