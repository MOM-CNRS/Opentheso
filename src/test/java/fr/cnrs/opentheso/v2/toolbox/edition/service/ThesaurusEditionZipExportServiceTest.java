package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import java.util.List;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionZipExportServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;
    @Mock
    private ToolboxExportPersistence toolboxExportPersistence;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private ThesaurusEditionZipExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionZipExportService(
                thesaurusSkosDocumentBuilder,
                thesaurusCsvWriter,
                toolboxExportPersistence,
                toolboxThesaurusPersistence,
                toolboxPreferencePersistence
        );
    }

    @Test
    void exportEachGroupAsCsvZip_throwsWhenNoCollection() {
        when(toolboxExportPersistence.loadConceptGroups("TH1")).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () ->
                service.exportEachGroupAsCsvZip("TH1", "Pays", ',', List.of(), false));
    }

    @Test
    void exportEachGroupAsCsvZip_buildsZipAndDedupesFileNames() throws Exception {
        when(toolboxExportPersistence.loadConceptGroups("TH1")).thenReturn(List.of(
                group("G1", "Europe"),
                group("G2", "Europe")
        ));
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(NodeLangTheso.builder().code("fr").value("français").build()));
        when(thesaurusSkosDocumentBuilder.buildDocumentByGroup(eq("TH1"), any(), anyBoolean()))
                .thenReturn(new SKOSXmlDocument());
        when(thesaurusCsvWriter.writeCsv(any(), any(), anyChar())).thenReturn("id,label\n".getBytes());

        StreamedContent zip = service.exportEachGroupAsCsvZip(
                "TH1", "Pays", ',', List.of("fr"), false, List.of("G1", "G2"));

        assertEquals("application/zip", zip.getContentType());
        assertTrue(zip.getName().endsWith(".zip"));
        try (ZipInputStream zin = new ZipInputStream(zip.getStream().get())) {
            assertEquals("Pays_Europe.csv", zin.getNextEntry().getName());
            assertEquals("Pays_Europe_1.csv", zin.getNextEntry().getName());
        }
    }

    @Test
    void exportEachGroupAsCsvZip_filtersRestrictedGroups() {
        when(toolboxExportPersistence.loadConceptGroups("TH1")).thenReturn(List.of(
                group("G1", "Europe"),
                group("G2", "Asie")
        ));

        assertThrows(IllegalStateException.class, () ->
                service.exportEachGroupAsCsvZip("TH1", "Pays", ',', List.of(), false, List.of("missing")));
    }

    private static NodeGroup group(String id, String label) {
        return NodeGroup.builder()
                .conceptGroup(ConceptGroup.builder().idGroup(id).idThesaurus("TH1").build())
                .lexicalValue(label)
                .build();
    }
}
