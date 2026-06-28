package fr.cnrs.opentheso.v2.toolbox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkshopServiceTest {

    private WorkshopService service;

    @BeforeEach
    void setUp() {
        service = new WorkshopService();
    }

    @Test
    void resolveThesaurusId_prefersContextId() {
        assertEquals("TH_CTX", service.resolveThesaurusId("TH_CTX", "TH_SEL"));
    }

    @Test
    void resolveThesaurusId_fallsBackToSelectedId() {
        assertEquals("TH_SEL", service.resolveThesaurusId("", "TH_SEL"));
        assertEquals("TH_SEL", service.resolveThesaurusId(null, "TH_SEL"));
    }

    @Test
    void resolveThesaurusTitle_prefersContextTitle() {
        assertEquals(
                "Titre contexte",
                service.resolveThesaurusTitle("Titre contexte", "TH_CTX", "Titre sélection", "TH_SEL")
        );
    }

    @Test
    void resolveThesaurusTitle_fallsBackToSelectedName() {
        assertEquals(
                "Titre sélection",
                service.resolveThesaurusTitle("", "TH_CTX", "Titre sélection", "TH_SEL")
        );
    }

    @Test
    void resolveThesaurusTitle_fallsBackToThesaurusId() {
        assertEquals("TH_CTX", service.resolveThesaurusTitle("", "TH_CTX", "", "TH_SEL"));
        assertEquals("TH_SEL", service.resolveThesaurusTitle(null, null, "", "TH_SEL"));
    }
}
