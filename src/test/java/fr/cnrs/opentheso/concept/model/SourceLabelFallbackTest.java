package fr.cnrs.opentheso.concept.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceLabelFallbackTest {

    @Test
    void missing_recognizesTheBareIdAndTheParenthesesForm() {
        assertTrue(SourceLabelFallback.missing("300", "300"));
        assertTrue(SourceLabelFallback.missing("(300)", "300"));
        assertTrue(SourceLabelFallback.missing("  ", "300"));
        assertFalse(SourceLabelFallback.missing("amphore", "300"));
    }

    @Test
    void apply_replacesOnlyUntranslatedConceptsWithTheSourceLabel() {
        var untranslated = new ConceptTreeNodeData("300", "300", "", "file", false);
        var translated = new ConceptTreeNodeData("301", "amphore", "", "concept", true);
        var facet = new ConceptTreeNodeData("F1", "F1", "", "facet", false);

        List<ConceptTreeNodeData> applied = SourceLabelFallback.apply(
                List.of(untranslated, translated, facet),
                Map.of("300", "amphora", "F1", "ignored"));

        assertEquals("amphora", applied.get(0).label());
        assertTrue(applied.get(0).sourceLabel());
        assertEquals("amphore", applied.get(1).label());
        assertFalse(applied.get(1).sourceLabel());
        assertEquals("F1", applied.get(2).label());
        assertFalse(applied.get(2).sourceLabel());
    }
}
