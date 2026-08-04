package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.alignment.ui.ConceptAlignmentAdminBean;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.ui.PropositionBean;
import fr.cnrs.opentheso.v2.proposition.ui.PropositionSubmitBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.LeftTreeMode;
import fr.cnrs.opentheso.v2.concept.model.RightPanelMode;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusHomeOverview;
import fr.cnrs.opentheso.v2.concept.policy.ConceptAccessPolicy;
import fr.cnrs.opentheso.v2.concept.support.ConceptFlagSupport;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptTypeReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshState;
import fr.cnrs.opentheso.v2.concept.support.ConceptGpsMapRenderer;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.event.FacesEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusBrowseBean")
@RequiredArgsConstructor
public class ThesaurusBrowseBean implements Serializable, ConceptNavigationSupport {

    private final ThesaurusContext thesaurusContext;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptReadService conceptReadService;
    private final CollectionReadService collectionReadService;
    private final FacetReadService facetReadService;
    private final ConceptFullReadService conceptFullReadService;
    private final ThesaurusHomeReadService thesaurusHomeReadService;
    private final ConceptHistoryBean conceptHistoryBean;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final UserSession userSession;
    private final ConceptTypeReadService conceptTypeReadService;
    private final ConceptTreeRefreshState conceptTreeRefreshState;
    private final RightsService rightsService;
    private final ObjectProvider<ConceptAlignmentAdminBean> conceptAlignmentAdminBean;
    private final ObjectProvider<PropositionSubmitBean> propositionSubmitBean;
    private final ObjectProvider<PropositionBean> propositionBean;
    private final ObjectProvider<ThesaurusHomeEditorBean> thesaurusHomeEditorBean;

    private String conceptIdFromUri;
    private String groupIdFromUri;
    private String facetIdFromUri;
    private TreeNode conceptRoot;
    private TreeNode collectionRoot;
    private TreeNode arbreRoot;
    private TreeNode selectedNode;
    private ConceptDetail selectedConcept;
    private ConceptFullSnapshot selectedFullConcept;
    private List<ConceptCorpusLinkItem> displayedCorpusLinks = Collections.emptyList();
    private boolean corpusSearched;
    private boolean haveActiveCorpus;
    private GroupDetailOverview selectedGroup;
    private FacetDetailOverview selectedFacet;
    private ThesaurusHomeOverview thesaurusHome;
    private LeftTreeMode activeLeftTreeMode = LeftTreeMode.CONCEPT;
    private RightPanelMode rightPanelMode = RightPanelMode.HOME;
    private int leftTabIndex;
    /** Onglet logique du panneau droit (indépendant des onglets rendus dynamiquement). */
    private RightTabKey rightTabKey = RightTabKey.CONCEPT;

    private enum RightTabKey {
        CONCEPT,
        COLLECTION,
        ALIGNMENT,
        SUGGESTION
    }

    private String indexQuery;
    private boolean indexPermuted;
    private boolean indexWithAltLabel;
    private List<ConceptTreeNodeData> indexResults = Collections.emptyList();
    private ConceptTreeNodeData indexSelected;

    private boolean breadcrumbEnabled = true;
    private boolean useConceptTree = true;
    private boolean displayUserName = true;
    private boolean kohaLink;
    private boolean showHistoryNote;
    private boolean showEditorialNote;
    private boolean useCustomRelation;
    private boolean autoExpandTree = true;
    private boolean treeCacheEnabled;
    private boolean sortByNotation;
    private boolean manySiblings;
    private boolean useDeeplTranslation;
    private boolean suggestionEnabled;
    /** Onglet Suggestion activé après clic sur « Proposer une amélioration » (comme legacy isRubriqueVisible). */
    private boolean propositionRubriqueVisible;
    private boolean showAllNoteLanguages = true;
    private boolean showAllSynonymLanguages = true;
    private int branchConceptCount;
    private List<ConceptNote> displayedNotes = Collections.emptyList();
    private List<ConceptLabel> displayedSynonymLabels = Collections.emptyList();
    private int narrowerOffset;
    private boolean haveMoreNarrowers;

    public void load() {
        thesaurusContext.syncFromViewParams();

        String restoreConceptId = StringUtils.trimToNull(thesaurusContext.getIdConceptFromUri());
        String restoreGroupId = StringUtils.trimToNull(thesaurusContext.getIdGroupFromUri());
        String restoreFacetId = StringUtils.trimToNull(thesaurusContext.getIdFacetFromUri());
        thesaurusContext.setIdConceptFromUri(null);
        thesaurusContext.setIdGroupFromUri(null);
        thesaurusContext.setIdFacetFromUri(null);
        conceptIdFromUri = null;
        groupIdFromUri = null;
        facetIdFromUri = null;

        // Comme legacy (ConceptView en session) : conserver la sélection au refresh de page.
        if (restoreConceptId == null && restoreGroupId == null && restoreFacetId == null
                && conceptSelectionContext.hasSelection()
                && thesaurusContext.matchesCurrentThesaurus(conceptSelectionContext.getThesaurusId())) {
            restoreConceptId = conceptSelectionContext.getConceptId();
        }

        resetState();
        if (conceptTreeRefreshState.consumeRefresh()) {
            invalidateConceptTree();
        }
        if (!isScreenAvailable()) {
            return;
        }
        loadConsultationPreferences();
        refreshActiveCorpusState();
        ensureTreeBuilt(activeLeftTreeMode);
        loadThesaurusHome();
        if (restoreConceptId != null) {
            openConcept(restoreConceptId);
        } else if (restoreGroupId != null) {
            focusGroup(restoreGroupId);
        } else if (restoreFacetId != null) {
            focusFacet(restoreFacetId);
        }
    }

