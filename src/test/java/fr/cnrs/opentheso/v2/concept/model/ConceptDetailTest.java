package fr.cnrs.opentheso.v2.concept.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptDetailTest {

    @Test
    void customRelations_splitOutgoingAndReciprocal() {
        var detail = new ConceptDetail(
                new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "place", "", "", "", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new ConceptCustomRelationItem("C2", "Target", "rel", "Rel", false),
                        new ConceptCustomRelationItem("C3", "Reciprocal", "rel2", "Rel2", true)
                ),
                List.of(),
                null,
                "",
                ""
        );

        assertEquals(1, detail.outgoingCustomRelations().size());
        assertEquals("C2", detail.outgoingCustomRelations().get(0).targetConceptId());
        assertEquals(1, detail.reciprocalCustomRelations().size());
        assertEquals("C3", detail.reciprocalCustomRelations().get(0).targetConceptId());
        assertTrue(detail.hasOutgoingCustomRelations());
        assertTrue(detail.hasReciprocalCustomRelations());
        assertFalse(detail.outgoingCustomRelations().isEmpty());
    }
}
