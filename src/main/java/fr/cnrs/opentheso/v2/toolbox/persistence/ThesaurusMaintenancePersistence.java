package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public boolean reorganizeHierarchy(String thesaurusId) {
        if (StringUtils.isEmpty(thesaurusId)) {
            return false;
        }
        cleanThesaurus(thesaurusId);
        reorganizingTopTermInThesaurus(thesaurusId);
        doReorganizingThesaurus(thesaurusId);
        removeTopTermForConceptWithBT(thesaurusId);
        removeSameRelations(thesaurusId);
        return true;
    }

    @Transactional
    public int reorganizeConceptsAndCollections(String thesaurusId) {
        int cleaned = 0;
        cleaned += deleteConceptsWithEmptyRelation(thesaurusId);
        cleaned += deleteConceptsHavingRelationShipWithDeletedGroup(thesaurusId);
        cleaned += deleteConceptsHavingRelationShipWithDeletedConcept(thesaurusId);
        return cleaned;
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
            if (term == null) {
                continue;
            }
            boolean changed = false;
            if (term.getCreator() != null && term.getCreator() > 0) {
                concept.setCreator(term.getCreator());
                changed = true;
            }
            if (term.getContributor() != null && term.getContributor() > 0) {
                concept.setContributor(term.getContributor());
                changed = true;
            }
            if (changed) {
                conceptRepository.save(concept);
            }
        }
    }

    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        int count = 0;
        if (StringUtils.isBlank(naan)) {
            MessageUtils.showErrorMessage("Le NAAN est obligatoire");
            return count;
        }
        Preferences preference = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (preference == null) {
            MessageUtils.showErrorMessage("Pas de paramètres !! ");
            return count;
        }
        String safePrefix = StringUtils.trimToEmpty(prefix);
        for (String conceptId : loadAllConceptIds(thesaurusId)) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null) {
                continue;
            }
            if (overwrite || StringUtils.isEmpty(concept.getIdArk())) {
                conceptRepository.setIdArk(naan.trim() + "/" + safePrefix + conceptId, new Date(), conceptId, thesaurusId);
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
        try {
            File file = new File(new URI(Objects.requireNonNull(this.getClass().getResource("/")).toString()) + fileName);
            Files.deleteIfExists(file.toPath());
            if (!file.createNewFile()) {
                throw new IllegalStateException("Impossible de créer le sitemap " + fileName);
            }
            try (var writeFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writeFile.write(buildSitemapXml(thesaurusId, originBaseUrl()));
            }
            MessageUtils.showInformationMessage("L'export du siteMap a réussi, nom du fichier " + fileName);
        } catch (Exception e) {
            MessageUtils.showErrorMessage("L'export du siteMap a échoué");
        }
    }

    public String buildSitemapXml(String thesaurusId, String baseUrl) {
        if (StringUtils.isEmpty(thesaurusId)) {
            return "";
        }
        var conceptIds = loadAllConceptIds(thesaurusId);
        return getHeader() + getDatas(conceptIds, thesaurusId, baseUrl) + "</urlset>";
    }

    private void cleanThesaurus(String thesaurusId) {
        termRepository.deleteByIdTermAndIdThesaurus("", thesaurusId);
        conceptGroupLabelRepository.deleteByIdThesaurusAndIdGroup(thesaurusId, "");
        conceptGroupRepository.deleteByIdThesaurusAndIdGroup(thesaurusId, "");
        conceptGroupRepository.cleanIdTypeCode();
        conceptRepository.cleanConcept();
    }

    private void reorganizingTopTermInThesaurus(String thesaurusId) {
        var listIds = loadTopTermIdsForRepair(thesaurusId);
        for (String idConcept : listIds) {
            conceptRepository.setTopConceptTag(true, idConcept, thesaurusId);
        }
    }

    private void removeTopTermForConceptWithBT(String thesaurusId) {
        var tabIdTT = conceptRepository.findAllByIdThesaurusAndTopConceptAndStatusNotLike(thesaurusId, true, "CA");
        for (Concept concept : tabIdTT) {
            if (hasRelationBt(concept.getIdConcept(), thesaurusId)) {
                conceptRepository.setTopConceptTag(false, concept.getIdConcept(), thesaurusId);
            }
        }
    }

    private void removeSameRelations(String idTheso) {
        removeSameRelations("BT", idTheso);
        removeSameRelations("NT", idTheso);
        removeSameRelations("RT", idTheso);
        removeSameRelations("BTG", idTheso);
        removeSameRelations("NTG", idTheso);
        removeSameRelations("BTP", idTheso);
        removeSameRelations("NTP", idTheso);
    }

    private void removeSameRelations(String role, String thesaurusId) {
        var tabRelations = hierarchicalRelationshipRepository.getListLoopRelations(thesaurusId, role);
        if (!tabRelations.isEmpty()) {
            for (HierarchicalRelationship relation : tabRelations) {
                hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                        thesaurusId, relation.getIdConcept1(), relation.getIdConcept2(), role);
            }
        }
    }

    /**
     * Complete missing BT/NT pairs and promote orphans to top concepts.
     * Handles specialized roles (BTG/NTG, BTP/NTP) as well as BT/NT.
     */
    @Transactional
    public boolean reorganizingThesaurus(String thesaurusId) {
        return doReorganizingThesaurus(thesaurusId);
    }

    private boolean doReorganizingThesaurus(String thesaurusId) {
        for (String idConcept : loadAllConceptIds(thesaurusId)) {
            List<HierarchicalRelationship> btRelations = loadBtRelations(idConcept, thesaurusId);
            List<HierarchicalRelationship> ntParentRelations = loadNtParentRelations(idConcept, thesaurusId);

            if (btRelations.isEmpty() && ntParentRelations.isEmpty()) {
                if (!isTopConcept(idConcept, thesaurusId)) {
                    conceptRepository.setTopConceptTag(true, idConcept, thesaurusId);
                }
                continue;
            }

            Set<String> existingBtKeys = btRelations.stream()
                    .map(rel -> relationKey(rel.getIdConcept2(), rel.getRole()))
                    .collect(Collectors.toSet());
            Set<String> existingNtParentKeys = ntParentRelations.stream()
                    .map(rel -> relationKey(rel.getIdConcept1(), rel.getRole()))
                    .collect(Collectors.toSet());

            for (HierarchicalRelationship ntParent : ntParentRelations) {
                String expectedBtRole = inverseHierarchicalRole(ntParent.getRole());
                if (expectedBtRole == null) {
                    continue;
                }
                String key = relationKey(ntParent.getIdConcept1(), expectedBtRole);
                if (!existingBtKeys.contains(key)) {
                    saveRelationIfAbsent(idConcept, thesaurusId, expectedBtRole, ntParent.getIdConcept1());
                    existingBtKeys.add(key);
                }
            }

            for (HierarchicalRelationship bt : btRelations) {
                String expectedNtRole = inverseHierarchicalRole(bt.getRole());
                if (expectedNtRole == null) {
                    continue;
                }
                String key = relationKey(bt.getIdConcept2(), expectedNtRole);
                if (!existingNtParentKeys.contains(key)) {
                    saveRelationIfAbsent(bt.getIdConcept2(), thesaurusId, expectedNtRole, idConcept);
                    existingNtParentKeys.add(key);
                }
            }
        }
        return true;
    }

    private int deleteConceptsWithEmptyRelation(String thesaurusId) {
        try {
            List<ConceptGroupConcept> emptyLinks = conceptGroupConceptRepository
                    .findByIdGroupAndIdThesaurus("", thesaurusId);
            if (CollectionUtils.isEmpty(emptyLinks)) {
                // also remove blank-only ids if any were stored as whitespace-only via native cleanup
                conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdGroup(thesaurusId, "");
                return 0;
            }
            int count = emptyLinks.size();
            conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdGroup(thesaurusId, "");
            return count;
        } catch (Exception e) {
            log.error("Error while deleting empty group relations for thesaurus: {}", thesaurusId, e);
            throw new IllegalStateException("Erreur pendant la suppression des relations vides", e);
        }
    }

    private int deleteConceptsHavingRelationShipWithDeletedGroup(String thesaurusId) {
        try {
            var missingGroupIds = conceptGroupConceptRepository.findGroupIdsMissingFromConceptGroup(thesaurusId);
            int cleaned = 0;
            for (String idGroup : missingGroupIds) {
                if (StringUtils.isBlank(idGroup)) {
                    continue;
                }
                List<ConceptGroupConcept> links = conceptGroupConceptRepository
                        .findByIdGroupAndIdThesaurus(idGroup, thesaurusId);
                cleaned += links.size();
                removeAllConceptsFromGroup(idGroup, thesaurusId);
            }
            return cleaned;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: {}", thesaurusId, e);
            throw new IllegalStateException(
                    "Erreur pendant la suppression des relations vers collections absentes", e);
        }
    }

    private int deleteConceptsHavingRelationShipWithDeletedConcept(String thesaurusId) {
        try {
            var orphanLinks = conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts(thesaurusId);
            int cleaned = 0;
            for (Object[] row : orphanLinks) {
                String idGroup = row[0] != null ? row[0].toString() : null;
                String idConcept = row[1] != null ? row[1].toString() : null;
                if (StringUtils.isAnyBlank(idGroup, idConcept)) {
                    continue;
                }
                conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(
                        idGroup, idConcept, thesaurusId);
                cleaned++;
            }
            return cleaned;
        } catch (Exception e) {
            log.error("Error while deleting invalid group-concept relations for thesaurus: {}", thesaurusId, e);
            throw new IllegalStateException(
                    "Erreur pendant la suppression des relations vers concepts absents", e);
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
        return CollectionUtils.isNotEmpty(loadBtRelations(conceptId, thesaurusId));
    }

    private List<HierarchicalRelationship> loadBtRelations(String conceptId, String thesaurusId) {
        return hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(
                thesaurusId, conceptId, "BT%");
    }

    private List<HierarchicalRelationship> loadNtParentRelations(String conceptId, String thesaurusId) {
        return hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike(
                thesaurusId, conceptId, "NT%");
    }

    private boolean isTopConcept(String conceptId, String thesaurusId) {
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .map(concept -> Boolean.TRUE.equals(concept.getTopConcept()))
                .orElse(false);
    }

    private void saveRelationIfAbsent(String concept1, String thesaurusId, String role, String concept2) {
        if (StringUtils.isAnyBlank(concept1, thesaurusId, role, concept2)) {
            return;
        }
        if (hierarchicalRelationshipRepository.existsByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                thesaurusId, concept1, concept2, role)) {
            return;
        }
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(concept1)
                .idConcept2(concept2)
                .idThesaurus(thesaurusId)
                .role(role)
                .build());
    }

    private static String inverseHierarchicalRole(String role) {
        if (StringUtils.isBlank(role)) {
            return null;
        }
        return switch (role) {
            case "BT" -> "NT";
            case "NT" -> "BT";
            case "BTG" -> "NTG";
            case "NTG" -> "BTG";
            case "BTP" -> "NTP";
            case "NTP" -> "BTP";
            default -> {
                if (role.startsWith("BT")) {
                    yield "NT" + role.substring(2);
                }
                if (role.startsWith("NT")) {
                    yield "BT" + role.substring(2);
                }
                yield null;
            }
        };
    }

    private static String relationKey(String otherConceptId, String role) {
        return otherConceptId + "|" + role;
    }

    private String getDatas(List<String> conceptIds, String thesaurusId, String baseUrl) {
        var date = new DateUtils().getDate();
        var stringBuilder = new StringBuilder();
        for (String conceptId : conceptIds) {
            stringBuilder.append(getLine(getUri(conceptId, thesaurusId, baseUrl), date));
        }
        return stringBuilder.toString();
    }

    private String getUri(String idConcept, String idTheso, String baseUrl) {
        String path = StringUtils.defaultIfBlank(baseUrl, originBaseUrl()).replaceAll("/$", "");
        return path + "/?idc=" + idConcept + "&amp;idt=" + idTheso;
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

    private String originBaseUrl() {
        if (FacesContext.getCurrentInstance() == null) {
            return "";
        }
        String path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return StringUtils.defaultString(path)
                + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }
}
