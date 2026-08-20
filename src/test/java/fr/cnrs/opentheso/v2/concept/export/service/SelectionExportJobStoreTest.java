package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SelectionExportJobStoreTest {

    private final SelectionExportJobStore store = new SelectionExportJobStore();

    @AfterEach
    void tearDown() {
        try {
            store.clear();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void current_keepsTheSameRunningJobAcrossCalls() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SelectionExportJob job = store.current();
        job.start(1, "Déjà en cours");

        SelectionExportJob again = store.current();

        assertSame(job, again);
        assertEquals("running", again.getStatus());
    }
}