    public boolean isScreenAvailable() {
        return ConceptAccessPolicy.hasSelectedThesaurus(thesaurusContext.resolveThesaurusId());
    }

    public boolean isHomePanel() {
        return rightPanelMode == RightPanelMode.HOME;
    }

    public boolean isConceptPanel() {
        return rightPanelMode == RightPanelMode.CONCEPT;
    }

    public boolean isGroupPanel() {
        return rightPanelMode == RightPanelMode.GROUP;
    }

    public boolean isFacetPanel() {
        return rightPanelMode == RightPanelMode.FACET;
    }

    /** Concept ou collection sélectionné : affiche la barre d'onglets (comme legacy). */
    public boolean isValueSelected() {
        return isConceptPanel() || isGroupPanel();
    }

    public boolean isSuggestionEnabled() {
        return suggestionEnabled;
    }

    /** Visibilité de l'onglet Alignement (comme legacy alignementVisible / admin). */
    public boolean isAlignmentTabVisible() {
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
    }

    /**
     * Index PrimeFaces parmi les onglets visibles
     * (Concept, Collection, Alignement?, Suggestion?).
     */
    public int getRightTabIndex() {
        List<RightTabKey> visible = visibleRightTabs();
        int index = visible.indexOf(rightTabKey);
        return index >= 0 ? index : 0;
    }

    public void setRightTabIndex(int index) {
        List<RightTabKey> visible = visibleRightTabs();
        if (index >= 0 && index < visible.size()) {
            rightTabKey = visible.get(index);
        }
    }

    private List<RightTabKey> visibleRightTabs() {
        if (!isValueSelected()) {
            return List.of(RightTabKey.CONCEPT);
        }
        var tabs = new ArrayList<RightTabKey>();
        tabs.add(RightTabKey.CONCEPT);
        tabs.add(RightTabKey.COLLECTION);
        if (isAlignmentTabVisible()) {
            tabs.add(RightTabKey.ALIGNMENT);
        }
        if (suggestionEnabled) {
            tabs.add(RightTabKey.SUGGESTION);
        }
        return List.copyOf(tabs);
    }

    private void activateRightTab(RightTabKey key) {
        rightTabKey = key;
    }

    public String getPageTitle() {
        if (selectedConcept != null && selectedConcept.summary() != null) {
            String label = selectedConcept.summary().preferredLabel();
            return StringUtils.isNotBlank(label) ? label : selectedConcept.summary().conceptId();
        }
        if (selectedGroup != null && StringUtils.isNotBlank(selectedGroup.label())) {
            return selectedGroup.label();
        }
        if (selectedFacet != null && StringUtils.isNotBlank(selectedFacet.label())) {
            return selectedFacet.label();
        }
        if (StringUtils.isNotBlank(getThesaurusTitle())) {
            return getThesaurusTitle();
        }
        return "Opentheso";
    }

    public String getThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    public String getThesaurusTitle() {
        return thesaurusContext.getCurrentThesaurusTitle();
    }

    public String getLanguage() {
        return thesaurusContext.resolveWorkLanguage();
    }

    public void onLeftTabChange(TabChangeEvent<?> event) {
        activeLeftTreeMode = resolveTreeMode(event.getTab().getId());
        leftTabIndex = switch (event.getTab().getId()) {
            case "viewTabList" -> 1;
            case "viewTabGroups" -> 2;
            case "viewTabConceptTree" -> 3;
            default -> 0;
        };
        // Comme legacy : changer d'onglet gauche ne réinitialise pas le panneau droit
        ensureTreeBuilt(activeLeftTreeMode);
    }

    public void onRightTabChange(TabChangeEvent<?> event) {
        if (event.getTab() == null) {
            return;
        }
        String tabId = event.getTab().getId();
        if ("viewTabAlignement".equals(tabId)) {
            activateRightTab(RightTabKey.ALIGNMENT);
            conceptAlignmentAdminBean.getObject().openForCurrentConcept();
        } else if ("viewTabGroup".equals(tabId)) {
            activateRightTab(RightTabKey.COLLECTION);
        } else if ("viewTabSuggestion".equals(tabId)) {
            activateRightTab(RightTabKey.SUGGESTION);
        } else {
            activateRightTab(RightTabKey.CONCEPT);
        }
    }

    public void onNodeExpand(NodeExpandEvent event) {
        LeftTreeMode mode = resolveTreeModeFromComponent(event);
        DefaultTreeNode node = (DefaultTreeNode) event.getTreeNode();
        if (node.getChildCount() == 1 && isDummyChild(node)) {
            node.getChildren().clear();
            ConceptTreeNodeData data = (ConceptTreeNodeData) node.getData();
            loadChildren(node, data, mode);
        }
    }

    public void onNodeSelect(NodeSelectEvent event) {
        LeftTreeMode mode = resolveTreeModeFromComponent(event);
        TreeNode newlySelected = event.getTreeNode();
        // Un seul nœud sélectionné dans les arbres gauches (comme legacy).
        selectSingleTreeNode(newlySelected);
        ConceptTreeNodeData data = newlySelected == null ? null : (ConceptTreeNodeData) newlySelected.getData();
        if (data == null || data.isDummy() || "root".equals(data.nodeType()) || "....".equals(data.nodeId())) {
            openThesaurusHome();
            refreshAfterTreeSelection();
            return;
        }
        if (data.isGroup() && (mode == LeftTreeMode.COLLECTION || mode == LeftTreeMode.ARBRE)) {
            openGroup(data.nodeId());
            refreshAfterTreeSelection();
            return;
        }
        if ("facet".equals(data.nodeType())) {
            openFacet(data.nodeId());
            refreshAfterTreeSelection();
            return;
        }
        openConcept(data.nodeId(), false);
        refreshAfterTreeSelection();
    }

