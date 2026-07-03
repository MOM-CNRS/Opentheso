package fr.cnrs.opentheso.v2.concept.session;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptSelectionContextTest {

    private ConceptSelectionContext context;

    @BeforeEach
    void setUp() {
        context = new ConceptSelectionContext();
    }

    @Test
    void hasSelection_falseWhenEmpty() {
        assertFalse(context.hasSelection());
        assertNull(context.getConceptId());
        assertNull(context.getThesaurusId());
        assertNull(context.getSummary());
    }

    @Test
    void update_storesSummaryFromDetail() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "D", "", "concept", "", "", "", "");
        var detail = new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        context.update("TH1", detail);

        assertTrue(context.hasSelection());
        assertEquals("C1", context.getConceptId());
        assertEquals("TH1", context.getThesaurusId());
        assertEquals("Label", context.getSummary().preferredLabel());
    }

    @Test
    void update_clearsWhenDetailIsNull() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "D", "", "concept", "", "", "", "");
        context.update("TH1", "C1", summary);

        context.update("TH1", (ConceptDetail) null);

        assertFalse(context.hasSelection());
    }

    @Test
    void clear_resetsAllFields() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "D", "", "concept", "", "", "", "");
        context.update("TH1", "C1", summary);

        context.clear();

        assertFalse(context.hasSelection());
        assertNull(context.getConceptId());
    }
}
