package fr.cnrs.opentheso.v2.toolbox.edition.io.csv;

import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvAlignmentRow;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvExportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusCsvWriterExportTest {

    @Mock
    private ThesaurusEditionCsvExportPersistence csvExportQuerySupport;

    private ThesaurusCsvWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ThesaurusCsvWriter(csvExportQuerySupport);
    }

    @Test
    void writeCsvById_serializesConceptRow() {
        when(csvExportQuerySupport.listConceptIds("TH1", null)).thenReturn(List.of("C1"));
        when(csvExportQuerySupport.loadConceptForCsvById("C1", "TH1", "fr"))
                .thenReturn(Optional.of(sampleConceptRow()));

        byte[] csv = writer.writeCsvById("TH1", "fr", null, ',');

        assertNotNull(csv);
        String content = new String(csv, StandardCharsets.UTF_8);
        assertTrue(content.contains("conceptId"));
        assertTrue(content.contains("C1"));
        assertTrue(content.contains("Chat"));
    }

    @Test
    void writeCsvByDeprecated_serializesDeprecatedRow() {
        when(csvExportQuerySupport.listDeprecatedConcepts("TH1", "fr"))
                .thenReturn(List.of(deprecatedSample()));

        byte[] csv = writer.writeCsvByDeprecated("TH1", "fr", ';');

        assertNotNull(csv);
        String content = new String(csv, StandardCharsets.UTF_8);
        assertTrue(content.contains("deprecatedId"));
        assertTrue(content.contains("D1"));
        assertTrue(content.contains("Ancien"));
    }

    private ThesaurusCsvByIdRow sampleConceptRow() {
        return new ThesaurusCsvByIdRow(
                "C1",
                "ark:/123",
                "hdl:456",
                "Chat",
                List.of("Minou"),
                List.of("Animal domestique"),
                List.of(new ThesaurusCsvAlignmentRow("exactMatch", "http://example.org/cat")));
    }

    private NodeDeprecated deprecatedSample() {
        var deprecated = new NodeDeprecated();
        deprecated.setDeprecatedId("D1");
        deprecated.setDeprecatedLabel("Ancien");
        deprecated.setReplacedById("C1");
        deprecated.setReplacedByLabel("Nouveau");
        deprecated.setUserName("admin");
        return deprecated;
    }
}
