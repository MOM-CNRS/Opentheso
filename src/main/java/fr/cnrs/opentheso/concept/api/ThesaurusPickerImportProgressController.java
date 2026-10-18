package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.concept.service.ThesaurusPickerImportProgressTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v2/thesaurus-import")
@RequiredArgsConstructor
public class ThesaurusPickerImportProgressController {

    private final ThesaurusPickerImportProgressTracker progressTracker;

    @GetMapping(value = "/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThesaurusPickerImportProgressTracker.ProgressSnapshot> progress(HttpSession session) {
        ThesaurusPickerImportProgressTracker.ProgressSnapshot snapshot =
                session == null
                        ? new ThesaurusPickerImportProgressTracker.ProgressSnapshot(
                                false, false, 0, 0, "idle", "", 0, 0, -1L, 0d)
                        : progressTracker.snapshot(session.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(snapshot);
    }

    @PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CancelResponse> cancel(HttpSession session) {
        boolean accepted = session != null && progressTracker.requestCancel(session.getId());
        return ResponseEntity.ok(new CancelResponse(accepted));
    }

    @GetMapping(value = "/report")
    public ResponseEntity<byte[]> report(HttpSession session) {
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return progressTracker.report(session.getId())
                .map(text -> {
                    byte[] body = text.getBytes(StandardCharsets.UTF_8);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport-import.txt\"")
                            .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                            .body(body);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record CancelResponse(boolean accepted) {
    }
}
