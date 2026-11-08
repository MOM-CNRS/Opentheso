package fr.cnrs.opentheso.concept.ui;

import fr.cnrs.opentheso.config.JsfPrettyUrlFilter;
import fr.cnrs.opentheso.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.concept.model.ConceptDetail;
import fr.cnrs.opentheso.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.concept.model.ConceptIdentifiers;
import fr.cnrs.opentheso.concept.model.ConceptLabel;
import fr.cnrs.opentheso.concept.model.ConceptNote;
import fr.cnrs.opentheso.concept.model.ConceptRelation;
import fr.cnrs.opentheso.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.concept.model.ThesaurusHomeOverview;
import fr.cnrs.opentheso.concept.model.ThesaurusMetadataItem;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.stats.service.ConsultationViewLogger;
import fr.cnrs.opentheso.concept.service.ConceptReadService;
import fr.cnrs.opentheso.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.concept.support.ConceptQrSvgSupport;
import fr.cnrs.opentheso.concept.service.ThesaurusHomeReadService;
import fr.cnrs.opentheso.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.toolbox.policy.ToolboxAccessPolicy;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import fr.cnrs.opentheso.concept.model.ConceptTreeNodeKinds;
import fr.cnrs.opentheso.proposition.ui.PropositionBean;

/**
 * Façade de l'accueil thésaurus : lit / écrit {@link ThesaurusContext}.
 */
@Named("thesaurusViewBean")
@ViewScoped
@RequiredArgsConstructor
public class ThesaurusViewBean implements Serializable {

    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusHomeReadService thesaurusHomeReadService;
    private final transient ThesaurusPreferenceService thesaurusPreferenceService;
    private final transient ThesaurusHomeWriteService thesaurusHomeWriteService;
    private final transient ConceptReadService conceptReadService;
    private final transient UserSession userSession;
    private final transient RightsService rightsService;
    private final transient LocaleBean localeBean;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;
    private final transient ConsultationShellBean consultationShellBean;
    private final transient PropositionBean propositionBean;
    private final transient ConceptDcTermRepository conceptDcTermRepository;
    private final transient ConsultationViewLogger consultationViewLogger;
    private final transient ObjectProvider<ThesaurusBrowseBean> thesaurusBrowseBean;
    private ThesaurusHomeOverview homeOverview;
    private List<ThesaurusLanguage> languages;
    private String selectedLang;
    private boolean sessionReady;
    private List<ThesaurusTreeNode> treeRoots;
    private Boolean canEdit;
    private Boolean conceptActionsVisible;
    private Boolean breadcrumbEnabled;
    private Boolean sortByNotation;
    private Boolean customRelationVisible;
    private Boolean suggestionEnabled;
    @Getter
    private boolean showAllNoteLanguages;
    @Getter
    private List<ThesaurusMetadataItem> conceptMetadata = List.of();

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
    @Setter
    private String ficheEditCard;
    @Getter
    private ConceptDetail selectedConcept;
    @Getter
    private FacetDetailOverview selectedFacet;
    @Getter
    private String candidateBy = "";
    @Getter
    private String candidateOn = "";
    @Getter
    private boolean candidateRejected;
    @Getter
    private int conceptRevision;
    @Getter
    private boolean detailRequested;
    private boolean openedConceptFromRequest;
    @Getter
    private int branchConceptCount;
    @Getter
    private boolean corpusSearched;
    @Getter
    private List<ConceptCorpusLinkItem> displayedCorpusLinks = Collections.emptyList();

    public void ensureSessionThesaurus() {
        sessionReady = true;
        applyThesaurusIdFromRequest();
    }

