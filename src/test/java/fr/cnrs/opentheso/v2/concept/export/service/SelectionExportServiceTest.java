package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportRequest;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionZipExportService;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectionExportServiceTest {

    @Mock
    private ConceptSkosExportService conceptSkosExportService;
    @Mock
    private ConceptQueryRepository conceptQueryRepository;
    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;
    @Mock
    private ThesaurusPdfWriter thesaurusPdfWriter;
    @Mock
    private ThesaurusEditionCsvStructuredExportPersistence csvStructuredPersistence;
    @Mock
    private ThesaurusEditionZipExportService zipExportService;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private SelectionExportService service;
    private SelectionExportJob job;

    @BeforeEach
    void setUp() {
        service = new SelectionExportService(
                conceptSkosExportService,
                conceptQueryRepository,
                thesaurusSkosDocumentBuilder,
                thesaurusCsvWriter,
                thesaurusPdfWriter,
                csvStructuredPersistence,
                zipExportService,
                toolboxThesaurusPersistence,
                toolboxPreferencePersistence
        );
        job = new SelectionExportJob();
    }

    @AfterEach
    void tearDown() {
        job.reset();
    }

    @Test
    void export_normalizesJsonLdAliasBeforeSkosExport() throws Exception {
        when(conceptSkosExportService.buildDocument(eq("TH1"), any(), any(), eq(false)))
                .thenReturn(new SKOSXmlDocument());
        when(conceptSkosExportService.serialize(any(), eq("TH1"), any(), eq("jsonld")))
                .thenReturn(new ExportResult(new byte[]{1}, "ignored.json", "application/ld+json"));

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "JSON-LD", false, false), job);

        assertEquals("done", job.getStatus());
        verify(conceptSkosExportService).serialize(any(), eq("TH1"), any(), eq("jsonld"));
        assertEquals(datedName("th1_selection", ".json"), job.getFilename());
    }

    @Test
    void resolveConceptIds_keepsSelectionWhenDescendantsAreOff() {
        List<String> ids = service.resolveConceptIds("TH1", List.of("C1", " ", "C1"), false);

        assertEquals(List.of("C1"), ids);
        verify(conceptQueryRepository, never()).findDescendantConceptIds(any(), any());
    }

    @Test
    void resolveConceptIds_addsDescendantsFromRepository() {
        when(conceptQueryRepository.findDescendantConceptIds(eq("TH1"), any()))
                .thenReturn(List.of("C2", "C3"));

        List<String> ids = service.resolveConceptIds("TH1", List.of("C1"), true);

        assertEquals(List.of("C1", "C2", "C3"), ids);
    }

    @Test
    void export_failsWhenThesaurusIsMissing() {
        service.export(SelectionExportRequest.of(" ", List.of("C1"), "rdf", false, false), job);

        assertEquals("error", job.getStatus());
        assertEquals("Thésaurus manquant", job.getError());
    }

    @Test
    void export_cancelsWhenRequestedBeforeWork() throws Exception {
        job.requestCancel();

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "rdf", false, false), job);

        assertEquals("cancelled", job.getStatus());
        verify(conceptSkosExportService, never()).buildDocument(any(), any(), any(), anyBoolean());
    }

    @Test
    void export_writesSkosSelectionAndCompletesJob() throws Exception {
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenAnswer(invocation -> {
                    BiConsumer<Integer, Integer> progress = invocation.getArgument(2);
                    progress.accept(1, 1);
                    return new SKOSXmlDocument();
                });
        when(conceptSkosExportService.serialize(any(), eq("TH1"), any(), eq("rdf")))
                .thenReturn(new ExportResult(new byte[]{1, 2}, "ignored.rdf", "application/rdf+xml"));

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "rdf", false, false), job);

        assertEquals("done", job.getStatus());
        assertEquals(100, job.getProgress());
        assertEquals(datedName("th1_selection", ".rdf"), job.getFilename());
        assertEquals("application/rdf+xml", job.getContentType());
        assertArrayEquals(new byte[]{1, 2}, job.getContent());
        assertEquals("Fichier RDF/XML prêt · 1 concept", job.getMessage());
        assertEquals(3, job.getPhaseIndex());
        assertTrue(job.toStatus().downloadable());
    }

    @Test
    void export_stripsHtmlWhenRequested() throws Exception {
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(true)))
                .thenReturn(new SKOSXmlDocument());
        when(conceptSkosExportService.serialize(any(), eq("TH1"), any(), eq("rdf")))
                .thenReturn(new ExportResult(new byte[]{3}, "ignored.rdf", "application/rdf+xml"));

        service.export(request("TH1", List.of("C1"), "rdf", false, false, true), job);

        verify(conceptSkosExportService).buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(true));
        assertEquals("done", job.getStatus());
    }

    @Test
    void export_writesCsvSelection() {
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(new SKOSXmlDocument());
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(NodeLangTheso.builder().code("fr").value("français").build()));
        when(thesaurusCsvWriter.writeCsv(any(), any(), eq(','))).thenReturn("id,label".getBytes());

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "csv", false, false), job);

        assertEquals("done", job.getStatus());
        assertEquals(datedName("th1_selection", ".csv"), job.getFilename());
        assertEquals("text/csv", job.getContentType());
        assertArrayEquals("id,label".getBytes(), job.getContent());
    }

    @Test
    void export_failsCsvWhenNoLanguageIsAvailable() {
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(new SKOSXmlDocument());
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr")).thenReturn(List.of());

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "csv", false, false), job);

        assertEquals("error", job.getStatus());
        assertEquals("Aucune langue disponible pour l'export CSV", job.getError());
    }

    @Test
    void export_writesWholeThesaurusCsv() throws Exception {
        when(thesaurusSkosDocumentBuilder.buildDocument(eq("TH1"), any())).thenReturn(new SKOSXmlDocument());
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(NodeLangTheso.builder().code("fr").value("français").build()));
        when(thesaurusCsvWriter.writeCsv(any(), any(), eq(','))).thenReturn("all".getBytes());

        service.export(SelectionExportRequest.of("TH1", List.of(), "csv", false, true), job);

        assertEquals("done", job.getStatus());
        assertEquals(datedName("th1_thesaurus", ".csv"), job.getFilename());
        verify(conceptSkosExportService, never()).buildDocument(any(), any(), any(), anyBoolean());
    }

    @Test
    void export_writesCsvIdForSelection() {
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(thesaurusCsvWriter.writeCsvById(eq("TH1"), eq("fr"), isNull(), eq(','), eq(List.of("C1"))))
                .thenReturn("id;pref".getBytes());

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "csv-id", false, false), job);

        assertEquals("done", job.getStatus());
        assertEquals("text/csv", job.getContentType());
        assertEquals(datedName("th1_selection", ".csv"), job.getFilename());
        verify(conceptSkosExportService, never()).buildDocument(any(), any(), any(), anyBoolean());
    }

    @Test
    void export_writesPdfForSelection() throws Exception {
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(new SKOSXmlDocument());
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(NodeLangTheso.builder().code("fr").value("français").build()));
        when(thesaurusPdfWriter.createPdfFile(any(), eq("fr"), eq(""), eq(ThesaurusPdfExportType.HIERARCHIQUE), eq(false)))
                .thenReturn(new byte[]{9, 9});

        service.export(SelectionExportRequest.of("TH1", List.of("C1"), "pdf", false, false), job);

        assertEquals("done", job.getStatus());
        assertEquals("application/pdf", job.getContentType());
        assertEquals(datedName("th1_selection", ".pdf"), job.getFilename());
        assertArrayEquals(new byte[]{9, 9}, job.getContent());
    }

    @Test
    void export_filtersSelectionByCollection() throws Exception {
        when(conceptQueryRepository.findConceptIdsInGroups("TH1", List.of("G1"))).thenReturn(List.of("C1"));
        when(conceptSkosExportService.buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(new SKOSXmlDocument());
        when(conceptSkosExportService.serialize(any(), eq("TH1"), any(), eq("rdf")))
                .thenReturn(new ExportResult(new byte[]{1}, "ignored.rdf", "application/rdf+xml"));

        service.export(requestWithGroups("TH1", List.of("C1", "C2"), "rdf", List.of("G1"), false), job);

        assertEquals("done", job.getStatus());
        verify(conceptSkosExportService).buildDocument(eq("TH1"), eq(List.of("C1")), any(), eq(false));
    }

    @Test
    void export_rejectsFilterWithoutCollection() {
        service.export(requestWithGroups("TH1", List.of("C1"), "rdf", List.of(), true), job);

        assertEquals("error", job.getStatus());
        assertEquals("Choisissez au moins une collection", job.getError());
    }

    @Test
    void export_rejectsZipUnlessWholeThesaurus() {
        service.export(requestZip("TH1", List.of("C1"), false), job);

        assertEquals("error", job.getStatus());
        assertEquals("L’archive par collection s’applique au thésaurus entier", job.getError());
    }

    @Test
    void export_writesZipForWholeThesaurus() throws Exception {
        StreamedContent streamed = mock(StreamedContent.class);
        when(streamed.getStream()).thenReturn(() -> new ByteArrayInputStream(new byte[]{4, 5}));
        when(zipExportService.exportEachGroupAsSkosZip(eq("TH1"), any(), eq("rdf"), eq(false), anyList()))
                .thenReturn(streamed);

        service.export(requestZip("TH1", List.of(), true), job);

        assertEquals("done", job.getStatus());
        assertEquals("application/zip", job.getContentType());
        assertEquals(datedName("th1_thesaurus", ".zip"), job.getFilename());
        assertArrayEquals(new byte[]{4, 5}, job.getContent());
    }

    @Test
    void normalizeFormat_mapsLegacyAliases() {
        assertEquals("jsonld", SelectionExportService.normalizeFormat("JSON-LD"));
        assertEquals("csv-id", SelectionExportService.normalizeFormat("csv_id"));
        assertEquals("csv-structured", SelectionExportService.normalizeFormat("csv-struc"));
        assertEquals("csv-deprecated", SelectionExportService.normalizeFormat("deprecated"));
        assertEquals("pdf", SelectionExportService.normalizeFormat("PDF"));
        assertEquals("turtle", SelectionExportService.normalizeFormat("ttl"));
    }

    private static SelectionExportRequest request(
            String thesaurusId,
            List<String> ids,
            String format,
            boolean descendants,
            boolean whole,
            boolean clearHtml
    ) {
        return new SelectionExportRequest(
                thesaurusId, null, ids, format, descendants, whole,
                clearHtml, false, false, false, List.of(), List.of(), ",", "hierarchical", null, null
        );
    }

    private static SelectionExportRequest requestWithGroups(
            String thesaurusId,
            List<String> ids,
            String format,
            List<String> groups,
            boolean filter
    ) {
        return new SelectionExportRequest(
                thesaurusId, null, ids, format, false, false,
                false, false, filter, false, groups, List.of(), ",", "hierarchical", null, null
        );
    }

    private static SelectionExportRequest requestZip(String thesaurusId, List<String> ids, boolean whole) {
        return new SelectionExportRequest(
                thesaurusId, null, ids, "rdf", false, whole,
                false, false, false, true, List.of(), List.of(), ",", "hierarchical", null, null
        );
    }

    private static String datedName(String base, String extension) {
        return base + "_" + LocalDate.now() + extension;
    }
}