    private void refreshAfterTreeSelection() {
        if (!PrimeFaces.current().isAjaxRequest()) {
            return;
        }
        PrimeFaces.current().ajax().update(
                "indexTitle",
                "containerIndex:formRightTab",
                "containerIndex:tabTree"
        );
    }

    public void onIndexChange() {
        if (!isScreenAvailable() || StringUtils.isBlank(indexQuery)) {
            indexResults = Collections.emptyList();
            return;
        }
        indexResults = conceptReadService.searchIndex(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                indexQuery,
                indexPermuted,
                indexWithAltLabel,
                100
        );
    }

    public void onIndexSelect(SelectEvent<ConceptTreeNodeData> event) {
        ConceptTreeNodeData selected = event.getObject();
        if (selected == null || StringUtils.isBlank(selected.nodeId())) {
            return;
        }
        indexSelected = selected;
        openConcept(selected.nodeId());
    }

    public void openThesaurusHome() {
        rightPanelMode = RightPanelMode.HOME;
        activateRightTab(RightTabKey.CONCEPT);
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedGroup = null;
        selectedFacet = null;
        clearAllLeftTreeSelections();
        branchConceptCount = 0;
        displayedNotes = Collections.emptyList();
        displayedSynonymLabels = Collections.emptyList();
        displayedCorpusLinks = Collections.emptyList();
        corpusSearched = false;
        ThesaurusHomeEditorBean homeEditor = thesaurusHomeEditorBean.getIfAvailable();
        if (homeEditor != null) {
            homeEditor.reset();
        }
        loadThesaurusHome();
    }

    public void refreshSelectedConcept() {
        if (selectedConcept != null && selectedConcept.summary() != null) {
            openConcept(selectedConcept.summary().conceptId(), false);
        }
    }

    @Override
    public void refreshAfterRename(String conceptId, String newLabel) {
        openConcept(conceptId, false);
        updateSelectedTreeNodeLabel(conceptId, newLabel);
    }

    @Override
    public void refreshAfterNotationUpdate(String conceptId, String notation) {
        openConcept(conceptId, false);
        updateSelectedTreeNodeNotation(conceptId, notation);
    }

    private void updateSelectedTreeNodeLabel(String conceptId, String newLabel) {
        if (!(selectedNode instanceof DefaultTreeNode defaultNode)) {
            return;
        }
        if (!(defaultNode.getData() instanceof ConceptTreeNodeData data)) {
            return;
        }
        if (!conceptId.equalsIgnoreCase(data.nodeId())) {
            return;
        }
        defaultNode.setData(new ConceptTreeNodeData(
                data.nodeId(),
                newLabel,
                data.notation(),
                data.nodeType(),
                data.hasChildren()
        ));
    }

    private void updateSelectedTreeNodeNotation(String conceptId, String notation) {
        if (!(selectedNode instanceof DefaultTreeNode defaultNode)) {
            return;
        }
        if (!(defaultNode.getData() instanceof ConceptTreeNodeData data)) {
            return;
        }
        if (!conceptId.equalsIgnoreCase(data.nodeId())) {
            return;
        }
        defaultNode.setData(new ConceptTreeNodeData(
                data.nodeId(),
                data.label(),
                notation,
                data.nodeType(),
                data.hasChildren()
        ));
    }

    @Override
    public void invalidateConceptTree() {
        conceptRoot = null;
        arbreRoot = null;
    }

    public void invalidateCollectionTree() {
        collectionRoot = null;
        arbreRoot = null;
    }

    /**
     * Lazy-builds the concept tree when the model was invalidated (create/delete/refresh)
     * so AJAX updates of {@code formLeftTab} always receive a usable root.
     */
    public TreeNode getConceptRoot() {
        if (conceptRoot == null && isScreenAvailable()) {
            ensureTreeBuilt(LeftTreeMode.CONCEPT);
        }
        return conceptRoot;
    }

    /**
     * Lazy-builds the collection tree after invalidation so newly created collections
     * appear without requiring a tab switch.
     */
    public TreeNode getCollectionRoot() {
        if (collectionRoot == null && isScreenAvailable()) {
            ensureTreeBuilt(LeftTreeMode.COLLECTION);
        }
        return collectionRoot;
    }

    public TreeNode getArbreRoot() {
        if (arbreRoot == null && isScreenAvailable() && useConceptTree
                && activeLeftTreeMode == LeftTreeMode.ARBRE) {
            ensureTreeBuilt(LeftTreeMode.ARBRE);
        }
        return arbreRoot;
    }

