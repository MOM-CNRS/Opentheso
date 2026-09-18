package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusPickerRow;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.shared.ui.V2NavigationBean;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.ui.NewThesaurusEditor;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusPickerBean")
@RequiredArgsConstructor
public class ThesaurusPickerBean implements Serializable {

    public static final String TAB_ALL = "all";
    public static final String TAB_MEMBER = "member";
    public static final String TAB_PUBLIC = "public";

    private static final List<ColumnDef> COLUMN_DEFS = List.of(
            new ColumnDef("name", "v2.picker.col.name", true, true),
            new ColumnDef("access", "v2.picker.col.access", false, false),
            new ColumnDef("role", "v2.picker.col.role", false, false),
            new ColumnDef("terms", "v2.picker.col.terms", false, false),
            new ColumnDef("created", "v2.picker.col.created", false, false),
            new ColumnDef("domain", "v2.picker.col.domain", true, false),
            new ColumnDef("projects", "v2.picker.col.projects", true, false),
            new ColumnDef("org", "v2.picker.col.org", true, false),
            new ColumnDef("chrono", "v2.picker.col.chrono", false, false),
            new ColumnDef("lang", "v2.picker.col.lang", false, false)
    );

    private final transient ConsultationCatalogService consultationCatalogService;
    private final transient ConsultationShellBean consultationShellBean;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient V2NavigationBean v2NavigationBean;
    private final transient NewThesaurusService newThesaurusService;
    private final transient ThesaurusPickerImportBean thesaurusPickerImportBean;
    private final transient ThesaurusPickerExportBean thesaurusPickerExportBean;
    private final transient ThesaurusAccessService thesaurusAccessService;
    private final transient ModifyThesaurusService modifyThesaurusService;
    private final transient EditionThesaurusService editionThesaurusService;

    private List<ThesaurusPickerRow> rows = List.of();
    private String query = "";
    private String activeTab = TAB_ALL;
    private String sortColumn = "name";
    private boolean sortAscending = true;
    private boolean listLoading;

    private transient List<ThesaurusPickerRow> filteredRowsCache;
    private transient String filterCacheKey;
    private int cachedMemberCount = -1;
    private int cachedPublicCount = -1;
    private int cachedQueryMatchCount = -1;

    private List<ColumnCfg> columnCfg = defaultColumnCfg();
    private boolean columnsMenuOpen;
    private String filterName = "";
    private String filterDomain = "";
    private String filterProjects = "";
    private String filterOrg = "";
    private String filterLang = "";

    private boolean createMode;
    private NewThesaurusEditor createEditor = NewThesaurusEditor.empty();
    private List<LanguageOption> createLanguages = Collections.emptyList();
    private List<ProjectOption> createProjects = Collections.emptyList();
    private boolean createSuperAdmin;

    private boolean editMode;
    private String editThesaurusId;
    private String editTitle;
    private String editLanguage;
    private boolean editPrivateThesaurus;
    private String editError;

    private String deleteThesaurusId;
    private String deleteThesaurusTitle;
    private boolean deletePerennialIdentifiers;

    /** Toast ponctuel sur la liste (import / suppression / actions). */
    private String listFlashMessage;
    private String listFlashToken;
    private boolean listFlashError;

    @PostConstruct
    public void init() {
        if (!userSession.isLoggedIn()) {
            activeTab = TAB_PUBLIC;
            applyGuestColumnDefaults();
        }
        load();
    }

    public void load() {
        listLoading = true;
        try {
            consultationShellBean.clearThesaurusAccessDenied();
            rows = consultationCatalogService.listPickerThesauri(
                    userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                    userSession.isSuperAdmin(),
                    v2LocaleBean.getIdLangue()
            );
            invalidateFilterCache();
        } finally {
            listLoading = false;
        }
    }

