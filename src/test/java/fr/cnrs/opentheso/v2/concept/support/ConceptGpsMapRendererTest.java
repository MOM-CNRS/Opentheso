package fr.cnrs.opentheso.v2.concept.support;

import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptGpsMapRendererTest {

    @Test
    void renderMapScript_buildsLeafletInitialization() {
        String script = ConceptGpsMapRenderer.renderMapScript(
                "v2ConceptMap",
                List.of(new ConceptGpsPoint(48.85, 2.35, 1))
        );

        assertTrue(script.contains("v2ConceptMap"));
        assertTrue(script.contains("L.map"));
        assertTrue(script.contains("circleMarker"));
    }

    @Test
    void renderMapScript_returnsEmptyWhenNoPoints() {
        assertTrue(ConceptGpsMapRenderer.renderMapScript("v2ConceptMap", List.of()).isEmpty());
    }

    @Test
    void renderMapScript_drawsPolylineForOpenPath() {
        String script = ConceptGpsMapRenderer.renderMapScript(
                "v2ConceptMap",
                List.of(
                        new ConceptGpsPoint(48.85, 2.35, 1),
                        new ConceptGpsPoint(48.86, 2.36, 2)
                )
        );

        assertTrue(script.contains("polyline"));
        assertFalse(script.contains("polygon"));
    }
}
