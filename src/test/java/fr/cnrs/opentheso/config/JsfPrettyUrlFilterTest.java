package fr.cnrs.opentheso.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsfPrettyUrlFilterTest {

    @Test
    void prettyUrlsForwardToJsfViews() {
        assertEquals("/v2/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2"));
        assertEquals("/v2/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/thesauri"));
        assertEquals("/v2/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview"));
        assertEquals("/v2/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/"));
        assertEquals("/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/index"));
        assertEquals("/v2/admin/instance.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/admin/instance"));
        assertEquals("/v2/admin/projets.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/admin/projets"));
        assertEquals("/v2/admin/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/admin/thesauri"));
        assertEquals("/v2/admin/utilisateurs.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/admin/utilisateurs"));
        assertEquals("/v2/setting/preference.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/setting/preference"));
        assertEquals("/v2/setting/identifiants.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/setting/identifiants"));
        assertEquals("/v2/setting/corpus.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/setting/corpus"));
        assertEquals("/v2/setting/parametres.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/setting/parametres"));
        assertEquals("/v2/toolbox/atelier.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/atelier"));
        assertEquals("/v2/toolbox/maintenance.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/maintenance"));
        assertEquals("/v2/toolbox/statistiques.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/statistiques"));
        assertEquals("/v2/toolbox/synchronisation.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/synchronisation"));
        assertEquals("/v2/toolbox/portail.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/portail"));
        assertEquals("/v2/toolbox/actions-lot.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/toolbox/actions-lot"));
        assertEquals("/v2/candidat/candidats.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/candidat/candidats"));
        assertEquals("/v2/project/projets.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/project/projets"));
        assertEquals("/v2/user/compte.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/user/compte"));
        assertEquals("/v2/user/preference.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/user/preference"));
        assertEquals("/v2/proposition/propositions.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/proposition/propositions"));
        assertEquals("/v2/graph/graphe.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/graph/graphe"));
        assertEquals("/v2/thesaurus/consultation.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2/thesaurus/consultation"));
    }

    @Test
    void oldPreviewPagesForwardToOrganizedV2Paths() {
        assertEquals("/v2/setting/preference.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/preference.xhtml"));
        assertEquals("/v2/toolbox/statistiques.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/statistiques.xhtml"));
        assertEquals("/v2/toolbox/synchronisation.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/synchronisation.xhtml"));
        assertEquals("/v2/toolbox/portail.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/v2-preview/portail.xhtml"));
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
