package fr.cnrs.opentheso.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsfPrettyUrlFilterTest {

    @Test
    void prettyUrlsForwardToJsfViews() {
        assertEquals("/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/v2"));
        assertEquals("/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/thesauri"));
        assertEquals("/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/"));
        assertEquals("/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/"));
        assertEquals("/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/index"));
        assertEquals("/index.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/index.xhtml"));
        assertEquals("/user/compte.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/profile"));
        assertEquals("/candidat/candidats.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/candidat"));
        assertEquals("/toolbox/atelier.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/edition"));
        assertEquals("/admin/instance.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/admin/instance"));
        assertEquals("/admin/projets.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/admin/projets"));
        assertEquals("/admin/thesauri.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/admin/thesauri"));
        assertEquals("/admin/utilisateurs.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/admin/utilisateurs"));
        assertEquals("/setting/preference.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/setting/preference"));
        assertEquals("/setting/identifiants.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/setting/identifiants"));
        assertEquals("/setting/corpus.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/setting/corpus"));
        assertEquals("/setting/parametres.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/setting/parametres"));
        assertEquals("/toolbox/atelier.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/atelier"));
        assertEquals("/toolbox/maintenance.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/maintenance"));
        assertEquals("/toolbox/statistiques.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/statistiques"));
        assertEquals("/toolbox/synchronisation.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/synchronisation"));
        assertEquals("/toolbox/portail.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/portail"));
        assertEquals("/toolbox/actions-lot.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/toolbox/actions-lot"));
        assertEquals("/candidat/candidats.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/candidat/candidats"));
        assertEquals("/project/projets.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/project/projets"));
        assertEquals("/user/compte.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/user/compte"));
        assertEquals("/user/preference.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/user/preference"));
        assertEquals("/proposition/propositions.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/proposition/propositions"));
        assertEquals("/graph/graphe.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/graph/graphe"));
        assertEquals("/thesaurus/consultation.xhtml", JsfPrettyUrlFilter.resolveForwardTarget("/thesaurus/consultation"));
    }

    @Test
    void oldPreviewPagesForwardToOrganizedV2Paths() {
        assertEquals("/setting/preference.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/preference.xhtml"));
        assertEquals("/toolbox/statistiques.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/statistiques.xhtml"));
        assertEquals("/toolbox/synchronisation.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/synchronisation.xhtml"));
        assertEquals("/toolbox/portail.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/portail.xhtml"));
        assertEquals("/candidat/candidats.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/candidats.xhtml"));
        assertEquals("/admin/projets.xhtml",
                JsfPrettyUrlFilter.resolveForwardTarget("/admin-projets.xhtml"));
    }

    @Test
    void unknownOrOrganizedV2PathsAreLeftUntouched() {
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget(null));
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget("/setting/preference.xhtml"));
        assertNull(JsfPrettyUrlFilter.resolveForwardTarget("/includes/layout.xhtml"));
    }
}
