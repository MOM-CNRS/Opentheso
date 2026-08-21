package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Façade de l'accueil thésaurus : lit / écrit {@link ThesaurusContext}.
 * Tant qu'aucun thésaurus n'est en session, sélectionne temporairement {@code th17}.
 */
@Named("v2ThesaurusViewBean")
@ViewScoped
@RequiredArgsConstructor
public class ThesaurusViewBean implements Serializable {

    public static final String THESAURUS_ID = "th17";

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusHomeWriteService thesaurusHomeWriteService;
    private final ConceptReadService conceptReadService;
    private final UserSession userSession;
    private final RightsService rightsService;
    private final V2LocaleBean v2LocaleBean;
    private Integer conceptCount;
    private List<ThesaurusLanguage> languages;
    private String selectedLang;
    private boolean sessionReady;
    private String homePageHtml;
    private boolean homePageHtmlLoaded;
    private List<ThesaurusTreeNode> treeRoots;
    private Boolean canEdit;
    private Boolean breadcrumbEnabled;
    private Boolean sortByNotation;

    @Getter
    @Setter
    private String homeHtml;
    @Getter
    private boolean editing;
    @Getter
    private String saveMessage;
    @Getter
    private boolean saveError;
    @Getter
    @Setter
    private String openId;
    @Getter
    @Setter
    private String openType;
    @Getter
    @Setter
    private String revealId;
    @Getter
    private String revealedConceptId;
    @Getter
    private String selectedId;
    @Getter
    private String selectedKind;
    @Getter
    private ConceptDetail selectedConcept;
    @Getter
    private FacetDetailOverview selectedFacet;
    @Getter
    private String candidateBy = "";
    @Getter
    private String candidateOn = "";
    @Getter
    private boolean detailRequested;