    public List<ThesaurusPickerRow> getFilteredRows() {
        String key = buildFilterCacheKey();
        if (filteredRowsCache != null && Strings.CS.equals(key, filterCacheKey)) {
            return filteredRowsCache;
        }
        String needle = normalize(query);
        filteredRowsCache = getCatalogRows().stream()
                .filter(this::matchesColumnFilters)
                .filter(row -> matchesQuery(row, needle))
                .filter(row -> matchesTab(row, activeTab))
                .toList();
        filterCacheKey = key;
        cachedQueryMatchCount = (int) rows.stream().filter(row -> matchesQuery(row, needle)).count();
        return filteredRowsCache;
    }

    /**
     * Toutes les lignes triées (filtres colonnes / recherche / onglet côté client).
     */
    public List<ThesaurusPickerRow> getCatalogRows() {
        return rows.stream()
                .sorted(comparator())
                .toList();
    }

    /** Texte indexé pour le filtre de recherche client (liste des thésaurus). */
    public String searchText(ThesaurusPickerRow row) {
        if (row == null) {
            return "";
        }
        return normalize(row.name())
                + " "
                + normalize(row.id())
                + " "
                + normalize(row.projects())
                + " "
                + normalize(row.domain())
                + " "
                + normalize(row.organization())
                + " "
                + normalize(row.chronology());
    }

    public String colFilterName(ThesaurusPickerRow row) {
        if (row == null) {
            return "";
        }
        return normalize(row.name()) + " " + normalize(row.id());
    }

    public String colFilterDomain(ThesaurusPickerRow row) {
        return row == null ? "" : normalize(row.domain());
    }

    public String colFilterProjects(ThesaurusPickerRow row) {
        return row == null ? "" : normalize(row.projects());
    }

    public String colFilterOrg(ThesaurusPickerRow row) {
        return row == null ? "" : normalize(row.organization());
    }

    public String colFilterLangs(ThesaurusPickerRow row) {
        if (row == null || row.languages() == null || row.languages().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String lang : row.languages()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(normalize(lang));
        }
        return sb.toString();
    }

    public int getMemberCount() {
        if (cachedMemberCount < 0) {
            cachedMemberCount = (int) rows.stream().filter(ThesaurusPickerRow::isMember).count();
        }
        return cachedMemberCount;
    }

    public int getPublicCount() {
        if (cachedPublicCount < 0) {
            cachedPublicCount = (int) rows.stream().filter(ThesaurusPickerRow::isPublicAccess).count();
        }
        return cachedPublicCount;
    }

    /** Nombre de lignes après recherche, tous onglets confondus (badge « Tous »). */
    public int getQueryMatchCount() {
        String key = buildFilterCacheKey();
        if (cachedQueryMatchCount < 0 || !Strings.CS.equals(key, filterCacheKey)) {
            getFilteredRows();
        }
        if (cachedQueryMatchCount < 0) {
            String needle = normalize(query);
            cachedQueryMatchCount = (int) rows.stream().filter(row -> matchesQuery(row, needle)).count();
        }
        return cachedQueryMatchCount;
    }

    private void invalidateFilterCache() {
        filteredRowsCache = null;
        filterCacheKey = null;
        cachedMemberCount = -1;
        cachedPublicCount = -1;
        cachedQueryMatchCount = -1;
    }

    private String buildFilterCacheKey() {
        return System.identityHashCode(rows)
                + "|" + StringUtils.defaultString(query)
                + "|" + StringUtils.defaultString(activeTab)
                + "|" + StringUtils.defaultString(filterName)
                + "|" + StringUtils.defaultString(filterDomain)
                + "|" + StringUtils.defaultString(filterProjects)
                + "|" + StringUtils.defaultString(filterOrg)
                + "|" + StringUtils.defaultString(filterLang)
                + "|" + StringUtils.defaultString(sortColumn)
                + "|" + sortAscending;
    }

    public int getFilteredCount() {
        return getFilteredRows().size();
    }

    public int getEmptyColspan() {
        // cfg + colonnes visibles + actions (connecté uniquement)
        return getColumnCfg().size() + 1 + (isLoggedIn() ? 1 : 0);
    }

