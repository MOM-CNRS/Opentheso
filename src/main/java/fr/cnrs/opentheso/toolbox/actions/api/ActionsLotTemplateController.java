package fr.cnrs.opentheso.toolbox.actions.api;

import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotAlignmentService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotAltLabelService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotArkService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotCollectionService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotImageService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotNotationService;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class ActionsLotTemplateController {

    private static final byte[] RELATIONS_TEMPLATE = """
            localId,localId cible
            26678,300
            """.getBytes(StandardCharsets.UTF_8);

    private final ActionsLotCollectionService collectionService;
    private final ActionsLotNotationService notationService;
    private final ActionsLotImageService imageService;
    private final ActionsLotArkService arkService;
    private final ActionsLotNoteService noteService;
    private final ActionsLotAltLabelService altLabelService;
    private final ActionsLotAlignmentService alignmentService;

    @GetMapping("/api/toolbox/{area:collections|notations|images|identifiants|notes|synonymes|alignements|relations}/template")
    public ResponseEntity<byte[]> template(
            @PathVariable String area,
            @RequestParam(name = "kind", defaultValue = "import") String kind
    ) {
        TemplateFile file = templateFile(area, kind);
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

    private TemplateFile templateFile(String area, String kind) {
        String normalized = kind == null ? "import" : kind.trim().toLowerCase();
        return switch (area) {
            case "collections" -> new TemplateFile("modele-collections.csv", collectionService.templateBytes());
            case "notations" -> new TemplateFile("modele-notations.csv", notationService.templateBytes());
            case "images" -> new TemplateFile("modele-images.csv", imageService.templateBytes());
            case "identifiants" -> new TemplateFile("modele-identifiants-ark.csv", arkService.templateBytes());
            case "notes" -> new TemplateFile("modele-notes.csv", noteService.templateBytes());
            case "synonymes" -> synonymes(normalized);
            case "alignements" -> alignements(normalized);
            case "relations" -> new TemplateFile("modele-relations.csv", RELATIONS_TEMPLATE);
            default -> null;
        };
    }

    private TemplateFile synonymes(String kind) {
        return switch (kind) {
            case "import" -> new TemplateFile(
                    "modele-formes-alternatives.csv", altLabelService.templateBytes());
            case "delete" -> new TemplateFile(
                    "modele-formes-alternatives-suppression.csv", altLabelService.templateBytes());
            default -> null;
        };
    }

    private TemplateFile alignements(String kind) {
        return switch (kind) {
            case "import" -> new TemplateFile(
                    "modele-alignements.csv", alignmentService.importTemplateBytes());
            case "delete" -> new TemplateFile(
                    "modele-alignements-suppression.csv", alignmentService.deleteTemplateBytes());
            default -> null;
        };
    }

    private record TemplateFile(String filename, byte[] bytes) {
    }
}