    /**
     * Applique {@code ?idt=} même si les {@code f:viewAction} du template n'ont pas tourné.
     */
    private void applyThesaurusIdFromRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        String idt = StringUtils.trimToNull(requestParameter("idt"));
        if (idt == null) {
            return;
        }
        String current = thesaurusContext.resolveThesaurusId();
        if (idt.equalsIgnoreCase(StringUtils.defaultString(current))) {
            return;
        }
        if (!consultationShellBean.trySelectThesaurusForConsultation(idt)) {
            return;
        }
        conceptSelectionContext.clear();
        invalidateHomeOverview();
        invalidateTree();
        homeHtml = null;
        languages = null;
        selectedLang = null;
        canEdit = null;
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
        return home().conceptCount();
    }

    public String getConceptCountLabel() {
        int count = getConceptCount();
        return formatCount(count) + (count > 1 ? " concepts" : " concept");
    }

    public String getProjectName() {
        return StringUtils.defaultString(home().projectName());
    }

    public boolean isProjectNamePresent() {
        return StringUtils.isNotBlank(getProjectName());
    }

    public String getLastModifiedLabel() {
        return StringUtils.firstNonBlank(home().lastModifiedRelative(), home().lastModifiedDate());
    }

    public String getLastModifiedExact() {
        return StringUtils.defaultString(home().lastModifiedDate());
    }

    public boolean isLastModifiedPresent() {
        return StringUtils.isNotBlank(getLastModifiedExact()) || StringUtils.isNotBlank(getLastModifiedLabel());
    }

    public String getPermalinkUrl() {
        return StringUtils.defaultString(home().permalinkUrl());
    }

    public String getPermalinkLabel() {
        return StringUtils.defaultString(home().permalinkLabel());
    }

    public boolean isPermalinkPresent() {
        return StringUtils.isNotBlank(getPermalinkUrl());
    }

    public boolean isIdentityPresent() {
        return isProjectNamePresent()
                || isLastModifiedPresent()
                || isPermalinkPresent()
                || isLastModifiedConceptsPresent()
                || isMetadataPresent();
    }

    /** Carte d'identité : visible dès qu'un thésaurus est sélectionné (visiteur et connecté). */
    public boolean isIdentityCardVisible() {
        return StringUtils.isNotBlank(getId());
    }

    public List<ConceptLinkItem> getLastModifiedConcepts() {
        List<ConceptLinkItem> concepts = home().lastModifiedConcepts();
        return concepts == null ? Collections.emptyList() : concepts;
    }

    public boolean isLastModifiedConceptsPresent() {
        return !getLastModifiedConcepts().isEmpty();
    }

    public List<ThesaurusMetadataItem> getMetadata() {
        List<ThesaurusMetadataItem> metadata = home().metadata();
        return metadata == null ? Collections.emptyList() : metadata;
    }

    public boolean isMetadataPresent() {
        return !getMetadata().isEmpty();
    }

    public boolean isWorkshopVisible() {
        return toolboxAccessPolicy.canAccessWorkshop(userSession)
                && StringUtils.isNotBlank(getId());
    }

    public boolean isSettingsVisible() {
        return isCanEdit();
    }

    public boolean isMaintenanceVisible() {
        return toolboxAccessPolicy.canAccessMaintenance(userSession)
                && StringUtils.isNotBlank(getId());
    }

    public boolean isStatisticsDetailVisible() {
        return toolboxAccessPolicy.canViewStatistics(userSession)
                && StringUtils.isNotBlank(getId());
    }

    /** KPIs accueil : réservés aux utilisateurs connectés. */
    public boolean isStatisticsBlockVisible() {
        return userSession.isLoggedIn() && StringUtils.isNotBlank(getId());
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

    public String getSelectedLangFlag() {
        return flagEmoji(getSelectedLang());
    }

    public String getSelectedLangCode() {
        return StringUtils.defaultString(getSelectedLang()).toUpperCase(Locale.ROOT);
    }

    public boolean currentWorkLangIs(String code) {
        return StringUtils.isNotBlank(code) && code.equalsIgnoreCase(getSelectedLang());
    }

    public boolean isWorkLanguageSwitchable() {
        return getLanguages().size() > 1;
    }

    public void onLanguageChange() {
        if (StringUtils.isNotBlank(selectedLang)) {
            thesaurusContext.changeWorkLanguage(selectedLang);
            applyTitleForWorkLanguage(selectedLang);
        }
        invalidateHomeOverview();
        invalidateTree();
        breadcrumbEnabled = null;
        customRelationVisible = null;
        suggestionEnabled = null;
        if (editing) {
            homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        }
        if (detailRequested && StringUtils.isNotBlank(selectedId)) {
            openTreeNode(selectedId, selectedKind);
        }
    }

    /**
     * Comme le legacy : clic sur une traduction de la fiche → même concept
     * dans la langue choisie (sélecteur d'affichage + arbre + recherche).
     */
    public void openConceptInTranslationLanguage(String lang) {
        if (StringUtils.isBlank(lang) || "all".equalsIgnoreCase(lang)) {
            return;
        }
        selectedLang = lang.trim();
        thesaurusSearchLanguageSync.applyConsultationLanguageFromConceptTranslation(selectedLang);
        onLanguageChange();
    }

    public String getAlignmentSourcesUrl() {
        String conceptId = currentConceptId();
        if (StringUtils.isBlank(conceptId)) {
            return "setting/preference.xhtml#stAlign";
        }
        return "setting/preference.xhtml?idc=" + conceptId + "#stAlign";
    }

    public String getSettingsReturnUrl() {
        String conceptId = settingsConceptId();
        if (StringUtils.isBlank(conceptId)) {
            return "index.xhtml";
        }
        return "thesaurus/consultation.xhtml?id=" + conceptId;
    }

    public String getSettingsReturnTitle() {
        return StringUtils.isBlank(settingsConceptId())
                ? "Retour à l'accueil thésaurus"
                : "Retour au concept";
    }

    public void revealCurrentConceptInTree() {
        String conceptId = settingsConceptId();
        if (StringUtils.isBlank(conceptId)) {
            return;
        }
        revealId = conceptId;
        revealInTree();
    }

    public void restoreOpenedConcept() {
        ensureOpenedConceptFromRequest();
    }

    /**
     * La fiche est rendue après {@code showThesaurusHome}. On rouvre {@code idc}
     * au moment du rendu, quand les paramètres du chemin sont disponibles.
     */
    private void ensureOpenedConceptFromRequest() {
        if (openedConceptFromRequest || BrowseErrorRedirect.alreadyHandled()) {
            return;
        }
        openedConceptFromRequest = true;
        if (consultationShellBean.isThesaurusAccessDenied()) {
            return;
        }
        if (detailRequested && StringUtils.isNotBlank(selectedId)) {
            return;
        }
        String conceptId = firstNonBlank(requestParameter("idc"), requestParameter("id"));
        if (StringUtils.isBlank(conceptId)) {
            return;
        }
        restoreOpenedConcept(conceptId, requestParameter("type"));
        if (selectedConcept == null && selectedFacet == null) {
            detailRequested = false;
            selectedId = "";
            BrowseErrorRedirect.toConceptMissing(requestParameter("idt"), conceptId);
        }
    }

    public void restoreOpenedConcept(String conceptId, String nodeType) {
        if (detailRequested && StringUtils.isNotBlank(selectedId)) {
            return;
        }
        if (StringUtils.isBlank(conceptId)) {
            return;
        }
        openTreeNode(conceptId.trim(), StringUtils.trimToEmpty(nodeType));
        if (StringUtils.isBlank(selectedId)) {
            return;
        }
        revealId = selectedId;
        revealInTree();
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.isNotBlank(first)) {
            return first.trim();
        }
        return StringUtils.trimToEmpty(second);
    }

    public String currentConceptId() {
        if (selectedConcept != null && selectedConcept.getSummary() != null) {
            return StringUtils.trimToEmpty(selectedConcept.getSummary().getConceptId());
        }
        return StringUtils.trimToEmpty(conceptSelectionContext.getConceptId());
    }

    private String settingsConceptId() {
        String fromRequest = requestParameter("idc");
        if (StringUtils.isNotBlank(fromRequest)) {
            return fromRequest.trim();
        }
        return currentConceptId();
    }

    private String requestParameter(String name) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || StringUtils.isBlank(name)) {
            return "";
        }
        String fromParameter = StringUtils.trimToNull(
                facesContext.getExternalContext().getRequestParameterMap().get(name));
        if (fromParameter != null) {
            return fromParameter;
        }
        String fromPath = JsfPrettyUrlFilter.rememberedPathParameter(
                facesContext.getExternalContext().getRequestMap(),
                name
        );
        return StringUtils.trimToEmpty(fromPath);
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

    public void clearTreeReveal() {
        revealId = "";
        revealedConceptId = "";
    }

    public void revealInTree() {
        String id = StringUtils.trimToEmpty(revealId);
        revealedConceptId = id;
        if (StringUtils.isBlank(id)) {
            return;
        }
        ensureTreeLoaded();
        for (List<BreadcrumbStep> path : breadcrumbPathsForReveal(id)) {
            if (expandTreePath(idsOfPath(path, id))) {
                return;
            }
        }
    }

    private List<List<BreadcrumbStep>> breadcrumbPathsForReveal(String id) {
        List<List<BreadcrumbStep>> paths = conceptReadService.loadBreadcrumbPaths(
                getId(),
                id,
                getSelectedLang()
        );
        if (paths != null && !paths.isEmpty()) {
            return paths;
        }
        List<BreadcrumbStep> fallback = conceptReadService.loadBreadcrumb(getId(), id, getSelectedLang());
        return fallback == null || fallback.isEmpty()
                ? List.of(List.of(new BreadcrumbStep(id, id, 0, true)))
                : List.of(fallback);
    }

    private static List<String> idsOfPath(List<BreadcrumbStep> path, String id) {
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
        return ids;
    }

    public void openSelectedNode() {
        openTreeNode(openId, openType);
    }

    /**
     * Accueil détail du thésaurus (pas de concept ouvert) — utilisé depuis le picker.
     */
    public void showThesaurusHome() {
        if (BrowseErrorRedirect.alreadyHandled()) {
            return;
        }
        selectedConcept = null;
        selectedFacet = null;
        selectedKind = "";
        selectedId = "";
        detailRequested = false;
        openedConceptFromRequest = false;
        revealId = "";
        revealedConceptId = "";
        resetConceptExtras();
        conceptSelectionContext.clear();
        invalidateHomeOverview();
        invalidateTree();
        homeHtml = null;
        canEdit = null;
        languages = null;
        selectedLang = null;
        refreshPropositionBadge();
    }

    public void openTreeNode(String id, String nodeType) {
        if (propositionBean != null) {
            propositionBean.clearConsultation();
            propositionBean.refreshBadgeCount();
        }
        detailRequested = true;
        selectedId = StringUtils.trimToEmpty(id);
        selectedKind = "";
        selectedConcept = null;
        selectedFacet = null;
        ficheEditCard = null;
        candidateBy = "";
        candidateOn = "";
        candidateRejected = ConceptTreeNodeKinds.REJETE.equalsIgnoreCase(nodeType);
        resetConceptExtras();
        if (StringUtils.isBlank(id)) {
            conceptSelectionContext.clear();
            return;
        }
        if (ConceptTreeNodeKinds.FACET.equalsIgnoreCase(nodeType)) {
            conceptSelectionContext.clear();
            selectedFacet = conceptReadService.loadFacetDetail(getId(), id, getSelectedLang()).orElse(null);
            if (selectedFacet != null) {
                selectedKind = ConceptTreeNodeKinds.FACET;
                ThesaurusBrowseBean browse = thesaurusBrowseBean.getIfAvailable();
                if (browse != null) {
                    browse.logConceptView(selectedFacet);
                } else {
                    consultationViewLogger.logFacet(
                            selectedFacet.label(),
                            selectedFacet.lang(),
                            selectedFacet.facetId(),
                            getTitle(),
                            getId(),
                            getSelectedLang()
                    );
                }
            }
            return;
        }
        selectedConcept = conceptReadService.loadDetail(getId(), id, getSelectedLang(), true).orElse(null);
        if (selectedConcept == null) {
            conceptSelectionContext.clear();
            return;
        }
        boolean candidate = ConceptTreeNodeKinds.CANDIDAT.equalsIgnoreCase(nodeType)
                || ConceptTreeNodeKinds.REJETE.equalsIgnoreCase(nodeType)
                || "CA".equalsIgnoreCase(selectedConcept.getSummary().getStatus());
        selectedKind = candidate ? ConceptTreeNodeKinds.CANDIDAT : ConceptTreeNodeKinds.CONCEPT;
        if (candidate) {
            applySelectedCandidateMeta(id);
            var summary = selectedConcept.getSummary();
            consultationViewLogger.logCandidate(
                    summary.getPreferredLabel(),
                    summary.getLang(),
                    summary.getConceptId(),
                    getTitle(),
                    getId(),
                    getSelectedLang()
            );
        }
        loadConceptMetadata(id);
        conceptSelectionContext.update(getId(), selectedConcept);
    }

    /**
     * Reprend la sélection partagée (après mutation menu fil d'Ariane) et recharge arbre + fiche.
     */
    public void refreshFromSelectionContext() {
        reloadTree();
        if (conceptSelectionContext.hasSelection()) {
            String status = conceptSelectionContext.getSummary() != null
                    ? conceptSelectionContext.getSummary().getStatus()
                    : "";
            String kind = "CA".equalsIgnoreCase(status) ? ConceptTreeNodeKinds.CANDIDAT : ConceptTreeNodeKinds.CONCEPT;
            openTreeNode(conceptSelectionContext.getConceptId(), kind);
            return;
        }
        selectedConcept = null;
        selectedFacet = null;
        selectedKind = "";
        selectedId = "";
        detailRequested = false;
        resetConceptExtras();
    }

    /** Recharge la fiche ouverte après une mutation (libellé, etc.) sans changer de nœud. */
    public void reloadSelectedConcept() {
        if (StringUtils.isNotBlank(selectedId)) {
            openTreeNode(selectedId, selectedKind);
        }
    }

    public void searchCorpusLinks() {
        corpusSearched = true;
        if (selectedConcept == null || selectedConcept.getSummary() == null) {
            displayedCorpusLinks = Collections.emptyList();
            return;
        }
        displayedCorpusLinks = conceptReadService.loadCorpusLinks(
                getId(),
                conceptReadService.toCorpusSearchContext(selectedConcept.getSummary())
        );
    }

    public boolean isCorpusNoResult() {
        return corpusSearched && displayedCorpusLinks.isEmpty();
    }

    public boolean isShowQrCode() {
        ConceptIdentifiers identifiers = selectedConcept == null ? null : selectedConcept.getIdentifiers();
        return identifiers != null && identifiers.isShowQrCode();
    }

    public String getQrSvg() {
        if (!isShowQrCode()) {
            return "";
        }
        return ConceptQrSvgSupport.toSvg(selectedConcept.getIdentifiers().getQrCodeValue());
    }

    public String getGpsDisplay() {
        if (selectedConcept == null || selectedConcept.getGpsPoints() == null
                || selectedConcept.getGpsPoints().isEmpty()) {
            return "";
        }
        return selectedConcept.getGpsPoints().stream()
                .map(ThesaurusViewBean::formatGpsPoint)
                .collect(Collectors.joining(", "));
    }

    public List<String> getSelectedNoteTypeCodes() {
        return getDisplayedNotes().stream()
                .map(ConceptNote::typeCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    public void toggleNoteLanguages() {
        showAllNoteLanguages = !showAllNoteLanguages;
    }

    public boolean isNoteLanguageToggleVisible() {
        if (selectedConcept == null || selectedConcept.getNotes() == null) {
            return false;
        }
        String lang = getSelectedLang();
        return selectedConcept.getNotes().stream()
                .anyMatch(note -> note != null && !matchesNoteLang(note.lang(), lang));
    }

    public boolean hasDisplayedNotes() {
        return !getDisplayedNotes().isEmpty();
    }

    /** EL JSF : {@code #{bean.hasDisplayedNotes}} résout {@code isHasDisplayedNotes()}, pas {@code hasDisplayedNotes()}. */
    public boolean isHasDisplayedNotes() {
        return hasDisplayedNotes();
    }

    public List<ConceptNote> displayedNotes() {
        return getDisplayedNotes();
    }

    public List<ConceptNote> getDisplayedNotes() {
        if (selectedConcept == null || selectedConcept.getNotes() == null) {
            return Collections.emptyList();
        }
        if (showAllNoteLanguages) {
            return selectedConcept.getNotes();
        }
        String lang = getSelectedLang();
        return selectedConcept.getNotes().stream()
                .filter(note -> note != null && matchesNoteLang(note.lang(), lang))
                .toList();
    }

    private static boolean matchesNoteLang(String noteLang, String selected) {
        String note = languagePrimary(noteLang);
        if (note.isEmpty()) {
            return true;
        }
        return Strings.CI.equals(note, languagePrimary(selected));
    }

    private static String languagePrimary(String lang) {
        String value = StringUtils.trimToEmpty(lang);
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return "";
        }
        int dash = value.indexOf('-');
        int underscore = value.indexOf('_');
        int cut = dash > 0 ? dash : underscore;
        if (cut > 0) {
            value = value.substring(0, cut);
        }
        return value;
    }

    /** Libellés préférentiels des autres langues — structure {@code tr-list} de la maquette. */
    public List<ConceptLabel> getPreferredTranslations() {
        if (selectedConcept == null || selectedConcept.getTranslations() == null) {
            return Collections.emptyList();
        }
        return selectedConcept.getTranslations().stream()
                .filter(ConceptLabel::isPreferred)
                .toList();
    }

    /**
     * Traductions affichées sur la fiche : hors langue courante, pastille « à compléter »
     * si le libellé est vide (maquette EDT-9).
     */
    public List<TranslationViewItem> getTranslationViewItems() {
        String work = StringUtils.defaultString(getSelectedLang()).trim().toLowerCase(Locale.ROOT);
        List<TranslationViewItem> items = new ArrayList<>();
        for (ConceptLabel label : getPreferredTranslations()) {
            if (label == null || StringUtils.isBlank(label.lang())) {
                continue;
            }
            if (work.equals(label.lang().trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(new TranslationViewItem(
                    label.lang(),
                    StringUtils.defaultString(label.value()),
                    altTranslationsLabel(label.lang()),
                    StringUtils.isBlank(label.value())
            ));
        }
        return items;
    }

    /**
     * Traductions de la fiche facette : même présentation que le concept
     * (hors langue courante déjà filtrée à la lecture).
     */
    public List<TranslationViewItem> getFacetTranslationViewItems() {
        if (selectedFacet == null || selectedFacet.translations() == null) {
            return List.of();
        }
        String work = StringUtils.defaultString(getSelectedLang()).trim().toLowerCase(Locale.ROOT);
        List<TranslationViewItem> items = new ArrayList<>();
        for (GroupTranslationItem tr : selectedFacet.translations()) {
            if (tr == null || StringUtils.isBlank(tr.lang())) {
                continue;
            }
            if (work.equals(tr.lang().trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            items.add(new TranslationViewItem(
                    tr.lang(),
                    StringUtils.defaultString(tr.value()),
                    "",
                    StringUtils.isBlank(tr.value())
            ));
        }
        return items;
    }

    public boolean hasLanguageFlag(String lang) {
        String emoji = flagEmoji(lang);
        return StringUtils.isNotBlank(emoji) && !"🏳️".equals(emoji);
    }

    public String textDirection(String lang) {
        String code = StringUtils.defaultString(lang).trim().toLowerCase(Locale.ROOT);
        if (code.contains("-")) {
            code = code.substring(0, code.indexOf('-'));
        }
        return switch (code) {
            case "ar", "he", "fa", "ur" -> "rtl";
            default -> "ltr";
        };
    }

    @Getter
    public static final class TranslationViewItem implements Serializable {
        private final String lang;
        private final String value;
        private final String alts;
        private final boolean todo;

        public TranslationViewItem(String lang, String value, String alts, boolean todo) {
            this.lang = lang;
            this.value = value;
            this.alts = alts;
            this.todo = todo;
        }
    }

    public String altTranslationsLabel(String lang) {
        if (selectedConcept == null || selectedConcept.getTranslations() == null
                || StringUtils.isBlank(lang)) {
            return "";
        }
        return selectedConcept.getTranslations().stream()
                .filter(label -> !label.isPreferred() && !label.isHidden()
                        && lang.equalsIgnoreCase(label.lang()))
                .map(ConceptLabel::value)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    public List<ConceptNote> notesOfType(String typeCode) {
        return getDisplayedNotes().stream()
                .filter(note -> Strings.CS.equals(note.typeCode(), typeCode))
                .toList();
    }

    public String flagEmoji(String lang) {
        String code = StringUtils.defaultString(lang).trim().toLowerCase(Locale.ROOT);
        if (code.contains("-")) {
            code = code.substring(0, code.indexOf('-'));
        }
        String region = switch (code) {
            case "en" -> "gb";
            case "ar" -> "sa";
            case "zh" -> "cn";
            case "ja" -> "jp";
            case "ko" -> "kr";
            case "el" -> "gr";
            case "uk" -> "ua";
            case "cs" -> "cz";
            case "da" -> "dk";
            case "sv" -> "se";
            case "nb", "nn", "no" -> "no";
            case "he" -> "il";
            case "fa" -> "ir";
            case "hi" -> "in";
            case "eu" -> "es";
            case "ca" -> "es";
            case "cy" -> "gb";
            default -> code.length() == 2 ? code : "";
        };
        if (region.length() != 2 || !region.chars().allMatch(ch -> ch >= 'a' && ch <= 'z')) {
            return "🏳️";
        }
        return new String(Character.toChars(0x1F1E6 + (region.charAt(0) - 'a')))
                + new String(Character.toChars(0x1F1E6 + (region.charAt(1) - 'a')));
    }

    public String languageLabel(String lang) {
        if (StringUtils.isBlank(lang)) {
            return "";
        }
        return getLanguages().stream()
                .filter(item -> lang.equalsIgnoreCase(item.code()))
                .map(ThesaurusLanguage::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(lang);
    }

    public String skosExportUrl(String format) {
        if (selectedConcept == null || selectedConcept.getSummary() == null) {
            return "#";
        }
        String extension = switch (StringUtils.defaultString(format).toLowerCase(Locale.ROOT)) {
            case "json" -> "json";
            case "jsonld" -> "jsonld";
            case "turtle", "ttl" -> "ttl";
            default -> "rdf";
        };
        String contextPath = requestContextPath();
        return contextPath + "/api/" + getId() + "." + selectedConcept.getSummary().getConceptId() + "." + extension;
    }

    public boolean isDetailVisible() {
        ensureOpenedConceptFromRequest();
        return selectedConcept != null || selectedFacet != null;
    }

    public boolean isConceptSelected() {
        return ConceptTreeNodeKinds.CONCEPT.equals(selectedKind) && selectedConcept != null;
    }

    public boolean isCandidateSelected() {
        return ConceptTreeNodeKinds.CANDIDAT.equals(selectedKind) && selectedConcept != null;
    }

    public boolean isRejectedSelected() {
        return isCandidateSelected() && candidateRejected;
    }

    /** Après réactivation : force le mode « en attente » côté sélection arbre/fiche. */
    public void markSelectedCandidatePending() {
        candidateRejected = false;
        if (selectedConcept != null) {
            selectedKind = ConceptTreeNodeKinds.CANDIDAT;
        }
    }

    /**
     * Après acceptation : la fiche ouverte est un concept, même si la lecture
     * du statut renvoie encore {@code CA} dans la même requête.
     */
    public void markSelectedAsConcept() {
        candidateRejected = false;
        candidateBy = "";
        candidateOn = "";
        if (selectedConcept != null) {
            selectedKind = ConceptTreeNodeKinds.CONCEPT;
        }
        touchOpenedConcept();
    }

    /** Après refus : la fiche ouverte reste un candidat, au statut rejeté. */
    public void markSelectedRejected() {
        candidateRejected = true;
        if (selectedConcept != null) {
            selectedKind = ConceptTreeNodeKinds.CANDIDAT;
        }
        touchOpenedConcept();
    }

    /** Force la fiche candidat à relire son statut au prochain affichage. */
    public void touchOpenedConcept() {
        conceptRevision++;
    }

    /**
     * Le nœud déjà chargé passe en « inséré » sans reconstruire l'arbre
     * (l'arbre de la vue reste en cache tant que la page n'est pas rechargée).
     * S'il n'est pas dans les nœuds chargés, l'arbre est invalidé pour le prochain rendu.
     */
    public void markTreeNodeInserted(String conceptId) {
        ThesaurusTreeNode node = loadedTreeNode(conceptId);
        if (node == null) {
            invalidateTree();
            return;
        }
        node.setNodeType(node.isHasChildren() ? "concept" : "file");
        node.setStatus("insere");
        node.setCandidateBy("");
        node.setCandidateOn("");
    }

    /** Même mise à jour immédiate pour un candidat refusé depuis la fiche. */
    public void markTreeNodeRejected(String conceptId) {
        ThesaurusTreeNode node = loadedTreeNode(conceptId);
        if (node == null) {
            invalidateTree();
            return;
        }
        node.setNodeType(ConceptTreeNodeKinds.REJETE);
        node.setStatus(ConceptTreeNodeKinds.REJETE);
    }

    private ThesaurusTreeNode loadedTreeNode(String conceptId) {
        if (StringUtils.isBlank(conceptId) || treeRoots == null) {
            return null;
        }
        return findLoadedTreeNode(treeRoots, conceptId);
    }

    private ThesaurusTreeNode findLoadedTreeNode(List<ThesaurusTreeNode> nodes, String conceptId) {
        if (nodes == null) {
            return null;
        }
        for (ThesaurusTreeNode node : nodes) {
            if (conceptId.equalsIgnoreCase(node.getId())) {
                return node;
            }
            ThesaurusTreeNode found = findLoadedTreeNode(node.getChildren(), conceptId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public boolean isFacetSelected() {
        return ConceptTreeNodeKinds.FACET.equals(selectedKind) && selectedFacet != null;
    }

    public boolean isSelectedConceptDeprecated() {
        return selectedConcept != null && selectedConcept.isDeprecated();
    }

    /** Statut visuel aligné sur la maquette {@code target.html} : {@code valide} / {@code candidat} / {@code deprecie}. */
    public String getConceptDisplayStatus() {
        if (isRejectedSelected()) {
            return ConceptTreeNodeKinds.REJETE;
        }
        if (isCandidateSelected()) {
            return ConceptTreeNodeKinds.CANDIDAT;
        }
        if (isSelectedConceptDeprecated()) {
            return ConceptTreeNodeKinds.DEPRECIE;
        }
        return ConceptTreeNodeKinds.VALIDE;
    }

    public String getConceptUri() {
        if (selectedConcept == null || selectedConcept.getIdentifiers() == null) {
            return "";
        }
        ConceptIdentifiers identifiers = selectedConcept.getIdentifiers();
        if (identifiers.isShowOriginalUri() && StringUtils.isNotBlank(identifiers.getOriginalUri())) {
            return identifiers.getOriginalUri();
        }
        return StringUtils.defaultString(identifiers.getInternalPermalinkUrl());
    }

    public boolean isBreadcrumbEnabled() {
        if (breadcrumbEnabled == null) {
            ThesaurusPreferences preferences = loadThesaurusPreferences();
            breadcrumbEnabled = preferences != null && preferences.breadcrumb();
        }
        return breadcrumbEnabled;
    }

    public boolean isCustomRelationVisible() {
        if (customRelationVisible == null) {
            ThesaurusPreferences preferences = loadThesaurusPreferences();
            customRelationVisible = preferences != null && preferences.useCustomRelation();
        }
        return customRelationVisible;
    }

    /**
     * Préférence « Activer les propositions de modifications pour les concepts ».
     */
    public boolean isSuggestionEnabled() {
        if (suggestionEnabled == null) {
            ThesaurusPreferences preferences = loadThesaurusPreferences();
            suggestionEnabled = preferences != null && preferences.suggestion();
        }
        return suggestionEnabled;
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
        return StringUtils.defaultString(home().homePageHtml());
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

    /**
     * Menu contextuel à côté du fil d'Ariane : rôle manager (comme {@code hasRoleAsManager} legacy).
     */
    public boolean isConceptActionsVisible() {
        if (isCandidateSelected()) {
            return false;
        }
        if (conceptActionsVisible != null) {
            return conceptActionsVisible;
        }
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            conceptActionsVisible = false;
            return false;
        }
        conceptActionsVisible = rightsService.canOnThesaurus(
                userId, Permission.MUTATE_CONCEPT_STRUCTURE, thesaurusId);
        return conceptActionsVisible;
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
            saveMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
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
        invalidateHomeOverview();
        saveMessage = "Description enregistrée.";
    }

    private ThesaurusHomeOverview home() {
        if (homeOverview == null) {
            homeOverview = thesaurusHomeReadService.loadOverview(
                    getId(),
                    thesaurusContext.resolveWorkLanguage(),
                    getTitle()
            );
        }
        return homeOverview;
    }

    public void refreshHomeOverview() {
        invalidateHomeOverview();
    }

    private void refreshPropositionBadge() {
        if (propositionBean != null) {
            propositionBean.refreshBadgeCount();
        }
    }

    private void invalidateHomeOverview() {
        homeOverview = null;
    }

    private void invalidateTree() {
        treeRoots = null;
    }

    public void reloadTree() {
        invalidateTree();
    }

    private void ensureTreeLoaded() {
        if (treeRoots != null) {
            return;
        }
        treeRoots = new ArrayList<>();
        if (StringUtils.isBlank(getId())) {
            return;
        }
        List<ConceptTreeNodeData> roots = conceptReadService.loadTreeRootNodes(
                getId(),
                getSelectedLang(),
                isSortByNotation()
        );
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
        if (ConceptTreeNodeKinds.CANDIDAT.equals(data.getNodeType())) {
            node.setStatus(ConceptTreeNodeKinds.CANDIDAT);
        } else if ("insere".equals(data.getNodeType())) {
            node.setStatus("insere");
        } else if (ConceptTreeNodeKinds.REJETE.equals(data.getNodeType())) {
            node.setStatus(ConceptTreeNodeKinds.REJETE);
        } else if ("deprecated".equals(data.getNodeType())) {
            node.setStatus(ConceptTreeNodeKinds.DEPRECIE);
        } else {
            node.setStatus(ConceptTreeNodeKinds.VALIDE);
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
            if (row.length > 3 && row[3] != null) {
                candidateRejected = toCandidatStatus(row[3]) == CandidatStatusCode.REJECTED;
            }
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

    private void applyTitleForWorkLanguage(String lang) {
        ensureLanguagesLoaded();
        if (languages == null || StringUtils.isBlank(lang)) {
            return;
        }
        languages.stream()
                .filter(item -> lang.equalsIgnoreCase(item.code()))
                .map(ThesaurusLanguage::labelTheso)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .ifPresent(thesaurusContext::setCurrentThesaurusTitle);
    }

    private void ensureLanguagesLoaded() {
        if (languages != null) {
            return;
        }
        if (StringUtils.isBlank(getId())) {
            languages = Collections.emptyList();
            return;
        }
        languages = thesaurusPreferenceService.loadUsedLanguages(getId(), localeBean.getIdLangue());
        if (languages == null) {
            languages = Collections.emptyList();
        }
    }

    private boolean languageExists(String lang) {
        return StringUtils.isNotBlank(lang)
                && languages.stream().anyMatch(item -> item.code().equalsIgnoreCase(lang));
    }

    private static int toCandidatStatus(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String formatCount(int count) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(count);
    }

    private String requestContextPath() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return "";
        }
        return StringUtils.defaultString(facesContext.getExternalContext().getRequestContextPath());
    }

    private void resetConceptExtras() {
        branchConceptCount = 0;
        corpusSearched = false;
        displayedCorpusLinks = Collections.emptyList();
        conceptMetadata = List.of();
    }

    private void loadConceptMetadata(String conceptId) {
        List<Object[]> rows = conceptDcTermRepository.findDisplayedMetadata(getId(), conceptId);
        if (rows == null || rows.isEmpty()) {
            conceptMetadata = List.of();
            return;
        }
        List<ThesaurusMetadataItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String name = metadataCell(row, 0);
            String value = metadataCell(row, 1);
            if (value.isEmpty() || "null".equalsIgnoreCase(value) || name.isEmpty() || "maitre".equalsIgnoreCase(name)) {
                continue;
            }
            if (replacementShownInForm(name)) {
                continue;
            }
            String language = metadataCell(row, 2);
            if ("null".equalsIgnoreCase(language)) {
                language = "";
            }
            items.add(new ThesaurusMetadataItem(0, name, value, language, metadataCell(row, 3)));
        }
        conceptMetadata = List.copyOf(items);
    }

    private static String metadataCell(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return StringUtils.trimToEmpty(row[index].toString());
    }

    private boolean replacementShownInForm(String name) {
        if (selectedConcept == null) {
            return false;
        }
        String key = dcLocalName(name).toLowerCase(Locale.ROOT);
        if ("replaces".equals(key)) {
            return selectedConcept.getReplaces() != null && !selectedConcept.getReplaces().isEmpty();
        }
        if ("isreplacedby".equals(key)) {
            return selectedConcept.getReplacedBy() != null && !selectedConcept.getReplacedBy().isEmpty();
        }
        return false;
    }

    public String metadataLabel(String name) {
        String key = dcLocalName(name);
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "creator" -> "Créateur";
            case "contributor" -> "Contributeur";
            case "subject" -> "Sujet";
            case "description" -> "Description";
            case "publisher" -> "Éditeur";
            case "date" -> "Date";
            case "type" -> "Type";
            case "format" -> "Format";
            case "identifier" -> "Identifiant";
            case "source" -> "Source";
            case "language" -> "Langue";
            case "relation" -> "Relation";
            case "coverage" -> "Couverture";
            case "rights" -> "Droits";
            case "rightsholder" -> "Titulaire des droits";
            case "conformsto" -> "Conforme à";
            case "created" -> "Créé";
            case "modified" -> "Modifié";
            case "license" -> "Licence";
            case "replaces" -> "Remplace";
            case "isreplacedby" -> "Est remplacé par";
            case "alternative" -> "Alternative";
            case "title" -> "Titre";
            case "maitre" -> "Maître";
            default -> key.isEmpty() ? StringUtils.defaultString(name) : key;
        };
    }

    private static String dcLocalName(String name) {
        String value = StringUtils.trimToEmpty(name);
        int cut = Math.max(value.lastIndexOf('#'), Math.max(value.lastIndexOf('/'), value.lastIndexOf(':')));
        if (cut >= 0 && cut < value.length() - 1) {
            value = value.substring(cut + 1);
        }
        return value;
    }

    private static String formatGpsPoint(ConceptGpsPoint point) {
        return formatGpsCoordinate(point.latitude()) + " " + formatGpsCoordinate(point.longitude());
    }

    private static String formatGpsCoordinate(double value) {
        return ConceptGpsPoint.formatCoordinate(value);
    }
}
