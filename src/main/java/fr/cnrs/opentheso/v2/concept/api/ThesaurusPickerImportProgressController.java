package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.service.ThesaurusPickerImportProgressTracker;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
