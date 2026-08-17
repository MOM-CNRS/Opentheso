package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
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
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.v2.setting.ui.CorpusEditor;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
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
 * Façade preview : lit / écrit {@link ThesaurusContext}.
 * Tant qu'aucun thésaurus n'est en session, sélectionne temporairement {@code th17}.
 */
@Named("v2PreviewThesaurusBean")
@ViewScoped
@RequiredArgsConstructor
public class PreviewThesaurusBean implements Serializable {

    public static final String THESAURUS_ID = "th17";

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusHomeWriteService thesaurusHomeWriteService;
    private final ConceptReadService conceptReadService;
    private final UserSession userSession;
    private final RightsService rightsService;
    private final V2LocaleBean v2LocaleBean;
    private final ThesaurusCorpusService thesaurusCorpusService;
    private final ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;

    private Integer conceptCount;
    private Integer candidatePendingCount;
    private Integer candidateRejectedCount;
    private Integer languageCount;
    private Integer collectionCount;
    private Integer conceptsWithoutDefinitionCount;
    private Integer maxTreeDepth;
    private List<ThesaurusLanguage> languages;
    private String selectedLang;
    private boolean sessionReady;
    private String homePageHtml;
    private boolean homePageHtmlLoaded;
    private PreferenceEditor preference;
    private boolean preferenceLoaded;
    private List<PreviewTreeNode> treeRoots;

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
    private List<ThesaurusCorpus> corpusList = Collections.emptyList();
    private boolean corpusLoaded;
    private String corpusLoadedForThesaurus;
    @Getter
    @Setter
    private CorpusEditor corpusEditor = CorpusEditor.empty();
    @Getter
    private String editingCorpusName;
    @Getter
    private boolean corpusDialogOpen;
    @Getter
    private String corpusDialogMode;
    @Getter
    private String corpusMessage;
    @Getter
    private boolean corpusError;
    @Getter
    private String preferenceSaveMessage;
    @Getter
    private boolean preferenceSaveError;

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

    public String getConceptCountFormatted() {
        return formatCount(getConceptCount());
    }

    public int getCandidatePendingCount() {
        if (candidatePendingCount == null) {
            candidatePendingCount = thesaurusHomeQueryRepository.countCandidatesByStatus(
                    getId(), CandidatStatusCode.PENDING);
        }
        return candidatePendingCount;
    }

    public int getCandidateRejectedCount() {
        if (candidateRejectedCount == null) {
            candidateRejectedCount = thesaurusHomeQueryRepository.countCandidatesByStatus(
                    getId(), CandidatStatusCode.REJECTED);
        }
        return candidateRejectedCount;
    }

    public int getCandidateCount() {
        return getCandidatePendingCount() + getCandidateRejectedCount();
    }

    public String getCandidateCountFormatted() {
        return formatCount(getCandidateCount());
    }

    public int getLanguageCount() {
        if (languageCount == null) {
            languageCount = thesaurusHomeQueryRepository.countDefinedLanguages(getId());
        }
        return languageCount;
    }

    public String getLanguageCountFormatted() {
        return formatCount(getLanguageCount());
    }

    public int getCollectionCount() {
        if (collectionCount == null) {
            collectionCount = thesaurusHomeQueryRepository.countCollections(getId());
        }
        return collectionCount;
    }

    public String getCollectionCountFormatted() {
        return formatCount(getCollectionCount());
    }

    public int getConceptsWithoutDefinitionCount() {
        if (conceptsWithoutDefinitionCount == null) {
            conceptsWithoutDefinitionCount = thesaurusHomeQueryRepository.countConceptsWithoutDefinition(getId());
        }
        return conceptsWithoutDefinitionCount;
    }

    public String getConceptsWithoutDefinitionCountFormatted() {
        return formatCount(getConceptsWithoutDefinitionCount());
    }

    public int getMaxTreeDepth() {
        if (maxTreeDepth == null) {
            maxTreeDepth = thesaurusHomeQueryRepository.findMaxTreeDepth(getId());
        }
        return maxTreeDepth;
    }

