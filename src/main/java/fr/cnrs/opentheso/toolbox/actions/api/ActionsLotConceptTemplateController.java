package fr.cnrs.opentheso.toolbox.actions.api;

import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/toolbox/concepts")
@RequiredArgsConstructor
public class ActionsLotConceptTemplateController {

    private final ActionsLotConceptService conceptService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(name = "kind", defaultValue = "add") String kind) {
        TemplateFile file = templateFile(kind);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(file.bytes());
    }

    private TemplateFile templateFile(String kind) {
        return switch (kind == null ? "" : kind.trim().toLowerCase()) {
            case "add" -> new TemplateFile("modele-concepts-ajout.csv", conceptService.addTemplateBytes());
            case "merge", "replace" -> new TemplateFile(
                    "modele-concepts-remplacer.csv", conceptService.mergeTemplateBytes());
            case "deprecate" -> new TemplateFile(
                    "modele-concepts-obsolete.csv", conceptService.deprecateTemplateBytes());
            case "compare" -> new TemplateFile(
                    "modele-concepts-comparer.csv", conceptService.compareTemplateBytes());
            default -> null;
        };
    }

    private record TemplateFile(String filename, byte[] bytes) {
    }
}
