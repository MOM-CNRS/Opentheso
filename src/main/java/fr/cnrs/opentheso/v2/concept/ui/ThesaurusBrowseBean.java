package fr.cnrs.opentheso.v2.concept.ui;

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
import fr.cnrs.opentheso.v2.concept.support.ConceptGpsMapRenderer;
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

import java.io.Serializable;
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
    private int rightTabIndex;

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
    private boolean suggestionEnabled;
    private boolean showAllNoteLanguages = true;
    private boolean showAllSynonymLanguages = true;
    private boolean homePagePlainTextView;
    private int branchConceptCount;
    private List<ConceptNote> displayedNotes = Collections.emptyList();
    private List<ConceptLabel> displayedSynonymLabels = Collections.emptyList();
    private int narrowerOffset;
    private boolean haveMoreNarrowers;

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (StringUtils.isBlank(conceptIdFromUri) && StringUtils.isNotBlank(thesaurusContext.getIdConceptFromUri())) {
            conceptIdFromUri = thesaurusContext.getIdConceptFromUri();
            thesaurusContext.setIdConceptFromUri(null);
        }
        if (StringUtils.isBlank(groupIdFromUri) && StringUtils.isNotBlank(thesaurusContext.getIdGroupFromUri())) {
            groupIdFromUri = thesaurusContext.getIdGroupFromUri();
            thesaurusContext.setIdGroupFromUri(null);
        }
        resetState();
        if (!isScreenAvailable()) {
            return;
        }
        loadConsultationPreferences();
        refreshActiveCorpusState();
        ensureTreeBuilt(activeLeftTreeMode);
        loadThesaurusHome();
        if (StringUtils.isNotBlank(conceptIdFromUri)) {
            openConcept(conceptIdFromUri.trim());
            conceptIdFromUri = null;
        } else if (StringUtils.isNotBlank(groupIdFromUri)) {
            focusGroup(groupIdFromUri.trim());
            groupIdFromUri = null;
        } else if (StringUtils.isNotBlank(facetIdFromUri)) {
            openFacet(facetIdFromUri.trim());
            facetIdFromUri = null;
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
        selectedNode = null;
        ensureTreeBuilt(activeLeftTreeMode);
        openThesaurusHome();
    }

    public void onRightTabChange(TabChangeEvent<?> event) {
        if (event.getTab() == null) {
            return;
        }
        rightTabIndex = "viewTabAlignement".equals(event.getTab().getId()) ? 1 : 0;
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
        selectedNode = event.getTreeNode();
        ConceptTreeNodeData data = (ConceptTreeNodeData) event.getTreeNode().getData();
        if (data == null || data.isDummy() || "root".equals(data.nodeType())) {
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
        if (rightTabIndex != 1) {
            rightTabIndex = 0;
        }
        if (!PrimeFaces.current().isAjaxRequest()) {
            return;
        }
        PrimeFaces.current().ajax().update(
                "indexTitle",
                "containerIndex:rightTab",
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
        rightTabIndex = 0;
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedGroup = null;
        selectedFacet = null;
        selectedNode = null;
        branchConceptCount = 0;
        displayedNotes = Collections.emptyList();
        displayedSynonymLabels = Collections.emptyList();
        displayedCorpusLinks = Collections.emptyList();
        corpusSearched = false;
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
    }

    public void invalidateCollectionTree() {
        collectionRoot = null;
    }

    @Override
    public void afterConceptDeleted(String fallbackConceptId) {
        invalidateConceptTree();
        selectedNode = null;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(fallbackConceptId)) {
            openConcept(fallbackConceptId);
        } else {
            openThesaurusHome();
        }
    }

    public boolean isPropositionAuthorized() {
        return suggestionEnabled;
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
        if (rightTabIndex != 1) {
            rightTabIndex = 0;
        }
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
        if (syncTree && autoExpandTree) {
            syncConceptTreeSelection(conceptId);
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
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedFacet = null;
        selectedGroup = collectionReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                groupId,
                thesaurusContext.resolveWorkLanguage()
        ).orElse(null);
        selectedNode = null;
    }

    public void openFacet(String facetId) {
        rightPanelMode = RightPanelMode.FACET;
        conceptSelectionContext.clear();
        selectedConcept = null;
        selectedFullConcept = null;
        selectedGroup = null;
        selectedFacet = facetReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                facetId,
                thesaurusContext.resolveWorkLanguage()
        ).orElse(null);
        selectedNode = null;
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
        return StringUtils.isNotBlank(getBranchGraphConceptId());
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
        if (conceptRoot == null || selectedConcept == null) {
            return;
        }
        List<String> pathIds = resolveConceptPathIds(conceptId);
        if (pathIds.isEmpty()) {
            return;
        }
        TreeNode found = expandPath(conceptRoot, pathIds, 0, LeftTreeMode.CONCEPT);
        if (found != null) {
            selectedNode = found;
        }
    }

    private List<String> resolveConceptPathIds(String conceptId) {
        List<List<BreadcrumbStep>> paths = selectedConcept.breadcrumbPaths();
        if (paths != null && !paths.isEmpty() && !paths.get(0).isEmpty()) {
            return paths.get(0).stream().map(BreadcrumbStep::conceptId).toList();
        }
        return List.of(conceptId);
    }

    private TreeNode expandPath(TreeNode parent, List<String> pathIds, int depth, LeftTreeMode mode) {
        if (depth >= pathIds.size()) {
            return parent;
        }
        String targetId = pathIds.get(depth);
        if (parent instanceof DefaultTreeNode defaultParent && !isRootNode(defaultParent)) {
            ensureChildrenLoaded(defaultParent, mode);
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
            if (!targetId.equals(data.nodeId())) {
                continue;
            }
            child.setExpanded(depth < pathIds.size() - 1);
            if (depth == pathIds.size() - 1) {
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
        for (ConceptTreeNodeData nodeData : conceptReadService.loadRootNodes(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                mode
        )) {
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
                mode
        );
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
        homePagePlainTextView = false;
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
            suggestionEnabled = preferences.suggestion();
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
            suggestionEnabled = false;
        }
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
        branchConceptCount = 0;
        displayedNotes = Collections.emptyList();
        displayedSynonymLabels = Collections.emptyList();
        displayedCorpusLinks = Collections.emptyList();
        corpusSearched = false;
        haveActiveCorpus = false;
        homePagePlainTextView = false;
        rightTabIndex = 0;
    }
}