    public String getAriaSort(String column) {
        if (!isSortedBy(column)) {
            return "none";
        }
        return sortAscending ? "ascending" : "descending";
    }

    public boolean isLoggedIn() {
        return userSession.isLoggedIn();
    }

    public boolean isCanCreateThesaurus() {
        return userSession.isLoggedIn();
    }

    public boolean isCanImportThesaurus() {
        return thesaurusPickerImportBean != null && thesaurusPickerImportBean.isCanImportThesaurus();
    }

    public boolean isImportMode() {
        return thesaurusPickerImportBean != null && thesaurusPickerImportBean.isImportMode();
    }

    public boolean isExportMode() {
        return thesaurusPickerExportBean != null && thesaurusPickerExportBean.isExportMode();
    }

    public boolean isListMode() {
        return !createMode && !editMode && !isImportMode() && !isExportMode();
    }

    public boolean canManageRow(ThesaurusPickerRow row) {
        if (row == null || !userSession.isLoggedIn()) {
            return false;
        }
        if (userSession.isSuperAdmin()) {
            return true;
        }
        return row.canManage();
    }

    private boolean assertCanManage(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(userId, userSession.isSuperAdmin(), thesaurusId.trim());
    }

    public void openImportThesaurus() {
        if (!isCanImportThesaurus()) {
            return;
        }
        cancelCreateThesaurus();
        cancelEditThesaurus();
        cancelExportThesaurus();
        columnsMenuOpen = false;
        thesaurusPickerImportBean.open();
    }

    public void cancelImportThesaurus() {
        if (thesaurusPickerImportBean != null) {
            thesaurusPickerImportBean.cancel();
        }
    }

    public void openExportThesaurus() {
        FacesContext faces = FacesContext.getCurrentInstance();
        String thesaurusId = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("exportThesaurusId")
                : null;
        openExportThesaurus(thesaurusId);
    }

    public void openExportThesaurus(String thesaurusId) {
        if (!isLoggedIn() || StringUtils.isBlank(thesaurusId) || thesaurusPickerExportBean == null) {
            return;
        }
        String id = thesaurusId.trim();
        ThesaurusPickerRow match = rows == null ? null : rows.stream()
                .filter(row -> Strings.CS.equals(row.id(), id))
                .findFirst()
                .orElse(null);
        String title = match != null ? match.name() : id;
        long terms = match != null ? match.terms() : 0L;
        cancelCreateThesaurus();
        cancelEditThesaurus();
        cancelImportThesaurus();
        columnsMenuOpen = false;
        thesaurusPickerExportBean.open(id, title, terms);
    }

    public void cancelExportThesaurus() {
        if (thesaurusPickerExportBean != null) {
            thesaurusPickerExportBean.cancel();
        }
    }

    public List<ColumnCfg> getColumnCfg() {
        if (isLoggedIn()) {
            return columnCfg;
        }
        return columnCfg.stream()
                .filter(c -> !isGuestHiddenColumn(c.getKey()))
                .toList();
    }

    public List<ColumnCfg> getVisibleColumns() {
        return columnCfg.stream()
                .filter(ColumnCfg::isOn)
                .filter(c -> isLoggedIn() || !isGuestHiddenColumn(c.getKey()))
                .toList();
    }

    public boolean isColumnVisible(String key) {
        if (!isLoggedIn() && isGuestHiddenColumn(key)) {
            return false;
        }
        return columnCfg.stream().anyMatch(c -> c.isOn() && Strings.CS.equals(c.getKey(), key));
    }

    public String getColumnLabel(String key) {
        return COLUMN_DEFS.stream()
                .filter(d -> Strings.CS.equals(d.key(), key))
                .map(def -> {
                    String msg = v2LocaleBean.getMsg(def.labelKey());
                    return StringUtils.isNotBlank(msg) ? msg : key;
                })
                .findFirst()
                .orElse(key);
    }

    public boolean isColumnFilterable(String key) {
        return COLUMN_DEFS.stream()
                .anyMatch(d -> Strings.CS.equals(d.key(), key) && d.filterable());
    }

