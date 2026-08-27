package fr.cnrs.opentheso.v2.toolbox.api;

import fr.cnrs.opentheso.v2.toolbox.ui.MaintenanceLastRunStore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class MaintenanceSitemapApiController {

    private final MaintenanceLastRunStore lastRunStore;

    public MaintenanceSitemapApiController(MaintenanceLastRunStore lastRunStore) {
        this.lastRunStore = lastRunStore;
    }

    @GetMapping("/maintenance/sitemap.xml")
    public ResponseEntity<byte[]> downloadSitemap() {
        MaintenanceLastRunStore.PendingSitemap pending = lastRunStore.consumePendingSitemap();
        if (pending == null || pending.xml() == null || pending.xml().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String fileName = StringUtils.defaultIfBlank(pending.fileName(), "sitemap.xml");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(pending.xml());
    }
}
