package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.utils.DateUtils;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import jakarta.faces.context.FacesContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusMaintenancePersistence {

    private final ThesaurusRepository thesaurusRepository;
    private final ConceptRepository conceptRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final TermRepository termRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    @Transactional
    public int correctDisplayTopTerm(String thesaurusId) {
        int modified = thesaurusRepository.resetTopConceptsWithRelations(thesaurusId);
        log.info("Top concepts corrigés: {}", modified);
        return modified;
    }

    @Transactional
    public void reorganizeHierarchy(String thesaurusId) {
        if (StringUtils.isEmpty(thesaurusId)) {
            return;
        }
        if (!cleanThesaurus(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des espaces et des null");
            return;
        }
        if (!reorganizingTopTermInThesaurus(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la correction des TT");
            return;
        }
        if (!reorganizingThesaurus(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la correction des NT BT");
            return;
        }
        if (!removeTopTermForConceptWithBT(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des BT pour les topTermes");
            return;
        }
        if (!removeSameRelations(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations en boucle");
        }
    }

    @Transactional
    public void reorganizeConceptsAndCollections(String thesaurusId) {
        if (!deleteConceptsWithEmptyRelation(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations vides");
        }
        if (!deleteConceptsHavingRelationShipWithDeletedGroup(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations interdites");
        }
        if (!deleteConceptsHavingRelationShipWithDeletedConcept(thesaurusId)) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression des relations interdites");
        }
    }

    @Transactional
    public void switchRolesFromTermToConcept(String thesaurusId, String workLanguage) {
        for (String conceptId : loadAllConceptIds(thesaurusId)) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null) {
                continue;
            }
            var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(thesaurusId, conceptId).orElse(null);
            if (preferredTerm == null) {
                continue;
            }
            var term = termRepository.findByIdTermAndIdThesaurusAndLang(
                    preferredTerm.getIdTerm(), thesaurusId, workLanguage).orElse(null);
            if (concept.getCreator() != null && concept.getCreator() > 0 && term != null
                    && term.getCreator() != null && term.getCreator() > 0) {
                concept.setContributor(term.getCreator());
            }
            if (concept.getContributor() != null && concept.getContributor() > 0 && term != null
                    && term.getContributor() != null && term.getContributor() > 0) {
                concept.setContributor(term.getContributor());
            }
            conceptRepository.save(concept);
        }
    }

    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        int count = 0;
        Preferences preference = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (preference == null || StringUtils.isEmpty(preference.getNaanArkLocal())) {
            MessageUtils.showErrorMessage("Pas de paramètres !! ");
            return count;
        }
        for (String conceptId : loadAllConceptIds(thesaurusId)) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null) {
                continue;
            }
            if (overwrite || StringUtils.isEmpty(concept.getIdArk())) {
                conceptRepository.setIdArk(naan + "/" + prefix + conceptId, new Date(), conceptId, thesaurusId);
                count++;
            }
        }
        return count;
    }

    public int generateLocalArk(String thesaurusId, boolean overwrite) {
        int count = 0;
        Preferences preference = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (preference == null) {
            MessageUtils.showErrorMessage("Pas de paramètres !! ");
            return count;
        }
        for (String conceptId : loadAllConceptIds(thesaurusId)) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null) {
                continue;
            }
            if (overwrite || StringUtils.isEmpty(concept.getIdArk())) {
                var idArk = ToolsHelper.getNewId(preference.getSizeIdArkLocal(), preference.isUppercaseForArk(), true);
                var urlArk = preference.getNaanArkLocal() + "/" + preference.getPrefixArkLocal() + idArk;
                conceptRepository.setIdArk(urlArk, new Date(), conceptId, thesaurusId);
                count++;
            }
        }
        return count;
    }

    public void generateSitemap(String thesaurusId) {
        if (StringUtils.isEmpty(thesaurusId)) {
            return;
        }
        var fileName = thesaurusId + ".xml";
        var conceptIds = loadAllConceptIds(thesaurusId);
        try {
            File file = new File(new URI(Objects.requireNonNull(this.getClass().getResource("/")).toString()) + fileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            var writeFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            writeFile.write(getHeader());
            writeFile.write(getDatas(conceptIds, thesaurusId));
            writeFile.write("</urlset>");
            writeFile.close();
            MessageUtils.showInformationMessage("L'export du siteMap a réussi, nom du fichier " + fileName);
        } catch (Exception e) {
            MessageUtils.showErrorMessage("L'export du siteMap a échoué");
        }
    }

    private boolean cleanThesaurus(String thesaurusId) {
        termRepository.deleteByIdTermAndIdThesaurus("", thesaurusId);
        conceptGroupLabelRepository.deleteByIdThesaurusAndIdGroup(thesaurusId, "");
        conceptGroupRepository.deleteByIdThesaurusAndIdGroup(thesaurusId, "");
        conceptGroupRepository.cleanIdTypeCode();
        conceptRepository.cleanConcept();
        return true;
    }

    private boolean reorganizingTopTermInThesaurus(String thesaurusId) {
        var listIds = loadTopTermIdsForRepair(thesaurusId);
        for (String idConcept : listIds) {
            conceptRepository.setTopConceptTag(true, idConcept, thesaurusId);
        }
        return true;
    }

    private boolean removeTopTermForConceptWithBT(String thesaurusId) {
        var tabIdTT = conceptRepository.findAllByIdThesaurusAndTopConceptAndStatusNotLike(thesaurusId, true, "CA");
        for (Concept concept : tabIdTT) {
            if (hasRelationBt(concept.getIdConcept(), thesaurusId)) {
                conceptRepository.setTopConceptTag(false, concept.getIdConcept(), thesaurusId);
            }
        }
        return true;
    }

    private boolean removeSameRelations(String idTheso) {
        return removeSameRelations("BT", idTheso)
                && removeSameRelations("NT", idTheso)
                && removeSameRelations("RT", idTheso);
    }

    private boolean removeSameRelations(String role, String thesaurusId) {
        var tabRelations = hierarchicalRelationshipRepository.getListLoopRelations(thesaurusId, role);
        if (!tabRelations.isEmpty()) {
            for (HierarchicalRelationship relation : tabRelations) {
                hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                        thesaurusId, relation.getIdConcept1(), relation.getIdConcept2(), role);
            }
        }
        return true;
    }

    @Transactional
    public boolean reorganizingThesaurus(String thesaurusId) {
        for (String idConcept : loadAllConceptIds(thesaurusId)) {
            var idBT = loadBtIds(idConcept, thesaurusId);
            var idConcept1WhereIsNT = loadNtParentIds(idConcept, thesaurusId);
            if (idBT.isEmpty() && idConcept1WhereIsNT.isEmpty()) {
                if (!isTopConcept(idConcept, thesaurusId)) {
                    conceptRepository.setTopConceptTag(true, idConcept, thesaurusId);
                }
            } else {
                if (!(new HashSet<>(idBT).containsAll(idConcept1WhereIsNT))) {
                    ArrayList<String> btMiss = new ArrayList<>(idConcept1WhereIsNT);
                    btMiss.removeAll(idBT);
                    for (String miss : btMiss) {
                        saveRelation(idConcept, thesaurusId, "BT", miss);
                    }
                }
                if (!(new HashSet<>(idConcept1WhereIsNT).containsAll(idBT))) {
                    ArrayList<String> ntMiss = new ArrayList<>(idBT);
                    ntMiss.removeAll(idConcept1WhereIsNT);
                    for (String miss : ntMiss) {
                        saveRelation(miss, thesaurusId, "NT", idConcept);
                    }
                }
            }
        }
        return true;
    }

    private boolean deleteConceptsWithEmptyRelation(String thesaurusId) {
        conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdGroup(thesaurusId, "");
        return true;
    }

    private boolean deleteConceptsHavingRelationShipWithDeletedGroup(String thesaurusId) {
        try {
            var orphanLinks = conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts(thesaurusId);
            for (Object[] row : orphanLinks) {
                String idGroup = (String) row[0];
                removeAllConceptsFromGroup(idGroup, thesaurusId);
            }
            return true;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: {}", thesaurusId, e);
            return false;
        }
    }

    private boolean deleteConceptsHavingRelationShipWithDeletedConcept(String thesaurusId) {
        try {
            var orphanLinks = conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts(thesaurusId);
            for (Object[] row : orphanLinks) {
                String idGroup = (String) row[0];
                String idConcept = (String) row[1];
                conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(idGroup, idConcept, thesaurusId);
            }
            return true;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: {}", thesaurusId, e);
            return false;
        }
    }

    private void removeAllConceptsFromGroup(String idGroup, String thesaurusId) {
        conceptGroupConceptRepository.deleteAllByIdGroupAndIdThesaurus(idGroup, thesaurusId);
    }

    private List<String> loadAllConceptIds(String thesaurusId) {
        return conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA").stream()
                .map(Concept::getIdConcept)
                .toList();
    }

    private List<String> loadTopTermIdsForRepair(String thesaurusId) {
        try {
            List<String> topConcepts = hierarchicalRelationshipRepository.findTopConceptsWithNTOnly(thesaurusId);
            List<String> isolatedConcepts = hierarchicalRelationshipRepository.findIsolatedConcepts(thesaurusId);
            Set<String> merged = new LinkedHashSet<>();
            merged.addAll(topConcepts);
            merged.addAll(isolatedConcepts);
            return new ArrayList<>(merged);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des TopTerms pour réparation du thésaurus '{}'", thesaurusId, e);
            return new ArrayList<>();
        }
    }

    private boolean hasRelationBt(String conceptId, String thesaurusId) {
        return CollectionUtils.isNotEmpty(
                hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(
                        thesaurusId, conceptId, "BT"));
    }

    private List<String> loadBtIds(String conceptId, String thesaurusId) {
        return hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(thesaurusId, conceptId, "BT")
                .stream()
                .map(HierarchicalRelationship::getIdConcept2)
                .toList();
    }

    private List<String> loadNtParentIds(String conceptId, String thesaurusId) {
        return hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike(thesaurusId, conceptId, "NT")
                .stream()
                .map(HierarchicalRelationship::getIdConcept1)
                .toList();
    }

    private boolean isTopConcept(String conceptId, String thesaurusId) {
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .map(concept -> Boolean.TRUE.equals(concept.getTopConcept()))
                .orElse(false);
    }

    private void saveRelation(String concept1, String thesaurusId, String role, String concept2) {
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(concept1)
                .idConcept2(concept2)
                .idThesaurus(thesaurusId)
                .role(role)
                .build());
    }

    private String getDatas(List<String> conceptIds, String thesaurusId) {
        var date = new DateUtils().getDate();
        var stringBuilder = new StringBuilder();
        for (String conceptId : conceptIds) {
            stringBuilder.append(getLine(getUri(conceptId, thesaurusId), date));
        }
        return stringBuilder.toString();
    }

    private String getUri(String idConcept, String idTheso) {
        return getPath() + "/?idc=" + idConcept + "&amp;idt=" + idTheso;
    }

    private String getHeader() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """;
    }

    private String getLine(String url, String date) {
        return "  <url>\n"
                + "    <loc>" + url + "</loc>\n"
                + "    <lastmod>" + date + "</lastmod>\n"
                + "  </url>\n";
    }

    private String getPath() {
        if (FacesContext.getCurrentInstance() == null) {
            return null;
        }
        String path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }
}
