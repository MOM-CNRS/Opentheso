package fr.cnrs.opentheso.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsfPrettyUrlFilterTest {

    @Test
    void prettyUrlsForwardToJsfViews() {
        assertEquals("/v2/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2"));
        assertEquals("/v2/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview"));
        assertEquals("/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/"));
    }

    @Test
    void oldPreviewPagesForwardToOrganizedV2Paths() {
        assertEquals("/v2/setting/preference.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/preference.xhtml"));
        assertEquals("/v2/toolbox/statistiques.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/statistiques.xhtml"));
        assertEquals("/v2/candidat/candidats.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/candidats.xhtml"));
        assertEquals("/v2/admin/projets.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/admin-projets.xhtml"));
    }

    @Test
    void unknownOrOrganizedV2PathsAreLeftUntouched() {
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget(null));
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget("/v2/setting/preference.xhtml"));
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/includes/layout.xhtml"));
    }
}