    public String getMaxTreeDepthFormatted() {
        return formatCount(getMaxTreeDepth());
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
        if (editing) {
            homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        }
        if (detailRequested && StringUtils.isNotBlank(selectedId)) {
            openTreeNode(selectedId, selectedKind);
        }
    }

    public List<PreviewTreeNode> getTreeRoots() {
        ensureTreeLoaded();
        return treeRoots;
    }

    public List<PreviewTreeNode> getVisibleTreeNodes() {
        List<PreviewTreeNode> visible = new ArrayList<>();
        appendVisibleNodes(getTreeRoots(), visible);
        return visible;
    }

    public boolean isTreeEmpty() {
        return getTreeRoots().isEmpty();
    }

    public void toggleTreeNode(String path) {
        PreviewTreeNode node = findTreeNodeByPath(getTreeRoots(), path);
        if (node == null || !node.isHasChildren()) {
            return;
        }
        node.setExpanded(!node.isExpanded());
        if (node.isExpanded() && !node.isChildrenLoaded()) {
            loadTreeChildren(node);
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

    public PreferenceEditor getPreference() {
        ensurePreferencesLoaded();
        return preference;
    }

    /**
     * Charge les préférences générales depuis la base, comme l'écran V2
     * {@code v2PreferenceSettingsBean.load()}.
     */
    public void loadGeneralPreferences() {
        ensureSessionThesaurus();
        preferenceLoaded = false;
        ensurePreferencesLoaded();
        corpusLoaded = false;
        refreshCorpusList();
    }

    public List<ThesaurusCorpus> getCorpusList() {
        ensureCorpusListLoaded();
        return corpusList != null ? corpusList : Collections.emptyList();
    }

    /**
     * Enregistre les préférences preview, comme {@code v2PreferenceSettingsBean.save()}.
     */
    public void savePreferences() {
        preferenceSaveMessage = null;
        preferenceSaveError = false;
        if (!isCanEdit()) {
            preferenceSaveError = true;
            preferenceSaveMessage = "Action non autorisée";
            return;
        }
        PreferenceEditor editor = getPreference();
        if (editor == null) {
            preferenceSaveError = true;
            preferenceSaveMessage = "Aucune préférence à enregistrer.";
            return;
        }
        String thesaurusId = getId();
        if (thesaurusPreferenceService.isPreferredNameExist(thesaurusId, editor.getPreferredName())) {
            preferenceSaveError = true;
            preferenceSaveMessage = "PreferredName existe déjà, veuillez en choisir un autre ! ";
            return;
        }
        if (editor.isUseOpenArk()) {
            String openArkError = validateOpenArkEditor(editor);
            if (openArkError != null) {
                preferenceSaveError = true;
                preferenceSaveMessage = openArkError;
                return;
            }
        }
        syncIdentifierServerType(editor);
        try {
            ThesaurusPreferences saved = thesaurusPreferenceService.savePreferences(
                    thesaurusId,
                    editor.toModel(thesaurusId),
                    editor.getNewPassArk(),
                    editor.getNewPassHandle(),
                    editor.getNewDeeplApiKey(),
                    editor.getNewApiKeyOpenArk(),
                    preferenceWorkLanguage()
            );
            preference = PreferenceEditor.from(saved);
            preference.setNewPassArk(null);
            preference.setNewPassHandle(null);
            preference.setNewDeeplApiKey(null);
            preference.setNewApiKeyOpenArk(null);
            preferenceLoaded = true;
            if (StringUtils.isNotBlank(saved.sourceLang())) {
                thesaurusSearchLanguageSync.applyAfterSourceLanguageChange(thesaurusId, saved.sourceLang());
            }
            preferenceSaveMessage = "Préférences enregistrées avec succès";
        } catch (SettingAccessDeniedException | InvalidSettingDataException e) {
            preferenceSaveError = true;
            preferenceSaveMessage = e.getMessage();
        }
    }

    /**
     * Un seul serveur d'identifiants à la fois (Ark, Ark local, Handle, OpenArk).
     * Ne persiste pas : l'enregistrement se fait via {@link #savePreferences()}.
     */
    public void selectIdentifierServer(AjaxBehaviorEvent event) {
        PreferenceEditor editor = getPreference();
        if (editor == null || event == null || event.getComponent() == null) {
            return;
        }
        String id = event.getComponent().getId();
        boolean enabled = switch (id) {
            case "previewUseArk" -> editor.isUseArk();
            case "previewUseArkLocal" -> editor.isUseArkLocal();
            case "previewUseHandle" -> editor.isUseHandle();
            case "previewUseOpenArk" -> editor.isUseOpenArk();
            default -> false;
        };
        if (!enabled) {
            syncIdentifierServerType(editor);
            return;
        }
        editor.setUseArk("previewUseArk".equals(id));
        editor.setUseArkLocal("previewUseArkLocal".equals(id));
        editor.setUseHandle("previewUseHandle".equals(id));
        editor.setUseOpenArk("previewUseOpenArk".equals(id));
        syncIdentifierServerType(editor);
    }

    public void prepareCreateCorpus() {
        if (!isCanEdit()) {
            corpusError = true;
            corpusMessage = "Action non autorisée";
            return;
        }
        corpusEditor = CorpusEditor.empty();
        editingCorpusName = null;
        corpusDialogMode = "create";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void prepareEditCorpus() {
        prepareEditCorpus(findCorpus(requestCorpusTarget()));
    }

    public void prepareEditCorpus(ThesaurusCorpus corpus) {
        if (!isCanEdit() || corpus == null) {
            return;
        }
        corpusEditor = CorpusEditor.from(corpus);
        editingCorpusName = corpus.corpusName();
        corpusDialogMode = "edit";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void prepareDeleteCorpus() {
        prepareDeleteCorpus(findCorpus(requestCorpusTarget()));
    }

    public void prepareDeleteCorpus(ThesaurusCorpus corpus) {
        if (!isCanEdit() || corpus == null) {
            return;
        }
        corpusEditor = CorpusEditor.from(corpus);
        editingCorpusName = corpus.corpusName();
        corpusDialogMode = "delete";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void cancelCorpusDialog() {
        closeCorpusDialog();
        if (corpusError) {
            corpusMessage = null;
            corpusError = false;
        }
    }

    public void createCorpus() {
        if (!isCanEdit()) {
            denyCorpusAction();
            return;
        }
        try {
            thesaurusCorpusService.createCorpus(getId(), corpusEditor.toModel());
            closeCorpusDialog();
            refreshCorpusList();
            corpusError = false;
            corpusMessage = "Corpus créé avec succès";
        } catch (InvalidSettingDataException e) {
            corpusError = true;
            corpusMessage = e.getMessage();
        }
    }

    public void updateCorpus() {
        if (!isCanEdit()) {
            denyCorpusAction();
            return;
        }
        if (editingCorpusName == null) {
            return;
        }
        try {
            thesaurusCorpusService.updateCorpus(getId(), editingCorpusName, corpusEditor.toModel());
            closeCorpusDialog();
            refreshCorpusList();
            corpusError = false;
            corpusMessage = "Corpus modifié avec succès";
        } catch (InvalidSettingDataException e) {
            corpusError = true;
            corpusMessage = e.getMessage();
        }
    }

    public void deleteCorpus() {
        if (!isCanEdit()) {
            denyCorpusAction();
            return;
        }
        if (editingCorpusName == null) {
            return;
        }
        try {
            thesaurusCorpusService.deleteCorpus(getId(), editingCorpusName);
            closeCorpusDialog();
            refreshCorpusList();
            corpusError = false;
            corpusMessage = "Corpus supprimé avec succès";
        } catch (InvalidSettingDataException e) {
            corpusError = true;
            corpusMessage = e.getMessage();
        }
    }

    public boolean isCorpusFormDialog() {
        return corpusDialogOpen && ("create".equals(corpusDialogMode) || "edit".equals(corpusDialogMode));
    }

    public boolean isCorpusCreateDialog() {
        return corpusDialogOpen && "create".equals(corpusDialogMode);
    }

    public boolean isCorpusDeleteDialog() {
        return corpusDialogOpen && "delete".equals(corpusDialogMode);
    }

    public Integer getIdentifierAlphanumeric() {
        return 1;
    }

    public Integer getIdentifierNumeric() {
        return 2;
    }

    public String getPreferencePermalink() {
        PreferenceEditor editor = getPreference();
        if (editor == null || StringUtils.isBlank(editor.getPreferredName())) {
            return "";
        }
        return "/api/theso/" + editor.getPreferredName();
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
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
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
        List<ConceptTreeNodeData> roots = conceptReadService.loadPreviewRootNodes(getId(), getSelectedLang());
        treeRoots = new ArrayList<>();
        if (roots == null) {
            return;
        }
        for (ConceptTreeNodeData data : roots) {
            treeRoots.add(toTreeNode(data, 0, ""));
        }
        applyCandidateMeta(treeRoots);
    }

    private void loadTreeChildren(PreviewTreeNode parent) {
        List<ConceptTreeNodeData> children = conceptReadService.loadPreviewChildNodes(
                parent.getId(),
                parent.getNodeType(),
                getId(),
                getSelectedLang()
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

    private PreviewTreeNode toTreeNode(ConceptTreeNodeData data, int depth, String parentPath) {
        PreviewTreeNode node = new PreviewTreeNode();
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

    private void applyCandidateMeta(List<PreviewTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        List<String> ids = nodes.stream()
                .filter(PreviewTreeNode::isCandidate)
                .map(PreviewTreeNode::getId)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        for (Object[] row : conceptReadService.loadCandidateMeta(getId(), ids)) {
            String id = row[0] == null ? "" : row[0].toString();
            String by = row[1] == null ? "" : row[1].toString();
            String on = row[2] == null ? "" : row[2].toString();
            for (PreviewTreeNode node : nodes) {
                if (id.equals(node.getId())) {
                    node.setCandidateBy(by);
                    node.setCandidateOn(on);
                }
            }
        }
    }

    private void appendVisibleNodes(List<PreviewTreeNode> nodes, List<PreviewTreeNode> out) {
        if (nodes == null) {
            return;
        }
        for (PreviewTreeNode node : nodes) {
            out.add(node);
            if (node.isExpanded() && !node.getChildren().isEmpty()) {
                appendVisibleNodes(node.getChildren(), out);
            }
        }
    }

    private PreviewTreeNode findTreeNodeByPath(List<PreviewTreeNode> nodes, String path) {
        if (nodes == null || StringUtils.isBlank(path)) {
            return null;
        }
        for (PreviewTreeNode node : nodes) {
            if (path.equals(node.getPath())) {
                return node;
            }
            PreviewTreeNode found = findTreeNodeByPath(node.getChildren(), path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void ensureCorpusListLoaded() {
        String thesaurusId = getId();
        if (corpusLoaded && StringUtils.equals(corpusLoadedForThesaurus, thesaurusId) && corpusList != null) {
            return;
        }
        refreshCorpusList();
    }

    private void refreshCorpusList() {
        String thesaurusId = getId();
        corpusLoaded = true;
        corpusLoadedForThesaurus = thesaurusId;
        if (StringUtils.isBlank(thesaurusId)) {
            corpusList = Collections.emptyList();
            return;
        }
        List<ThesaurusCorpus> loaded = thesaurusCorpusService.listCorpus(thesaurusId);
        corpusList = loaded != null ? loaded : Collections.emptyList();
    }

    private String requestCorpusTarget() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }
        return context.getExternalContext().getRequestParameterMap().get("previewCorpusTarget");
    }

    private ThesaurusCorpus findCorpus(String corpusName) {
        if (StringUtils.isBlank(corpusName)) {
            return null;
        }
        ThesaurusCorpus found = matchCorpus(corpusName);
        if (found != null) {
            return found;
        }
        refreshCorpusList();
        return matchCorpus(corpusName);
    }

    private ThesaurusCorpus matchCorpus(String corpusName) {
        if (corpusList == null) {
            return null;
        }
        return corpusList.stream()
                .filter(item -> corpusName.equals(item.corpusName()))
                .findFirst()
                .orElse(null);
    }

    private void closeCorpusDialog() {
        corpusDialogOpen = false;
        corpusDialogMode = null;
        corpusEditor = CorpusEditor.empty();
        editingCorpusName = null;
    }

    private void denyCorpusAction() {
        corpusError = true;
        corpusMessage = "Action non autorisée";
    }

    private void syncIdentifierServerType(PreferenceEditor editor) {
        if (editor.isUseOpenArk()) {
            editor.setIdentifierServerType(IdentifierServerType.OPENARK);
        } else if (editor.isUseHandle()) {
            editor.setIdentifierServerType(IdentifierServerType.HANDLE);
        } else if (editor.isUseArkLocal()) {
            editor.setIdentifierServerType(IdentifierServerType.ARK_LOCAL);
        } else if (editor.isUseArk()) {
            editor.setIdentifierServerType(IdentifierServerType.ARK);
        } else {
            editor.setIdentifierServerType(IdentifierServerType.NONE);
        }
    }

    private String validateOpenArkEditor(PreferenceEditor editor) {
        String server = StringUtils.trimToEmpty(editor.getServerOpenArk());
        if (StringUtils.isBlank(server)) {
            return "OpenArk : URL du serveur obligatoire (ex. http://localhost:8080/api)";
        }
        String serverLower = server.toLowerCase();
        if (!serverLower.startsWith("http://") && !serverLower.startsWith("https://")) {
            return "OpenArk : l'URL du serveur doit commencer par http:// ou https://";
        }
        String naan = StringUtils.trimToEmpty(editor.getNaanOpenArk());
        if (StringUtils.isBlank(naan)) {
            return "OpenArk : NAAN obligatoire";
        }
        try {
            Integer.parseInt(naan);
        } catch (NumberFormatException ex) {
            return "OpenArk : NAAN invalide (nombre attendu, ex. 66666)";
        }
        if (StringUtils.isBlank(editor.getPrefixOpenArk())) {
            return "OpenArk : préfixe Ark obligatoire";
        }
        ThesaurusPreferences current = thesaurusPreferenceService.loadPreferencesOrNull(
                getId(),
                preferenceWorkLanguage()
        );
        boolean hasExistingKey = current != null && StringUtils.isNotBlank(current.apiKeyOpenArk());
        if (!hasExistingKey && StringUtils.isBlank(editor.getNewApiKeyOpenArk())) {
            return "OpenArk : clé API obligatoire";
        }
        return null;
    }

    private void ensurePreferencesLoaded() {
        if (preferenceLoaded && preference != null) {
            return;
        }
        String thesaurusId = getId();
        if (StringUtils.isBlank(thesaurusId)) {
            preference = new PreferenceEditor();
            return;
        }
        String workLang = preferenceWorkLanguage();
        ThesaurusPreferences prefs = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLang);
        if (prefs == null) {
            preference = new PreferenceEditor();
            preference.setLanguages(new ArrayList<>(loadPreferenceLanguages(thesaurusId, workLang)));
            preferenceLoaded = true;
            return;
        }
        preference = PreferenceEditor.from(prefs);
        if (preference.getLanguages() == null || preference.getLanguages().isEmpty()) {
            preference.setLanguages(new ArrayList<>(loadPreferenceLanguages(thesaurusId, workLang)));
        }
        preferenceLoaded = true;
    }

    private String preferenceWorkLanguage() {
        return StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
    }

    private List<ThesaurusLanguage> loadPreferenceLanguages(String thesaurusId, String workLang) {
        List<ThesaurusLanguage> loaded = thesaurusPreferenceService.loadUsedLanguages(thesaurusId, workLang);
        return loaded != null ? loaded : Collections.emptyList();
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
