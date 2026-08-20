package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportOptionsResponse;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportRequest;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportStatus;
import fr.cnrs.opentheso.v2.concept.export.service.SelectionExportJobStore;
import fr.cnrs.opentheso.v2.concept.export.service.SelectionExportService;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping({"/v2/api/selection-export", "/v2-preview/api/selection-export"})
public class SelectionExportController {

    private final SelectionExportService selectionExportService;
    private final SelectionExportJobStore selectionExportJobStore;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ToolboxExportPersistence toolboxExportPersistence;
    private final ThreadPoolTaskExecutor selectionExportExecutor;

    public SelectionExportController(
            SelectionExportService selectionExportService,
            SelectionExportJobStore selectionExportJobStore,
            ToolboxThesaurusPersistence toolboxThesaurusPersistence,
            ToolboxPreferencePersistence toolboxPreferencePersistence,
            ToolboxExportPersistence toolboxExportPersistence,
            @Qualifier("selectionExportExecutor") ThreadPoolTaskExecutor selectionExportExecutor
    ) {
        this.selectionExportService = selectionExportService;
        this.selectionExportJobStore = selectionExportJobStore;
        this.toolboxThesaurusPersistence = toolboxThesaurusPersistence;
        this.toolboxPreferencePersistence = toolboxPreferencePersistence;
        this.toolboxExportPersistence = toolboxExportPersistence;
        this.selectionExportExecutor = selectionExportExecutor;
    }

    @GetMapping(value = "/options", produces = MediaType.APPLICATION_JSON_VALUE)
    public SelectionExportOptionsResponse options(@RequestParam("thesaurusId") String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return new SelectionExportOptionsResponse("", "", List.of(), List.of());
        }
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        List<SelectionExportOptionsResponse.LangItem> languages = toolboxThesaurusPersistence
                .loadUsedLanguages(thesaurusId, workLang)
                .stream()
                .map(lang -> new SelectionExportOptionsResponse.LangItem(
                        lang.getCode(),
                        StringUtils.defaultIfBlank(lang.getValue(), lang.getCode())
                ))
                .toList();
        List<SelectionExportOptionsResponse.GroupItem> groups = toolboxExportPersistence
                .loadConceptGroups(thesaurusId)
                .stream()
                .map(SelectionExportController::toGroupItem)
                .toList();
        return new SelectionExportOptionsResponse(workLang, "", languages, groups);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SelectionExportStatus start(@RequestBody SelectionExportRequest request) {
        SelectionExportJob job = selectionExportJobStore.current();
        synchronized (job) {
            if ("running".equals(job.getStatus())) {
                return job.toStatus();
            }
            if (request == null || StringUtils.isBlank(request.thesaurusId())) {
                job.reset();
                job.fail("Thésaurus manquant");
                return job.toStatus();
            }
            if (!request.wholeThesaurus() && (request.conceptIds() == null || request.conceptIds().isEmpty())) {
                job.reset();
                job.fail("Aucun concept à exporter");
                return job.toStatus();
            }
            job.reset();
            job.start(1, "Démarrage de l'export…");
            try {
                Future<?> worker = selectionExportExecutor.submit(() -> {
                    try {
                        selectionExportService.export(request, job);
                    } finally {
                        job.clearWorker();
                    }
                });
                job.attachWorker(worker);
                if (job.isCancelRequested()) {
                    worker.cancel(true);
                    job.cancel();
                }
            } catch (TaskRejectedException ex) {
                job.fail("Serveur occupé. Réessayez dans un instant.");
            }
            return job.toStatus();
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public SelectionExportStatus status() {
        return selectionExportJobStore.current().toStatus();
    }

    @PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public SelectionExportStatus cancel() {
        SelectionExportJob job = selectionExportJobStore.current();
        job.requestCancel();
        if (!"running".equals(job.getStatus())) {
            job.cancel();
        }
        return job.toStatus();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> file() {
        SelectionExportJob job = selectionExportJobStore.current();
        if (!"done".equals(job.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        Path path = job.getFile();
        Resource body;
        long length;
        try {
            if (path != null && Files.isRegularFile(path)) {
                body = new FileSystemResource(path);
                length = Files.size(path);
            } else {
                byte[] content = job.getContent();
                if (content == null) {
                    return ResponseEntity.notFound().build();
                }
                body = new ByteArrayResource(content);
                length = content.length;
            }
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
        String filename = StringUtils.defaultIfBlank(job.getFilename(), "export.rdf");
        String contentType = StringUtils.defaultIfBlank(job.getContentType(), "application/octet-stream");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(length)
                .body(body);
    }

    private static SelectionExportOptionsResponse.GroupItem toGroupItem(NodeGroup group) {
        String id = group.getConceptGroup() == null ? "" : group.getConceptGroup().getIdGroup();
        return new SelectionExportOptionsResponse.GroupItem(
                id,
                StringUtils.defaultIfBlank(group.getLexicalValue(), id)
        );
    }
}
