package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportRequest;
import fr.cnrs.opentheso.v2.concept.export.service.SelectionExportJobStore;
import fr.cnrs.opentheso.v2.concept.export.service.SelectionExportService;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectionExportControllerTest {

    @Mock
    private SelectionExportService selectionExportService;
    @Mock
    private SelectionExportJobStore selectionExportJobStore;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ToolboxExportPersistence toolboxExportPersistence;

    private SelectionExportController controller;
    private SelectionExportJob job;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("selection-export-test-");
        executor.initialize();
        controller = new SelectionExportController(
                selectionExportService,
                selectionExportJobStore,
                toolboxThesaurusPersistence,
                toolboxPreferencePersistence,
                toolboxExportPersistence,
                executor
        );
        job = new SelectionExportJob();
        when(selectionExportJobStore.current()).thenReturn(job);
    }

    @AfterEach
    void tearDown() {
        job.reset();
        executor.shutdown();
    }

    @Test
    void start_rejectsMissingThesaurus() {
        var status = controller.start(SelectionExportRequest.of(" ", List.of("C1"), "rdf", false, false));

        assertEquals("error", status.status());
        assertEquals("Thésaurus manquant", status.error());
        verify(selectionExportService, never()).export(any(), any());
    }

    @Test
    void start_rejectsEmptySelection() {
        var status = controller.start(SelectionExportRequest.of("TH1", List.of(), "rdf", false, false));

        assertEquals("error", status.status());
        assertEquals("Aucun concept à exporter", status.error());
        verify(selectionExportService, never()).export(any(), any());
    }

    @Test
    void start_doesNotRestartARunningJob() {
        job.start(1, "Déjà en cours");

        var status = controller.start(SelectionExportRequest.of("TH1", List.of("C1"), "rdf", false, false));

        assertEquals("running", status.status());
        verify(selectionExportService, never()).export(any(), any());
    }

    @Test
    void start_runsOneExportPerSession() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(selectionExportService).export(any(), any());

        var status = controller.start(SelectionExportRequest.of("TH1", List.of("C1"), "rdf", true, false));

        assertEquals("running", status.status());
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        verify(selectionExportService).export(any(), any());
    }

    @Test
    void cancel_requestsStopOnRunningExport() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch hold = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            hold.await(3, TimeUnit.SECONDS);
            return null;
        }).when(selectionExportService).export(any(), any());

        controller.start(SelectionExportRequest.of("TH1", List.of("C1"), "rdf", false, false));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        var status = controller.cancel();
        hold.countDown();

        assertTrue(job.isCancelRequested());
        assertEquals("running", status.status());
    }

    @Test
    void file_returnsAttachmentWhenDone() throws Exception {
        job.complete(new byte[]{9, 8}, "TH1_selection.rdf", "application/rdf+xml", "Fichier prêt");

        var response = controller.file();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/rdf+xml", response.getHeaders().getContentType().toString());
        assertNotNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("TH1_selection.rdf"));
        assertEquals(2L, response.getHeaders().getContentLength());
        assertEquals(2L, response.getBody().contentLength());
    }

    @Test
    void file_isNotFoundWhileIdle() {
        assertEquals(HttpStatus.NOT_FOUND, controller.file().getStatusCode());
    }

    @Test
    void cancel_marksIdleJobAsCancelled() {
        var status = controller.cancel();

        assertEquals("cancelled", status.status());
    }

    @Test
    void options_returnsLanguagesAndEmptyGroups() {
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(NodeLangTheso.builder().code("fr").value("français").build()));
        when(toolboxExportPersistence.loadConceptGroups("TH1")).thenReturn(List.of());

        var options = controller.options("TH1");

        assertEquals("fr", options.workLanguage());
        assertEquals(1, options.languages().size());
        assertEquals("fr", options.languages().get(0).code());
        assertTrue(options.groups().isEmpty());
    }

    @Test
    void options_returnsEmptyWhenThesaurusIsBlank() {
        var options = controller.options(" ");

        assertEquals("", options.workLanguage());
        assertTrue(options.languages().isEmpty());
        assertNotNull(options.groups());
        assertTrue(options.groups().isEmpty());
    }
}