    public boolean isColumnGrow(String key) {
        return COLUMN_DEFS.stream()
                .anyMatch(d -> Strings.CS.equals(d.key(), key) && d.grow());
    }

    public List<String> getAvailableLanguages() {
        Set<String> langs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ThesaurusPickerRow row : rows) {
            if (row.languages() != null) {
                langs.addAll(row.languages());
            }
        }
        return List.copyOf(langs);
    }

    /** Drapeau emoji pour un code langue (affichage tableau / filtre). */
    public String languageFlag(String lang) {
        String code = normalizeLangCode(lang);
        if (code.isEmpty()) {
            return "🏳️";
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
            case "eu", "ca", "gl" -> "es";
            case "cy" -> "gb";
            case "pt" -> "pt";
            case "la" -> "va";
            default -> code.length() == 2 ? code : "";
        };
        if (region.length() != 2 || !region.chars().allMatch(ch -> ch >= 'a' && ch <= 'z')) {
            return "🏳️";
        }
        return new String(Character.toChars(0x1F1E6 + (region.charAt(0) - 'a')))
                + new String(Character.toChars(0x1F1E6 + (region.charAt(1) - 'a')));
    }

    /** Endonyme / libellé pour le tooltip. */
    public String languageLabel(String lang) {
        String code = normalizeLangCode(lang);
        if (code.isEmpty()) {
            return "";
        }
        Locale ui = Locale.forLanguageTag(StringUtils.defaultIfBlank(v2LocaleBean.getIdLangue(), "fr"));
        Locale langLocale = Locale.forLanguageTag(code);
        String display = langLocale.getDisplayLanguage(ui);
        return StringUtils.isNotBlank(display) ? StringUtils.capitalize(display) : code.toUpperCase(Locale.ROOT);
    }

    /** Libellé du filtre : 🇫🇷 Français */
    public String languageOptionLabel(String lang) {
        String code = normalizeLangCode(lang);
        if (code.isEmpty()) {
            return "";
        }
        return languageFlag(code) + " " + languageLabel(code);
    }

    /** Premières langues affichées en chips (le reste passe dans +N). */
    public List<String> visibleLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }
        if (languages.size() <= 5) {
            return languages;
        }
        return languages.subList(0, 4);
    }

    /** Langue masquée tant que le +N n'a pas été déplié. */
    public boolean isCollapsedLanguage(List<String> languages, int index) {
        if (languages == null || languages.size() <= 5) {
            return false;
        }
        return index >= 4;
    }

    public int hiddenLanguageCount(List<String> languages) {
        if (languages == null || languages.size() <= 5) {
            return 0;
        }
        return languages.size() - 4;
    }

    public String languagesTitle(List<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return "";
        }
        StringBuilder title = new StringBuilder();
        for (String lang : languages) {
            if (!title.isEmpty()) {
                title.append(", ");
            }
            title.append(languageLabel(lang)).append(" (").append(normalizeLangCode(lang)).append(")");
        }
        return title.toString();
    }

    private static String normalizeLangCode(String lang) {
        if (StringUtils.isBlank(lang)) {
            return "";
        }
        String code = lang.trim().toLowerCase(Locale.ROOT);
        int dash = code.indexOf('-');
        if (dash > 0) {
            code = code.substring(0, dash);
        }
        return code;
    }

    public void selectTab(String tab) {
        if (TAB_MEMBER.equals(tab) || TAB_PUBLIC.equals(tab) || TAB_ALL.equals(tab)) {
            activeTab = tab;
            invalidateFilterCache();
        }
    }

    public void clearQuery() {
        query = "";
        invalidateFilterCache();
    }

    public void toggleSort(String column) {
        if (Strings.CS.equals(sortColumn, column)) {
            sortAscending = !sortAscending;
            invalidateFilterCache();
            return;
        }
        sortColumn = column;
        sortAscending = true;
        invalidateFilterCache();
    }

    public boolean isSortedBy(String column) {
        return Strings.CS.equals(sortColumn, column);
    }

    public void toggleColumnsMenu() {
        columnsMenuOpen = !columnsMenuOpen;
    }

    public void closeColumnsMenu() {
        columnsMenuOpen = false;
    }

    public void toggleColumn(String key) {
        if ("name".equals(key)) {
            return;
        }
        for (ColumnCfg cfg : columnCfg) {
            if (Strings.CS.equals(cfg.getKey(), key)) {
                cfg.setOn(!cfg.isOn());
                return;
            }
        }
    }

    public void moveColumnUp(String key) {
        moveColumn(key, -1);
    }

    public void moveColumnDown(String key) {
        moveColumn(key, 1);
    }

    public void resetColumns() {
        columnCfg = defaultColumnCfg();
        if (!isLoggedIn()) {
            applyGuestColumnDefaults();
        }
        invalidateFilterCache();
    }

    private void applyGuestColumnDefaults() {
        for (ColumnCfg cfg : columnCfg) {
            if (isGuestHiddenColumn(cfg.getKey())) {
                cfg.setOn(false);
            }
        }
        if (isGuestHiddenColumn(sortColumn)) {
            sortColumn = "name";
            sortAscending = true;
        }
    }

    private static boolean isGuestHiddenColumn(String key) {
        return "access".equals(key) || "role".equals(key);
    }

    public void showListFlash(String message) {
        showListFlash(message, false);
    }

    public void showListFlash(String message, boolean error) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        listFlashMessage = message.trim();
        listFlashToken = Long.toString(System.currentTimeMillis());
        listFlashError = error;
    }

    public void clearListFlash() {
        listFlashMessage = null;
        listFlashToken = null;
        listFlashError = false;
    }

    public void openThesaurus() throws IOException {
        FacesContext faces = FacesContext.getCurrentInstance();
        String thesaurusId = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("openThesaurusId")
                : null;
        openThesaurus(thesaurusId);
    }

    public void openThesaurus(String thesaurusId) throws IOException {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        consultationShellBean.setSelectedThesaurusId(thesaurusId.trim());
        consultationShellBean.onThesaurusChange();
    }

    /** Lien GET absolu vers l'accueil détail du thésaurus. */
    public String browseUrl(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return "#";
        }
        FacesContext faces = FacesContext.getCurrentInstance();
        String ctx = faces != null ? faces.getExternalContext().getRequestContextPath() : "";
        return ctx + "/v2/index.xhtml?idt=" + thesaurusId.trim();
    }

    public void createThesaurus() {
        if (!isCanCreateThesaurus()) {
            return;
        }
        cancelImportThesaurus();
        cancelEditThesaurus();
        cancelExportThesaurus();
        if (newThesaurusService == null) {
            MessageUtils.showErrorMessage("Service de création indisponible — rechargez la page.");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        var options = newThesaurusService.loadFormOptions(
                userId != null ? userId : -1,
                userSession.isSuperAdmin()
        );
        createEditor = NewThesaurusEditor.empty();
        createLanguages = options.languages() != null ? options.languages() : Collections.emptyList();
        createProjects = options.projects() != null ? options.projects() : Collections.emptyList();
        createSuperAdmin = options.superAdmin();
        if (!createLanguages.isEmpty()) {
            String uiLang = v2LocaleBean.getIdLangue();
            boolean uiFound = createLanguages.stream().anyMatch(l -> Strings.CI.equals(l.code(), uiLang));
            createEditor.setSelectedLanguage(uiFound ? uiLang : createLanguages.get(0).code());
        }
        if (!createSuperAdmin && createProjects.size() == 1) {
            createEditor.setSelectedProjectId(String.valueOf(createProjects.get(0).id()));
        }
        createMode = true;
        columnsMenuOpen = false;
    }

    public void cancelCreateThesaurus() {
        createMode = false;
        createEditor = NewThesaurusEditor.empty();
        createLanguages = Collections.emptyList();
        createProjects = Collections.emptyList();
    }

    public void openEditThesaurus() {
        FacesContext faces = FacesContext.getCurrentInstance();
        String thesaurusId = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("editThesaurusId")
                : null;
        openEditThesaurus(thesaurusId);
    }

    public void openEditThesaurus(String thesaurusId) {
        editError = null;
        if (StringUtils.isBlank(thesaurusId) || !assertCanManage(thesaurusId)) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("v2.picker.edit.denied"));
            return;
        }
        try {
            var details = modifyThesaurusService.loadDetails(thesaurusId.trim());
            cancelCreateThesaurus();
            cancelImportThesaurus();
            cancelExportThesaurus();
            editThesaurusId = details.id();
            editTitle = StringUtils.defaultString(details.title());
            editLanguage = StringUtils.defaultIfBlank(details.sourceLang(), v2LocaleBean.getIdLangue());
            editPrivateThesaurus = details.privateThesaurus();
            editMode = true;
            columnsMenuOpen = false;
        } catch (InvalidToolboxDataException ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(
                    ex.getMessage(), v2LocaleBean.getMsg("v2.picker.edit.failed")));
        }
    }

    public void cancelEditThesaurus() {
        editMode = false;
        editThesaurusId = null;
        editTitle = null;
        editLanguage = null;
        editPrivateThesaurus = false;
        editError = null;
    }

    public void chooseEditPublicVisibility() {
        editPrivateThesaurus = false;
    }

    public void chooseEditPrivateVisibility() {
        editPrivateThesaurus = true;
    }

    public void submitEditThesaurus() {
        editError = null;
        if (!editMode || StringUtils.isBlank(editThesaurusId) || !assertCanManage(editThesaurusId)) {
            editError = v2LocaleBean.getMsg("v2.picker.edit.denied");
            return;
        }
        if (StringUtils.isBlank(editTitle)) {
            editError = v2LocaleBean.getMsg("v2.picker.edit.titleRequired");
            return;
        }
        try {
            String creator = StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "user");
            modifyThesaurusService.updateLanguage(
                    editThesaurusId,
                    StringUtils.defaultIfBlank(editLanguage, v2LocaleBean.getIdLangue()),
                    editTitle.trim(),
                    creator
            );
            modifyThesaurusService.changeVisibility(editThesaurusId, editPrivateThesaurus);
            String ok = v2LocaleBean.getMsg("v2.picker.edit.success");
            MessageUtils.showInformationMessage(ok);
            showListFlash(ok);
            cancelEditThesaurus();
            load();
        } catch (InvalidToolboxDataException ex) {
            editError = StringUtils.defaultIfBlank(ex.getMessage(), v2LocaleBean.getMsg("v2.picker.edit.failed"));
        }
    }

    public void prepareDeleteThesaurus() {
        FacesContext faces = FacesContext.getCurrentInstance();
        String thesaurusId = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("deleteThesaurusId")
                : null;
        prepareDeleteThesaurus(thesaurusId);
    }

    public void prepareDeleteThesaurus(String thesaurusId) {
        deleteThesaurusId = null;
        deleteThesaurusTitle = null;
        deletePerennialIdentifiers = false;
        if (StringUtils.isBlank(thesaurusId) || !assertCanManage(thesaurusId)) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("v2.picker.delete.denied"));
            return;
        }
        String id = thesaurusId.trim();
        deleteThesaurusId = id;
        deleteThesaurusTitle = rows.stream()
                .filter(row -> Strings.CS.equals(row.id(), id))
                .map(ThesaurusPickerRow::name)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(id);
        try {
            var details = modifyThesaurusService.loadDetails(id);
            if (StringUtils.isNotBlank(details.title())) {
                deleteThesaurusTitle = details.title();
            }
        } catch (InvalidToolboxDataException ignored) {
            // keep list title / id
        }
    }

    public void deleteThesaurus() {
        if (StringUtils.isBlank(deleteThesaurusId) || !assertCanManage(deleteThesaurusId)) {
            String denied = v2LocaleBean.getMsg("v2.picker.delete.denied");
            MessageUtils.showErrorMessage(denied);
            showListFlash(denied, true);
            clearDeleteState();
            return;
        }
        String id = deleteThesaurusId;
        String title = StringUtils.defaultIfBlank(deleteThesaurusTitle, id);
        try {
            editionThesaurusService.deleteThesaurus(id, deletePerennialIdentifiers);
            if (consultationShellBean != null
                    && Strings.CS.equals(consultationShellBean.getSelectedThesaurusId(), id)) {
                consultationShellBean.setSelectedThesaurusId(null);
            }
            String ok = v2LocaleBean.getMsg("v2.picker.delete.success") + " (« " + title + " »)";
            MessageUtils.showInformationMessage(ok);
            showListFlash(ok, false);
            clearDeleteState();
            if (editMode && Strings.CS.equals(editThesaurusId, id)) {
                cancelEditThesaurus();
            }
            load();
        } catch (InvalidToolboxDataException ex) {
            String fail = StringUtils.defaultIfBlank(
                    ex.getMessage(), v2LocaleBean.getMsg("v2.picker.delete.failed"));
            MessageUtils.showErrorMessage(fail);
            showListFlash(fail, true);
            clearDeleteState();
        }
    }

    public void cancelDeleteThesaurus() {
        clearDeleteState();
    }

    private void clearDeleteState() {
        deleteThesaurusId = null;
        deleteThesaurusTitle = null;
        deletePerennialIdentifiers = false;
    }

    public boolean isDeleteReady() {
        return StringUtils.isNotBlank(deleteThesaurusId);
    }

    public void submitCreateThesaurus() throws IOException {
        if (!isCanCreateThesaurus()) {
            return;
        }
        if (createEditor == null || StringUtils.isBlank(createEditor.getTitle())) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("v2.picker.create.nameRequired"));
            return;
        }
        try {
            String thesaurusId = newThesaurusService.create(
                    createEditor.toRequest(),
                    StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "user"),
                    userSession.getCurrentUserId()
            );
            String ok = v2LocaleBean.getMsg("v2.picker.create.success");
            MessageUtils.showInformationMessage(ok);
            showListFlash(ok);
            createMode = false;
            load();
            openThesaurus(thesaurusId);
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void choosePublicVisibility() {
        createEditor.setPrivateThesaurus(false);
    }

    public void choosePrivateVisibility() {
        createEditor.setPrivateThesaurus(true);
    }

    public String getCreateVisibility() {
        return createEditor != null && createEditor.isPrivateThesaurus() ? "private" : "public";
    }

    public void setCreateVisibility(String visibility) {
        if (createEditor == null) {
            createEditor = NewThesaurusEditor.empty();
        }
        createEditor.setPrivateThesaurus(Strings.CI.equals(visibility, "private"));
    }

    public void selectCreateLanguage() {
        if (createEditor == null) {
            createEditor = NewThesaurusEditor.empty();
        }
        FacesContext faces = FacesContext.getCurrentInstance();
        String code = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("createLangCode")
                : null;
        createEditor.setSelectedLanguage(StringUtils.defaultString(code));
    }

    public void selectCreateLanguage(String code) {
        if (createEditor == null) {
            createEditor = NewThesaurusEditor.empty();
        }
        createEditor.setSelectedLanguage(StringUtils.defaultString(code));
    }

    public boolean isCreateLanguageSelected(String code) {
        return createEditor != null && Strings.CI.equals(createEditor.getSelectedLanguage(), code);
    }

    public String getCreateSelectedLanguageFlag() {
        return languageFlag(createEditor != null ? createEditor.getSelectedLanguage() : null);
    }

    public String getCreateSelectedLanguageLabel() {
        return languageLabel(createEditor != null ? createEditor.getSelectedLanguage() : null);
    }

    public String languageOptionForCreate(LanguageOption lang) {
        if (lang == null) {
            return "";
        }
        return languageOptionLabel(lang.code());
    }

    private void moveColumn(String key, int delta) {
        int index = -1;
        for (int i = 0; i < columnCfg.size(); i++) {
            if (Strings.CS.equals(columnCfg.get(i).getKey(), key)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        int target = index + delta;
        if (target < 0 || target >= columnCfg.size()) {
            return;
        }
        ColumnCfg item = columnCfg.remove(index);
        columnCfg.add(target, item);
    }

    private boolean matchesTab(ThesaurusPickerRow row, String tab) {
        if (TAB_MEMBER.equals(tab)) {
            return row.isMember();
        }
        if (TAB_PUBLIC.equals(tab)) {
            return row.isPublicAccess();
        }
        return true;
    }

    private boolean matchesQuery(ThesaurusPickerRow row, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        return normalize(row.name()).contains(needle)
                || normalize(row.id()).contains(needle)
                || normalize(row.projects()).contains(needle)
                || normalize(row.domain()).contains(needle)
                || normalize(row.organization()).contains(needle)
                || normalize(row.chronology()).contains(needle);
    }

    private boolean matchesColumnFilters(ThesaurusPickerRow row) {
        if (!normalize(row.name()).contains(normalize(filterName))
                && !normalize(row.id()).contains(normalize(filterName))) {
            return false;
        }
        if (!normalize(row.domain()).contains(normalize(filterDomain))) {
            return false;
        }
        if (!normalize(row.projects()).contains(normalize(filterProjects))) {
            return false;
        }
        if (!normalize(row.organization()).contains(normalize(filterOrg))) {
            return false;
        }
        if (StringUtils.isNotBlank(filterLang)) {
            List<String> langs = row.languages() == null ? List.of() : row.languages();
            boolean any = langs.stream().anyMatch(lang -> Strings.CI.equals(lang, filterLang));
            if (!any) {
                return false;
            }
        }
        return true;
    }

    private Comparator<ThesaurusPickerRow> comparator() {
        Comparator<ThesaurusPickerRow> base = switch (StringUtils.defaultString(sortColumn)) {
            case "terms" -> Comparator.comparingLong(ThesaurusPickerRow::terms);
            case "created" -> Comparator.comparing(
                    ThesaurusPickerRow::created, Comparator.nullsLast(Comparator.naturalOrder()));
            case "projects" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.projects()), String.CASE_INSENSITIVE_ORDER);
            case "access" -> Comparator.comparing(ThesaurusPickerRow::isPrivateThesaurus);
            case "role" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.roleLabel()), String.CASE_INSENSITIVE_ORDER);
            case "domain" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.domain()), String.CASE_INSENSITIVE_ORDER);
            case "org" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.organization()), String.CASE_INSENSITIVE_ORDER);
            case "chrono" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.chronology()), String.CASE_INSENSITIVE_ORDER);
            case "lang" -> Comparator.comparing(
                    row -> String.join(",", row.languages() == null ? List.of() : row.languages()),
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.name()), String.CASE_INSENSITIVE_ORDER);
        };
        return sortAscending ? base : base.reversed();
    }

    private static List<ColumnCfg> defaultColumnCfg() {
        List<ColumnCfg> cfg = new ArrayList<>();
        for (ColumnDef def : COLUMN_DEFS) {
            cfg.add(new ColumnCfg(def.key(), true));
        }
        return cfg;
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).toLowerCase(Locale.ROOT).trim();
    }

    private record ColumnDef(String key, String labelKey, boolean filterable, boolean grow) {
    }

    @Getter
    @Setter
    public static class ColumnCfg implements Serializable {
        private String key;
        private boolean on;

        public ColumnCfg() {
        }

        public ColumnCfg(String key, boolean on) {
            this.key = key;
            this.on = on;
        }

        public void setOn(boolean on) {
            if ("name".equals(key)) {
                this.on = true;
                return;
            }
            this.on = on;
        }

        public boolean isNameLocked() {
            return "name".equals(key);
        }
    }
}
