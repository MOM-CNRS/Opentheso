package fr.cnrs.opentheso.shared.web;

import fr.cnrs.opentheso.shared.service.PickerFooterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class PickerFooterImageController {

    private final PickerFooterService pickerFooterService;

    @GetMapping("/api/instance/picker-footer/mark")
    public ResponseEntity<byte[]> mark() {
        return pickerFooterService.image()
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                        .body(image.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