    public void ensureSessionThesaurus() {
        if (sessionReady) {
            return;
        }
        sessionReady = true;
        if (StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId())) {
            return;
        }
        thesaurusContext.selectThesaurus(THESAURUS_ID);
    }

    public String getId() {
        ensureSessionThesaurus();
        return thesaurusContext.resolveThesaurusId();
    }

    public String getTitle() {
        ensureSessionThesaurus();
        return StringUtils.defaultString(thesaurusContext.getCurrentThesaurusTitle());
    }

    public int getConceptCount() {
        if (conceptCount == null) {
            conceptCount = thesaurusHomeQueryRepository.countValidConcepts(getId());
        }
        return conceptCount;
    }

    public String getConceptCountLabel() {
        int count = getConceptCount();
        return formatCount(count) + (count > 1 ? " concepts" : " concept");
    }

    public List<ThesaurusLanguage> getLanguages() {
        ensureLanguagesLoaded();
        return languages;
    }

    public String getSelectedLang() {
        ensureLanguagesLoaded();
        String current = StringUtils.firstNonBlank(selectedLang, thesaurusContext.resolveWorkLanguage());
        if (languageExists(current)) {
            return current;
        }
        if (languages.isEmpty()) {
            return current;
        }
        String fallback = languages.get(0).code();
        thesaurusContext.changeWorkLanguage(fallback);
        return fallback;
    }

    public void setSelectedLang(String selectedLang) {
        this.selectedLang = selectedLang;
    }

    public String getSelectedLangLabel() {
        String code = getSelectedLang();
        if (languages == null || StringUtils.isBlank(code)) {
            return code;
        }
        return languages.stream()
                .filter(lang -> code.equalsIgnoreCase(lang.code()))
                .map(ThesaurusLanguage::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(code);
    }

    public void onLanguageChange() {
        if (StringUtils.isNotBlank(selectedLang)) {
            thesaurusContext.changeWorkLanguage(selectedLang);
        }
        invalidateHomePageHtml();
        invalidateTree();
        breadcrumbEnabled = null;
        if (editing) {
            homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        }
        if (detailRequested && StringUtils.isNotBlank(selectedId)) {
            openTreeNode(selectedId, selectedKind);
        }
    }

    public List<ThesaurusTreeNode> getTreeRoots() {
        ensureTreeLoaded();
        return treeRoots;
    }

    public List<ThesaurusTreeNode> getVisibleTreeNodes() {
        List<ThesaurusTreeNode> visible = new ArrayList<>();
        appendVisibleNodes(getTreeRoots(), visible);
        return visible;
    }

    public boolean isTreeEmpty() {
        return getTreeRoots().isEmpty();
    }

    public void toggleTreeNode(String path) {
        ThesaurusTreeNode node = findTreeNodeByPath(getTreeRoots(), path);
        if (node == null || !node.isHasChildren()) {
            return;
        }
        node.setExpanded(!node.isExpanded());
        if (node.isExpanded() && !node.isChildrenLoaded()) {
            loadTreeChildren(node);
        }
    }

    public void revealInTree() {
        String id = StringUtils.trimToEmpty(revealId);
        revealedConceptId = id;
        if (StringUtils.isBlank(id)) {
            return;
        }
        ensureTreeLoaded();
        List<List<BreadcrumbStep>> paths = conceptReadService.loadBreadcrumbPaths(
                getId(),
                id,
                getSelectedLang()
        );
        if (paths == null || paths.isEmpty()) {
            List<BreadcrumbStep> fallback = conceptReadService.loadBreadcrumb(getId(), id, getSelectedLang());
            paths = fallback == null || fallback.isEmpty()
                    ? List.of(List.of(new BreadcrumbStep(id, id, 0, true)))
                    : List.of(fallback);
        }
        for (List<BreadcrumbStep> path : paths) {
            List<String> ids = new ArrayList<>();
            if (path != null) {
                for (BreadcrumbStep step : path) {
                    if (step != null && StringUtils.isNotBlank(step.getConceptId())) {
                        ids.add(step.getConceptId());
                    }
                }
            }
            if (ids.isEmpty() || !ids.get(ids.size() - 1).equalsIgnoreCase(id)) {
                ids.add(id);
            }
            if (expandTreePath(ids)) {
                return;
            }
        }
    }

    public void openSelectedNode() {
        openTreeNode(openId, openType);
    }

    public void openTreeNode(String id, String nodeType) {
        detailRequested = true;
        selectedId = StringUtils.trimToEmpty(id);
        selectedKind = "";
        selectedConcept = null;
        selectedFacet = null;
        candidateBy = "";
        candidateOn = "";
        if (StringUtils.isBlank(id)) {
            return;
        }
        if ("facet".equalsIgnoreCase(nodeType)) {
            selectedFacet = conceptReadService.loadFacetDetail(getId(), id, getSelectedLang()).orElse(null);
            if (selectedFacet != null) {
                selectedKind = "facet";
            }
            return;
        }
        selectedConcept = conceptReadService.loadDetail(getId(), id, getSelectedLang(), true).orElse(null);
        if (selectedConcept == null) {
            return;
        }
        boolean candidate = "candidat".equalsIgnoreCase(nodeType)
                || "CA".equalsIgnoreCase(selectedConcept.getSummary().getStatus());
        selectedKind = candidate ? "candidat" : "concept";
        if (candidate) {
            applySelectedCandidateMeta(id);
        }
    }

    public boolean isDetailVisible() {
        return selectedConcept != null || selectedFacet != null;
    }

    public boolean isConceptSelected() {
        return "concept".equals(selectedKind) && selectedConcept != null;
    }

    public boolean isCandidateSelected() {
        return "candidat".equals(selectedKind) && selectedConcept != null;
    }

    public boolean isFacetSelected() {
        return "facet".equals(selectedKind) && selectedFacet != null;
    }

    public boolean isSelectedConceptDeprecated() {
        return selectedConcept != null && selectedConcept.isDeprecated();
    }

    public boolean isBreadcrumbEnabled() {
        if (breadcrumbEnabled == null) {
            ThesaurusPreferences preferences = loadThesaurusPreferences();
            breadcrumbEnabled = preferences != null && preferences.breadcrumb();
        }
        return breadcrumbEnabled;
    }

    /**
     * Tri par défaut de l'arbre, d'après la préférence « Tri par défaut sur la notation ».
     * Le switch du menu contextuel peut le surcharger pour la vue courante (comme le legacy).
     */
    public boolean isSortByNotation() {
        if (sortByNotation == null) {
            ThesaurusPreferences preferences = loadThesaurusPreferences();
            sortByNotation = preferences != null && preferences.sortByNotation();
        }
        return sortByNotation;
    }

    public void setAlphabeticSort() {
        applyTreeSort(false);
    }

    public void setNotationSort() {
        applyTreeSort(true);
    }

    /**
     * Recharge le tri de l'arbre après enregistrement des préférences (même vue, sidebar visible).
     */
    public void applyPreferenceTreeSort(boolean byNotation) {
        applyTreeSort(byNotation);
    }

    private void applyTreeSort(boolean byNotation) {
        sortByNotation = byNotation;
        invalidateTree();
    }

    private ThesaurusPreferences loadThesaurusPreferences() {
        return thesaurusPreferenceService.loadPreferencesOrNull(
                getId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public String noteTypeLabel(String typeCode) {
        return switch (StringUtils.defaultString(typeCode)) {
            case "definition" -> "Définition";
            case "scopeNote" -> "Note d'application";
            case "example" -> "Exemple";
            case "historyNote" -> "Note historique";
            case "editorialNote" -> "Note éditoriale";
            case "changeNote" -> "Note de changement";
            default -> "Note";
        };
    }

    public String dash(String value) {
        return StringUtils.isBlank(value) ? "—" : value;
    }

    public String getCandidateTitle() {
        return selectedConcept == null ? "" : StringUtils.defaultString(selectedConcept.getSummary().getPreferredLabel());
    }

    public String getCandidatePath() {
        if (selectedConcept == null || selectedConcept.getBreadcrumb() == null) {
            return "";
        }
        return selectedConcept.getBreadcrumb().stream()
                .map(BreadcrumbStep::getLabel)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" › "));
    }

    public String getCandidateParentName() {
        if (selectedConcept == null || selectedConcept.getBroaderTerms() == null
                || selectedConcept.getBroaderTerms().isEmpty()) {
            return "";
        }
        return selectedConcept.getBroaderTerms().get(0).getDisplayLabel();
    }

    public boolean isCandidateUnderParent() {
        return StringUtils.isNotBlank(getCandidateParentName());
    }

    public String getCandidateAlts() {
        return selectedConcept == null ? "" : joinValues(selectedConcept.getSynonyms());
    }

    public String getCandidateCollections() {
        return selectedConcept == null ? "" : joinRelations(selectedConcept.getCollections());
    }

    public String getCandidateBroader() {
        return selectedConcept == null ? "" : joinRelations(selectedConcept.getBroaderTerms());
    }

    public String getCandidateNarrower() {
        return selectedConcept == null ? "" : joinRelations(selectedConcept.getNarrowerTerms());
    }

    public String getCandidateRelated() {
        return selectedConcept == null ? "" : joinRelations(selectedConcept.getRelatedTerms());
    }

    public String getCandidateTranslation(String lang) {
        if (selectedConcept == null || selectedConcept.getTranslations() == null || StringUtils.isBlank(lang)) {
            return "";
        }
        return selectedConcept.getTranslations().stream()
                .filter(item -> lang.equalsIgnoreCase(item.getLang()))
                .map(ConceptLabel::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    public String getCandidateDefinition() {
        return firstNote("definition");
    }

    public String getCandidateScopeNote() {
        return firstNote("scopeNote");
    }

    private String firstNote(String typeCode) {
        if (selectedConcept == null || selectedConcept.getNotes() == null) {
            return "";
        }
        return selectedConcept.getNotes().stream()
                .filter(note -> typeCode.equals(note.getTypeCode()))
                .map(ConceptNote::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    private static String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(", "));
    }

    private static String joinRelations(List<ConceptRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return "";
        }
        return relations.stream()
                .map(ConceptRelation::getDisplayLabel)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    public String getHomePageHtml() {
        if (!homePageHtmlLoaded) {
            homePageHtml = StringUtils.defaultString(
                    thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage())
            );
            homePageHtmlLoaded = true;
        }
        return homePageHtml;
    }

    public boolean isHomePageHtmlPresent() {
        return StringUtils.isNotBlank(getHomePageHtml());
    }

    public boolean isCanEdit() {
        if (canEdit != null) {
            return canEdit;
        }
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            canEdit = false;
            return false;
        }
        canEdit = rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
        return canEdit;
    }

    public boolean isSuperAdmin() {
        return userSession.isSuperAdmin();
    }

    public void startEditing() {
        saveMessage = null;
        saveError = false;
        if (!isCanEdit()) {
            return;
        }
        homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        editing = true;
    }

    public void cancelEditing() {
        editing = false;
        homeHtml = null;
        saveMessage = null;
        saveError = false;
    }

    public void saveHomeHtml() {
        saveMessage = null;
        saveError = false;
        if (!isCanEdit()) {
            saveError = true;
            saveMessage = "Action non autorisée";
            return;
        }
        boolean ok = thesaurusHomeWriteService.saveHtml(
                getId(),
                thesaurusContext.resolveWorkLanguage(),
                homeHtml
        );
        if (!ok) {
            saveError = true;
            saveMessage = "L'enregistrement a échoué.";
            return;
        }
        editing = false;
        homeHtml = null;
        invalidateHomePageHtml();
        saveMessage = "Description enregistrée.";
    }

    private void invalidateHomePageHtml() {
        homePageHtml = null;
        homePageHtmlLoaded = false;
    }

    private void invalidateTree() {
        treeRoots = null;
    }

    private void ensureTreeLoaded() {
        if (treeRoots != null) {
            return;
        }
        List<ConceptTreeNodeData> roots = conceptReadService.loadTreeRootNodes(
                getId(),
                getSelectedLang(),
                isSortByNotation()
        );
        treeRoots = new ArrayList<>();
        if (roots == null) {
            return;
        }
        for (ConceptTreeNodeData data : roots) {
            treeRoots.add(toTreeNode(data, 0, ""));
        }
        applyCandidateMeta(treeRoots);
    }

    private void loadTreeChildren(ThesaurusTreeNode parent) {
        List<ConceptTreeNodeData> children = conceptReadService.loadTreeChildNodes(
                parent.getId(),
                parent.getNodeType(),
                getId(),
                getSelectedLang(),
                isSortByNotation()
        );
        parent.getChildren().clear();
        if (children != null) {
            for (ConceptTreeNodeData data : children) {
                parent.getChildren().add(toTreeNode(data, parent.getDepth() + 1, parent.getPath()));
            }
        }
        applyCandidateMeta(parent.getChildren());
        parent.setChildrenLoaded(true);
    }

    private ThesaurusTreeNode toTreeNode(ConceptTreeNodeData data, int depth, String parentPath) {
        ThesaurusTreeNode node = new ThesaurusTreeNode();
        node.setId(data.getNodeId());
        node.setLabel(data.getLabel());
        node.setNotation(data.getNotation());
        node.setNodeType(data.getNodeType());
        node.setHasChildren(data.isHasChildren());
        node.setDepth(depth);
        node.setPath(StringUtils.isBlank(parentPath) ? data.getLabel() : parentPath + "/" + data.getLabel());
        if ("candidat".equals(data.getNodeType())) {
            node.setStatus("candidat");
        } else if ("deprecated".equals(data.getNodeType())) {
            node.setStatus("deprecie");
        } else {
            node.setStatus("valide");
        }
        return node;
    }

    private void applySelectedCandidateMeta(String conceptId) {
        for (Object[] row : conceptReadService.loadCandidateMeta(getId(), List.of(conceptId))) {
            if (row == null || row.length < 3 || row[0] == null || !conceptId.equals(row[0].toString())) {
                continue;
            }
            candidateBy = row[1] == null ? "" : row[1].toString();
            candidateOn = row[2] == null ? "" : row[2].toString();
            return;
        }
    }

    private void applyCandidateMeta(List<ThesaurusTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        List<String> ids = nodes.stream()
                .filter(ThesaurusTreeNode::isCandidate)
                .map(ThesaurusTreeNode::getId)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        for (Object[] row : conceptReadService.loadCandidateMeta(getId(), ids)) {
            String id = row[0] == null ? "" : row[0].toString();
            String by = row[1] == null ? "" : row[1].toString();
            String on = row[2] == null ? "" : row[2].toString();
            for (ThesaurusTreeNode node : nodes) {
                if (id.equals(node.getId())) {
                    node.setCandidateBy(by);
                    node.setCandidateOn(on);
                }
            }
        }
    }

    private void appendVisibleNodes(List<ThesaurusTreeNode> nodes, List<ThesaurusTreeNode> out) {
        if (nodes == null) {
            return;
        }
        for (ThesaurusTreeNode node : nodes) {
            out.add(node);
            if (node.isExpanded() && !node.getChildren().isEmpty()) {
                appendVisibleNodes(node.getChildren(), out);
            }
        }
    }

    private boolean expandTreePath(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (int start = 0; start < ids.size(); start++) {
            if (expandTreePathFrom(getTreeRoots(), ids, start)) {
                return true;
            }
        }
        return false;
    }

    private boolean expandTreePathFrom(List<ThesaurusTreeNode> siblings, List<String> ids, int index) {
        if (siblings == null || index >= ids.size()) {
            return index >= ids.size();
        }
        String targetId = ids.get(index);
        for (ThesaurusTreeNode node : siblings) {
            if (!targetId.equalsIgnoreCase(node.getId())) {
                continue;
            }
            boolean last = index == ids.size() - 1;
            if (!last && node.isHasChildren()) {
                node.setExpanded(true);
                if (!node.isChildrenLoaded()) {
                    loadTreeChildren(node);
                }
                return expandTreePathFrom(node.getChildren(), ids, index + 1);
            }
            return last;
        }
        return false;
    }

    private ThesaurusTreeNode findTreeNodeByPath(List<ThesaurusTreeNode> nodes, String path) {
        if (nodes == null || StringUtils.isBlank(path)) {
            return null;
        }
        for (ThesaurusTreeNode node : nodes) {
            if (path.equals(node.getPath())) {
                return node;
            }
            ThesaurusTreeNode found = findTreeNodeByPath(node.getChildren(), path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void ensureLanguagesLoaded() {
        if (languages != null) {
            return;
        }
        languages = thesaurusPreferenceService.loadUsedLanguages(getId(), v2LocaleBean.getIdLangue());
        if (languages == null) {
            languages = Collections.emptyList();
        }
    }

    private boolean languageExists(String lang) {
        return StringUtils.isNotBlank(lang)
                && languages.stream().anyMatch(item -> item.code().equalsIgnoreCase(lang));
    }

    private static String formatCount(int count) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(count);
    }
}
