package fr.cnrs.opentheso.concept.service;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusPickerImportIssuesTest {

    @Test
    void fromCsv_collectsMissingIdentifierAndMissingLabel() {
        var concept = new ThesaurusCsvConceptObject();
        concept.setIdConcept("c1");
        concept.setPrefLabels(new ArrayList<>());

        var result = ThesaurusPickerImportIssues.fromCsv(
                "\nconcept sans Id : CSVRecord [comment='null', recordNumber=4, values=[a]]",
                List.of(concept)
        );

        assertEquals(2, result.total());
        assertEquals(4, result.issues().get(0).line());
        assertEquals(ThesaurusPickerImportIssues.MISSING_ID, result.issues().get(0).code());
        assertEquals(ThesaurusPickerImportIssues.WEAK_LABEL, result.issues().get(1).code());
        assertEquals("c1", result.issues().get(1).ref());
    }

    @Test
    void fromCsv_ignoresConceptsThatHaveAPreferredLabel() {
        var concept = new ThesaurusCsvConceptObject();
        concept.setIdConcept("c1");
        var label = new ThesaurusCsvConceptLabel();
        label.setLabel("Chat");
        label.setLang("fr");
        concept.setPrefLabels(new ArrayList<>(List.of(label)));

        var result = ThesaurusPickerImportIssues.fromCsv(null, List.of(concept));

        assertEquals(0, result.total());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void fromSkos_flagsConceptsWithoutPreferredLabel() throws Exception {
        var document = new SKOSXmlDocument();
        var labelled = new SKOSResource();
        labelled.setIdentifier("ok");
        labelled.getLabelsList().add(new SKOSLabel("Chat", "fr", SKOSProperty.PREF_LABEL));
        var blank = new SKOSResource();
        blank.setUri("https://example.org/blank");
        document.setConceptList(new ArrayList<>(List.of(labelled, blank)));

        var result = ThesaurusPickerImportIssues.fromSkos(document);

        assertEquals(1, result.total());
        assertEquals(2, result.issues().get(0).line());
        assertEquals("https://example.org/blank", result.issues().get(0).ref());
    }

    @Test
    void fromStructured_flagsBlankNodesAndTheirChildren() {
        var root = new NodeTree();
        var blank = new NodeTree(null, "  ");
        var child = new NodeTree(null, "Enfant");
        blank.setChildrens(new ArrayList<>(List.of(child)));
        var kept = new NodeTree(null, "Racine");
        root.setChildrens(new ArrayList<>(List.of(blank, kept)));

        var result = ThesaurusPickerImportIssues.fromStructured(root);

        assertEquals(2, result.total());
        assertEquals(ThesaurusPickerImportIssues.MISSING_LABEL, result.issues().get(0).code());
        assertEquals(ThesaurusPickerImportIssues.DROPPED_PARENT, result.issues().get(1).code());
        assertEquals("Enfant", result.issues().get(1).ref());
    }

    @Test
    void cancelFlagStopsTheNextBatch() throws Exception {
        var tracker = new ThesaurusPickerImportProgressTracker();
        tracker.start("session");
        tracker.requestCancel("session");

        ThesaurusPickerImportProgressContext.callWith(
                (done, total, phase) -> { },
                () -> tracker.isCancelled("session"),
                () -> {
                    org.junit.jupiter.api.Assertions.assertThrows(
                            ImportCancelledException.class,
                            ThesaurusPickerImportProgressContext::ensureActive
                    );
                    return null;
                }
        );
    }
}