    @Override
    public void afterConceptDeleted(String fallbackConceptId) {
        invalidateConceptTree();
        clearAllLeftTreeSelections();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(fallbackConceptId)) {
            openConcept(fallbackConceptId);
        } else {
            openThesaurusHome();
        }
    }

    @Override
    public void reloadAfterLanguageChange() {
        if (!isScreenAvailable()) {
            return;
        }
        String conceptId = selectedConcept != null && selectedConcept.summary() != null
                ? selectedConcept.summary().conceptId()
                : null;
        String groupId = selectedGroup != null ? selectedGroup.groupId() : null;
        String facetId = selectedFacet != null ? selectedFacet.facetId() : null;
        RightPanelMode previousPanel = rightPanelMode;
        LeftTreeMode previousTree = activeLeftTreeMode;
        int previousLeftTab = leftTabIndex;
        RightTabKey previousRightTab = rightTabKey;

        invalidateConceptTree();
        invalidateCollectionTree();
        indexResults = Collections.emptyList();
        indexSelected = null;
        selectedNode = null;
        conceptSelectionContext.clear();

        activeLeftTreeMode = previousTree;
        leftTabIndex = previousLeftTab;
        rightTabKey = previousRightTab;
        ensureTreeBuilt(activeLeftTreeMode);

        if (previousPanel == RightPanelMode.CONCEPT && StringUtils.isNotBlank(conceptId)) {
            openConcept(conceptId, true);
        } else if (previousPanel == RightPanelMode.GROUP && StringUtils.isNotBlank(groupId)) {
            focusGroup(groupId);
        } else if (previousPanel == RightPanelMode.FACET && StringUtils.isNotBlank(facetId)) {
            focusFacet(facetId);
        } else {
            openThesaurusHome();
        }
        PrimeFaces.current().executeScript("typeof srollToSelected === 'function' && srollToSelected();");
    }

    public boolean isPropositionAuthorized() {
        return suggestionEnabled;
    }

    /**
     * Comme legacy {@code propositionBean.switchToNouvelleProposition} :
     * prépare le formulaire et bascule sur l'onglet Suggestion.
     */
    public void openNouvelleProposition() {
        if (!suggestionEnabled || !isConceptPanel()) {
            return;
        }
        propositionBean.getObject().clearConsultation();
        propositionSubmitBean.getObject().prepare();
        propositionRubriqueVisible = true;
        activateRightTab(RightTabKey.SUGGESTION);
    }

    /**
     * Consultation d'une proposition depuis le tiroir (comme legacy {@code onSelectConcept}).
     */
    public void openPropositionConsultation(PropositionSummary proposition) {
        if (proposition == null || StringUtils.isAnyBlank(proposition.thesaurusId(), proposition.conceptId())) {
            return;
        }

        String thesaurusId = proposition.thesaurusId().trim();
        boolean thesaurusChanged = !thesaurusId.equalsIgnoreCase(
                StringUtils.defaultString(thesaurusContext.resolveThesaurusId()));

        if (thesaurusChanged) {
            thesaurusContext.selectThesaurus(thesaurusId);
            if (StringUtils.isNotBlank(proposition.lang())) {
                thesaurusContext.changeWorkLanguage(proposition.lang().trim());
            }
            loadConsultationPreferences();
            invalidateConceptTree();
            ensureTreeBuilt(activeLeftTreeMode);
            loadThesaurusHome();
        } else if (StringUtils.isNotBlank(proposition.lang())
                && !proposition.lang().equalsIgnoreCase(thesaurusContext.resolveWorkLanguage())) {
            thesaurusContext.changeWorkLanguage(proposition.lang().trim());
        }

        if (!suggestionEnabled) {
            MessageUtils.showWarnMessage(
                    // même message que legacy
                    "La suggestion est désactivée pour le thésaurus dans lequel la proposition sélectionnée appartient !"
            );
            return;
        }

        openConcept(proposition.conceptId().trim(), true);
        propositionBean.getObject().openReview(proposition);
        propositionRubriqueVisible = true;
        activateRightTab(RightTabKey.SUGGESTION);
    }

    /**
     * Retour à l'onglet Concept (équivalent legacy {@code annuler} / switchToConceptOnglet).
     */
    public void closeProposition() {
        propositionBean.getObject().clearConsultation();
        propositionRubriqueVisible = false;
        activateRightTab(RightTabKey.CONCEPT);
    }

    public void submitNouvelleProposition() {
        if (propositionSubmitBean.getObject().submit()) {
            closeProposition();
        }
    }

    public void executePropositionDecision() {
        propositionBean.getObject().executePendingAction();
        if (!propositionBean.getObject().isConsultation()) {
            propositionRubriqueVisible = false;
            activateRightTab(RightTabKey.CONCEPT);
        }
    }

    public void openConcept(String conceptId) {
        openConcept(conceptId, true);
    }

    public void openConcept(String conceptId, boolean syncTree) {
        if (StringUtils.isBlank(conceptId)) {
            openThesaurusHome();
            return;
        }
        rightPanelMode = RightPanelMode.CONCEPT;
        selectedGroup = null;
        selectedFacet = null;
        if (syncTree) {
            activeLeftTreeMode = LeftTreeMode.CONCEPT;
            leftTabIndex = 0;
        }
        // Comme legacy : rester sur Alignement si déjà ouvert, sinon onglet Concept
        boolean stayOnAlignmentTab = rightTabKey == RightTabKey.ALIGNMENT;
        if (!stayOnAlignmentTab) {
            activateRightTab(RightTabKey.CONCEPT);
        }
        propositionBean.getObject().clearConsultation();
        propositionRubriqueVisible = false;
        corpusSearched = false;
        displayedCorpusLinks = Collections.emptyList();
        Optional<ConceptReadService.ConceptDetailLoadResult> loaded = conceptReadService.loadDetailWithSource(
                thesaurusContext.resolveThesaurusId(),
                conceptId,
                thesaurusContext.resolveWorkLanguage()
        );
        selectedFullConcept = loaded.map(ConceptReadService.ConceptDetailLoadResult::fullConcept).orElse(null);
        selectedConcept = loaded.map(ConceptReadService.ConceptDetailLoadResult::detail).orElse(null);
        narrowerOffset = 0;
        haveMoreNarrowers = conceptFullReadService.hasMoreNarrowers(selectedFullConcept);
        if (haveMoreNarrowers) {
            narrowerOffset = conceptFullReadService.nextNarrowerOffset(0);
        }
        refreshConceptDisplayData();
        conceptSelectionContext.update(thesaurusContext.resolveThesaurusId(), selectedConcept);
        // Recharger l'atelier d'alignement APRÈS la mise à jour du contexte (sinon résumé obsolète).
        if (stayOnAlignmentTab) {
            conceptAlignmentAdminBean.getObject().openForCurrentConcept();
        }
        if (syncTree && autoExpandTree) {
            // Comme legacy ConceptView#getConcept → tree.expandTreeToPath
            ensureTreeBuilt(LeftTreeMode.CONCEPT);
            syncConceptTreeSelection(conceptId);
            scrollToSelectedNode();
        }
    }

    /**
     * Recharge l'arbre concept (comme legacy {@code tree.reloadSelectedConcept}).
     */
    public void reloadConceptTree() {
        manySiblings = false;
        invalidateConceptTree();
        ensureTreeBuilt(LeftTreeMode.CONCEPT);
        if (selectedConcept != null && selectedConcept.summary() != null && autoExpandTree) {
            syncConceptTreeSelection(selectedConcept.summary().conceptId());
            scrollToSelectedNode();
        }
    }

    public void setAlphabeticSort() {
        persistTreeSort(false);
    }

    public void setNotationSort() {
        persistTreeSort(true);
    }

    private void persistTreeSort(boolean byNotation) {
        sortByNotation = byNotation;
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isNotBlank(thesaurusId)) {
            try {
                thesaurusPreferenceService.updateSortByNotation(
                        thesaurusId,
                        byNotation,
                        thesaurusContext.resolveWorkLanguage()
                );
            } catch (RuntimeException ex) {
                MessageUtils.showWarnMessage("Tri appliqué pour la session, mais non enregistré en préférences");
            }
        }
        reloadAllLeftTrees();
    }

    private void reloadAllLeftTrees() {
        manySiblings = false;
        invalidateConceptTree();
        invalidateCollectionTree();
        ensureTreeBuilt(activeLeftTreeMode);
        if (activeLeftTreeMode == LeftTreeMode.CONCEPT
                && selectedConcept != null
                && selectedConcept.summary() != null
                && autoExpandTree) {
            syncConceptTreeSelection(selectedConcept.summary().conceptId());
            scrollToSelectedNode();
        }
    }

    private void scrollToSelectedNode() {
        if (PrimeFaces.current().isAjaxRequest()) {
            PrimeFaces.current().executeScript("typeof srollToSelected === 'function' && srollToSelected();");
        }
    }

    public void loadMoreNarrowers() {
        if (selectedFullConcept == null || StringUtils.isBlank(selectedFullConcept.getIdentifier())) {
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();
        var additional = conceptFullReadService.loadMoreNarrowers(
                thesaurusId,
                selectedFullConcept.getIdentifier(),
                lang,
                narrowerOffset,
                userSession.isLoggedIn()
        );
        if (additional.isEmpty()) {
            haveMoreNarrowers = false;
            return;
        }
        conceptFullReadService.appendNarrowers(selectedFullConcept, additional);
        narrowerOffset = conceptFullReadService.nextNarrowerOffset(narrowerOffset);
        selectedConcept = conceptReadService.buildDetailFromFullConcept(
                selectedFullConcept,
                thesaurusId,
                selectedFullConcept.getIdentifier(),
                lang
        );
        haveMoreNarrowers = conceptFullReadService.hasMoreNarrowers(selectedFullConcept);
    }

    public void searchCorpusLinks() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            displayedCorpusLinks = Collections.emptyList();
            corpusSearched = true;
            return;
        }
        displayedCorpusLinks = conceptReadService.loadCorpusLinks(
                thesaurusContext.resolveThesaurusId(),
                conceptReadService.toCorpusSearchContext(selectedConcept.summary())
        );
        corpusSearched = true;
    }

    public boolean hasDisplayedCorpusLinks() {
        return !displayedCorpusLinks.isEmpty();
    }

    public boolean isCorpusNoResult() {
        return corpusSearched && !hasDisplayedCorpusLinks();
    }

    private void refreshActiveCorpusState() {
        haveActiveCorpus = conceptReadService.hasActiveCorpus(thesaurusContext.resolveThesaurusId());
    }

    public void openGroup(String groupId) {
        rightPanelMode = RightPanelMode.GROUP;
        activateRightTab(RightTabKey.COLLECTION);
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedFacet = null;
        selectedGroup = collectionReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                groupId,
                thesaurusContext.resolveWorkLanguage()
        ).orElse(null);
        if (!isSelectedNodeId(groupId)) {
            clearAllLeftTreeSelections();
        }
    }

    public void openFacet(String facetId) {
        rightPanelMode = RightPanelMode.FACET;
        activateRightTab(RightTabKey.CONCEPT);
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedGroup = null;
        selectedFacet = facetReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                facetId,
                thesaurusContext.resolveWorkLanguage()
        ).orElse(null);
        if (!isSelectedNodeId(facetId)) {
            clearAllLeftTreeSelections();
        }
    }

    public void focusGroup(String groupId) {
        activeLeftTreeMode = LeftTreeMode.COLLECTION;
        leftTabIndex = 2;
        openGroup(groupId);
    }

    public void focusFacet(String facetId) {
        activeLeftTreeMode = LeftTreeMode.CONCEPT;
        leftTabIndex = 0;
        openFacet(facetId);
    }

    public void openConceptHistory() {
        if (selectedConcept == null || StringUtils.isBlank(selectedConcept.preferredTermId())) {
            return;
        }
        conceptHistoryBean.load(
                thesaurusContext.resolveThesaurusId(),
                selectedConcept.summary().conceptId(),
                selectedConcept.preferredTermId()
        );
    }

    public boolean isLoggedIn() {
        return userSession.isLoggedIn();
    }

    public boolean isEditorialNoteVisible() {
        return showEditorialNote || userSession.isLoggedIn();
    }

    public boolean isHistoryNoteVisible() {
        return showHistoryNote || userSession.isLoggedIn();
    }

    public boolean isCustomRelationVisible() {
        return useCustomRelation;
    }

    /** DeepL activé dans les préférences du thésaurus (icônes notes). */
    public boolean isDeeplTranslationEnabled() {
        return useDeeplTranslation;
    }

    public boolean isStandardConceptType() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            return true;
        }
        String conceptType = selectedConcept.summary().conceptType();
        return StringUtils.isBlank(conceptType) || "concept".equalsIgnoreCase(conceptType);
    }

    public boolean isSpecificConceptType() {
        return !isStandardConceptType();
    }

    public boolean isConceptTypeBadgeVisible() {
        return isSpecificConceptType()
                && selectedConcept != null
                && !selectedConcept.isDeprecated()
                && StringUtils.isNotBlank(getConceptTypeLabel());
    }

    public String getConceptTypeLabel() {
        if (selectedConcept == null || selectedConcept.summary() == null || isStandardConceptType()) {
            return "";
        }
        return conceptTypeReadService.resolveLabel(
                selectedConcept.summary().conceptType(),
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public List<ConceptLabel> getPreferredTranslations() {
        if (selectedConcept == null || selectedConcept.translations() == null) {
            return Collections.emptyList();
        }
        return selectedConcept.translations().stream()
                .filter(ConceptLabel::preferred)
                .toList();
    }

    public String getFlagImageUrl(String codeFlag) {
        return ConceptFlagSupport.resolveFlagImageUrl(codeFlag);
    }

    public void openConceptInTranslationLanguage(String lang) {
        if (selectedConcept == null || selectedConcept.summary() == null || StringUtils.isBlank(lang)) {
            return;
        }
        thesaurusContext.changeWorkLanguage(lang);
        openConcept(selectedConcept.summary().conceptId(), true);
    }

    public boolean isAlignmentPanelVisible() {
        return isLoggedIn()
                || (selectedConcept != null && selectedConcept.hasAlignments());
    }

    public boolean isNotationVisible() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            return false;
        }
        return userSession.isManager()
                || StringUtils.isNotBlank(selectedConcept.summary().notation());
    }

    public void onNoteLanguageToggle() {
        refreshConceptDisplayData();
    }

    public void onSynonymLanguageToggle() {
        refreshConceptDisplayData();
    }

    public void countBranchConcepts() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            branchConceptCount = 0;
            return;
        }
        branchConceptCount = conceptReadService.countBranchConcepts(
                thesaurusContext.resolveThesaurusId(),
                selectedConcept.summary().conceptId()
        );
    }

    public void focusConceptInTree() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            return;
        }
        syncConceptTreeSelection(selectedConcept.summary().conceptId());
    }

    public boolean hasDisplayedNotes() {
        return !displayedNotes.isEmpty();
    }

    public boolean isHasDisplayedNotes() {
        return hasDisplayedNotes();
    }

    public boolean hasNotesOfType(String typeCode) {
        return displayedNotes.stream().anyMatch(note -> StringUtils.equals(note.typeCode(), typeCode));
    }

    public List<ConceptNote> notesOfType(String typeCode) {
        return displayedNotes.stream()
                .filter(note -> StringUtils.equals(note.typeCode(), typeCode))
                .toList();
    }

    public boolean hasFacetNotesOfType(String typeCode) {
        if (selectedFacet == null || selectedFacet.notes() == null) {
            return false;
        }
        return selectedFacet.notes().stream()
                .anyMatch(note -> StringUtils.equals(note.typeCode(), typeCode));
    }

    public List<ConceptNote> facetNotesOfType(String typeCode) {
        if (selectedFacet == null || selectedFacet.notes() == null) {
            return List.of();
        }
        return selectedFacet.notes().stream()
                .filter(note -> StringUtils.equals(note.typeCode(), typeCode))
                .toList();
    }

    public boolean hasDisplayedSynonyms() {
        return !getVisibleSynonymLabels().isEmpty() || !getHiddenSynonymLabels().isEmpty();
    }

    public boolean isHasDisplayedSynonyms() {
        return hasDisplayedSynonyms();
    }

    public List<ConceptLabel> getVisibleSynonymLabels() {
        return displayedSynonymLabels.stream()
                .filter(label -> !label.hidden())
                .toList();
    }

    public List<ConceptLabel> getHiddenSynonymLabels() {
        return displayedSynonymLabels.stream()
                .filter(ConceptLabel::hidden)
                .toList();
    }

    public boolean isGpsMapVisible() {
        return selectedConcept != null && !selectedConcept.gpsPoints().isEmpty();
    }

    public String getGpsMapScript() {
        if (!isGpsMapVisible()) {
            return "";
        }
        return ConceptGpsMapRenderer.renderMapScript("v2ConceptMap", selectedConcept.gpsPoints());
    }

    public String getPreferredLabelForCopy() {
        if (selectedConcept == null || selectedConcept.summary() == null) {
            return "";
        }
        return StringUtils.defaultString(selectedConcept.summary().preferredLabel());
    }

    public boolean isBranchGraphEnabled() {
        // Comme legacy Tree#isGraphNotVisible : besoin d'un concept sélectionné non-feuille
        if (selectedNode != null && selectedNode.getData() instanceof ConceptTreeNodeData data
                && StringUtils.isNotBlank(data.nodeId())
                && !data.isGroup()
                && !"facet".equals(data.nodeType())
                && !"root".equals(data.nodeType())) {
            return !selectedNode.isLeaf();
        }
        return conceptSelectionContext.isHasNarrowers();
    }

    public String getBranchGraphConceptId() {
        if (selectedNode != null && selectedNode.getData() instanceof ConceptTreeNodeData data
                && StringUtils.isNotBlank(data.nodeId())
                && !data.isGroup()
                && !"facet".equals(data.nodeType())
                && !"root".equals(data.nodeType())) {
            return data.nodeId();
        }
        if (selectedConcept != null && selectedConcept.summary() != null) {
            return selectedConcept.summary().conceptId();
        }
        return "";
    }

    private void syncConceptTreeSelection(String conceptId) {
        if (StringUtils.isBlank(conceptId) || selectedConcept == null) {
            return;
        }
        ensureTreeBuilt(LeftTreeMode.CONCEPT);
        if (conceptRoot == null) {
            return;
        }

        clearAllLeftTreeSelections();

        TreeNode found = null;
        for (List<String> pathIds : resolveAllConceptPathIds(conceptId)) {
            if (pathIds.isEmpty()) {
                continue;
            }
            found = expandPath(conceptRoot, pathIds, 0, LeftTreeMode.CONCEPT);
            if (found != null) {
                break;
            }
        }
        if (found == null) {
            found = expandPath(conceptRoot, List.of(conceptId), 0, LeftTreeMode.CONCEPT);
        }
        selectSingleTreeNode(found);
    }

    /**
     * Garantit qu'un seul nœud est sélectionné dans les arbres du panneau gauche.
     */
    private void selectSingleTreeNode(TreeNode node) {
        clearAllLeftTreeSelections();
        selectedNode = node;
        if (selectedNode != null) {
            selectedNode.setSelected(true);
        }
    }

    private boolean isSelectedNodeId(String nodeId) {
        if (StringUtils.isBlank(nodeId) || selectedNode == null) {
            return false;
        }
        if (!(selectedNode.getData() instanceof ConceptTreeNodeData data)) {
            return false;
        }
        return nodeId.equalsIgnoreCase(data.nodeId());
    }

    private void clearAllLeftTreeSelections() {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
            selectedNode = null;
        }
        clearTreeSelectionFlags(conceptRoot);
        clearTreeSelectionFlags(arbreRoot);
        clearTreeSelectionFlags(collectionRoot);
    }

    private void clearTreeSelectionFlags(TreeNode node) {
        if (node == null) {
            return;
        }
        if (node.isSelected()) {
            node.setSelected(false);
        }
        for (Object childObject : node.getChildren()) {
            if (childObject instanceof TreeNode child) {
                clearTreeSelectionFlags(child);
            }
        }
    }

    private List<List<String>> resolveAllConceptPathIds(String conceptId) {
        List<List<BreadcrumbStep>> paths = selectedConcept.breadcrumbPaths();
        if (paths == null || paths.isEmpty()) {
            return List.of(List.of(conceptId));
        }
        List<List<String>> result = new ArrayList<>();
        for (List<BreadcrumbStep> path : paths) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            result.add(path.stream().map(BreadcrumbStep::conceptId).toList());
        }
        if (result.isEmpty()) {
            result.add(List.of(conceptId));
        }
        return result;
    }

    private TreeNode expandPath(TreeNode parent, List<String> pathIds, int depth, LeftTreeMode mode) {
        if (depth >= pathIds.size()) {
            return parent;
        }
        String targetId = pathIds.get(depth);
        if (parent instanceof DefaultTreeNode defaultParent && !isRootNode(defaultParent)) {
            ensureChildrenLoaded(defaultParent, mode);
            defaultParent.setExpanded(true);
        } else if (parent instanceof DefaultTreeNode rootNode && isRootNode(rootNode)) {
            rootNode.setExpanded(true);
        }
        for (Object childObject : parent.getChildren()) {
            if (!(childObject instanceof TreeNode child)) {
                continue;
            }
            if (!(child.getData() instanceof ConceptTreeNodeData data) || data.isDummy()) {
                continue;
            }
            if (!targetId.equalsIgnoreCase(data.nodeId())) {
                continue;
            }
            boolean last = depth == pathIds.size() - 1;
            child.setExpanded(!last);
            if (last) {
                return child;
            }
            return expandPath(child, pathIds, depth + 1, mode);
        }
        return null;
    }

    private void ensureChildrenLoaded(DefaultTreeNode parentNode, LeftTreeMode mode) {
        if (parentNode.getChildCount() == 1 && isDummyChild(parentNode)) {
            parentNode.getChildren().clear();
            ConceptTreeNodeData parentData = (ConceptTreeNodeData) parentNode.getData();
            loadChildren(parentNode, parentData, mode);
        }
    }

    private boolean isRootNode(DefaultTreeNode node) {
        return node.getData() instanceof ConceptTreeNodeData data && "root".equals(data.nodeType());
    }

    private void ensureTreeBuilt(LeftTreeMode mode) {
        switch (mode) {
            case CONCEPT -> {
                if (conceptRoot == null) {
                    conceptRoot = buildTree(LeftTreeMode.CONCEPT);
                }
            }
            case COLLECTION -> {
                if (collectionRoot == null) {
                    collectionRoot = buildTree(LeftTreeMode.COLLECTION);
                }
            }
            case ARBRE -> {
                if (useConceptTree && arbreRoot == null) {
                    arbreRoot = buildTree(LeftTreeMode.ARBRE);
                }
            }
        }
    }

    private TreeNode buildTree(LeftTreeMode mode) {
        TreeNode treeRoot = new DefaultTreeNode(
                new ConceptTreeNodeData("root", getThesaurusTitle(), "", "root", true),
                null
        );
        List<ConceptTreeNodeData> roots = conceptReadService.loadRootNodes(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                mode,
                sortByNotation
        );
        if (mode == LeftTreeMode.CONCEPT && roots.size() >= 2000) {
            manySiblings = true;
        }
        for (ConceptTreeNodeData nodeData : roots) {
            DefaultTreeNode node = createTreeNode(nodeData, treeRoot);
            addLazyPlaceholderIfNeeded(node, nodeData);
        }
        treeRoot.setExpanded(true);
        return treeRoot;
    }

    private void loadChildren(DefaultTreeNode parentNode, ConceptTreeNodeData parentData, LeftTreeMode mode) {
        List<ConceptTreeNodeData> children = conceptReadService.loadChildNodes(
                parentData.nodeId(),
                parentData.nodeType(),
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                mode,
                sortByNotation
        );
        if (mode == LeftTreeMode.CONCEPT && children.size() >= 2000) {
            manySiblings = true;
        }
        for (ConceptTreeNodeData childData : children) {
            DefaultTreeNode childNode = createTreeNode(childData, parentNode);
            addLazyPlaceholderIfNeeded(childNode, childData);
        }
    }

    private DefaultTreeNode createTreeNode(ConceptTreeNodeData data, TreeNode parent) {
        return new DefaultTreeNode(data.nodeType(), data, parent);
    }

    private void addLazyPlaceholderIfNeeded(DefaultTreeNode node, ConceptTreeNodeData data) {
        if (data.hasChildren()) {
            new DefaultTreeNode("default", ConceptTreeNodeData.dummy(), node);
        }
    }

    private boolean isDummyChild(DefaultTreeNode node) {
        if (node.getChildCount() != 1) {
            return false;
        }
        Object data = ((TreeNode) node.getChildren().get(0)).getData();
        return data instanceof ConceptTreeNodeData treeData && treeData.isDummy();
    }

    private LeftTreeMode resolveTreeModeFromComponent(FacesEvent event) {
        Object mode = event.getComponent().getAttributes().get("treeMode");
        if (mode instanceof LeftTreeMode leftTreeMode) {
            return leftTreeMode;
        }
        if (mode instanceof String modeName) {
            return LeftTreeMode.valueOf(modeName);
        }
        return activeLeftTreeMode;
    }

    private LeftTreeMode resolveTreeMode(String tabId) {
        return switch (tabId) {
            case "viewTabList" -> LeftTreeMode.INDEX;
            case "viewTabGroups" -> LeftTreeMode.COLLECTION;
            case "viewTabConceptTree" -> LeftTreeMode.ARBRE;
            default -> LeftTreeMode.CONCEPT;
        };
    }

    private void loadThesaurusHome() {
        thesaurusHome = thesaurusHomeReadService.loadOverview(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                getThesaurusTitle()
        );
    }

    /** Recharge la page d'accueil après édition HTML. */
    public void reloadThesaurusHomeAfterEdit() {
        loadThesaurusHome();
    }

    private void refreshConceptDisplayData() {
        if (selectedFullConcept == null) {
            displayedNotes = Collections.emptyList();
            displayedSynonymLabels = Collections.emptyList();
            branchConceptCount = 0;
            return;
        }
        String lang = thesaurusContext.resolveWorkLanguage();
        displayedNotes = ConceptMapper.mapNotes(selectedFullConcept, lang, showAllNoteLanguages);
        displayedSynonymLabels = ConceptMapper.mapSynonymLabels(
                selectedFullConcept,
                lang,
                showAllSynonymLanguages
        );
        branchConceptCount = 0;
    }

    private void loadConsultationPreferences() {
        ThesaurusPreferences preferences = null;
        try {
            preferences = thesaurusPreferenceService.loadPreferencesOrNull(
                    thesaurusContext.resolveThesaurusId(),
                    thesaurusContext.resolveWorkLanguage()
            );
        } catch (RuntimeException ignored) {
            preferences = null;
        }
        if (preferences != null) {
            breadcrumbEnabled = preferences.breadcrumb();
            useConceptTree = preferences.useConceptTree();
            displayUserName = preferences.displayUserName();
            kohaLink = preferences.kohaLink();
            showHistoryNote = preferences.showHistoryNote();
            showEditorialNote = preferences.showEditorialNote();
            useCustomRelation = preferences.useCustomRelation();
            autoExpandTree = preferences.autoExpandTree();
            treeCacheEnabled = preferences.treeCache();
            sortByNotation = preferences.sortByNotation();
            suggestionEnabled = preferences.suggestion();
            useDeeplTranslation = preferences.useDeeplTranslation();
        } else {
            breadcrumbEnabled = true;
            useConceptTree = true;
            displayUserName = true;
            kohaLink = false;
            showHistoryNote = false;
            showEditorialNote = false;
            useCustomRelation = false;
            autoExpandTree = true;
            treeCacheEnabled = false;
            sortByNotation = false;
            suggestionEnabled = false;
            useDeeplTranslation = false;
        }
        manySiblings = false;
    }

    private void resetState() {
        conceptRoot = null;
        collectionRoot = null;
        arbreRoot = null;
        selectedNode = null;
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedGroup = null;
        selectedFacet = null;
        thesaurusHome = null;
        indexResults = Collections.emptyList();
        indexSelected = null;
        indexQuery = null;
        indexPermuted = false;
        indexWithAltLabel = false;
        activeLeftTreeMode = LeftTreeMode.CONCEPT;
        rightPanelMode = RightPanelMode.HOME;
        leftTabIndex = 0;
        propositionRubriqueVisible = false;
        branchConceptCount = 0;
        displayedNotes = Collections.emptyList();
        displayedSynonymLabels = Collections.emptyList();
        displayedCorpusLinks = Collections.emptyList();
        corpusSearched = false;
        haveActiveCorpus = false;
        activateRightTab(RightTabKey.CONCEPT);
    }
}
