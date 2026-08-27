package fr.cnrs.opentheso.v2.toolbox.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaintenanceLastRunStoreTest {

    @Test
    void markAndGet_areScopedByThesaurusAndAction() {
        MaintenanceLastRunStore store = new MaintenanceLastRunStore();
        store.mark("TH1", MaintenanceLastRunStore.TOP_TERM);

        assertNotNull(store.get("TH1", MaintenanceLastRunStore.TOP_TERM));
        assertNull(store.get("TH1", MaintenanceLastRunStore.SITEMAP));
        assertNull(store.get("TH2", MaintenanceLastRunStore.TOP_TERM));
    }

    @Test
    void pendingSitemap_isConsumedOnce() {
        MaintenanceLastRunStore store = new MaintenanceLastRunStore();
        store.putPendingSitemap("th1.xml", "<urlset/>".getBytes());

        MaintenanceLastRunStore.PendingSitemap pending = store.consumePendingSitemap();
        assertEquals("th1.xml", pending.fileName());
        assertEquals("<urlset/>", new String(pending.xml()));
        assertNull(store.consumePendingSitemap());
    }
}
