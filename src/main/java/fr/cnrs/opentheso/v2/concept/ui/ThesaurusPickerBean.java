package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusPickerRow;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.shared.ui.V2NavigationBean;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
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
            new ColumnDef("name", "Nom du thésaurus", true, true),
            new ColumnDef("access", "Accès", false, false),
            new ColumnDef("terms", "Termes", false, false),
            new ColumnDef("created", "Créé le", false, false),
            new ColumnDef("domain", "Domaine", true, false),
            new ColumnDef("projects", "Projets", true, false),
            new ColumnDef("org", "Organisation", true, false),
            new ColumnDef("chrono", "Chronologie", false, false),
            new ColumnDef("lang", "Langues", false, false)
    );

    private final transient ConsultationCatalogService consultationCatalogService;
    private final transient ConsultationShellBean consultationShellBean;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient V2NavigationBean v2NavigationBean;
    private final transient NewThesaurusService newThesaurusService;

    private List<ThesaurusPickerRow> rows = List.of();
    private String query = "";
    private String activeTab = TAB_ALL;
    private String sortColumn = "name";
    private boolean sortAscending = true;

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

    @PostConstruct
    public void init() {
        if (!userSession.isLoggedIn()) {
            activeTab = TAB_PUBLIC;
        }
        load();
    }

    public void load() {
        consultationShellBean.clearThesaurusAccessDenied();
        consultationShellBean.load();
        rows = consultationCatalogService.listPickerThesauri(
                userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                userSession.isSuperAdmin(),
                v2LocaleBean.getIdLangue()
        );
    }

    public List<ThesaurusPickerRow> getFilteredRows() {
        String needle = normalize(query);
        return rows.stream()
                .filter(row -> matchesTab(row, activeTab))
                .filter(row -> matchesQuery(row, needle))
                .filter(this::matchesColumnFilters)
                .sorted(comparator())
                .toList();
    }

    public int getMemberCount() {
        return (int) rows.stream().filter(ThesaurusPickerRow::isMember).count();
    }

    public int getPublicCount() {
        return (int) rows.stream().filter(ThesaurusPickerRow::isPublicAccess).count();
    }

    /** Nombre de lignes après recherche, tous onglets confondus (badge « Tous »). */
    public int getQueryMatchCount() {
        String needle = normalize(query);
        return (int) rows.stream().filter(row -> matchesQuery(row, needle)).count();
    }

    public int getFilteredCount() {
        return getFilteredRows().size();
    }

    public int getEmptyColspan() {
        return getVisibleColumns().size() + 2;
    }

    public boolean isLoggedIn() {
        return userSession.isLoggedIn();
    }

    public boolean isCanCreateThesaurus() {
        return userSession.isLoggedIn();
    }

    public List<ColumnCfg> getColumnCfg() {
        return columnCfg;
    }

    public List<ColumnCfg> getVisibleColumns() {
        return columnCfg.stream().filter(ColumnCfg::isOn).toList();
    }

    public boolean isColumnVisible(String key) {
        return columnCfg.stream().anyMatch(c -> c.isOn() && Strings.CS.equals(c.getKey(), key));
    }

    public String getColumnLabel(String key) {
        return COLUMN_DEFS.stream()
                .filter(d -> Strings.CS.equals(d.key(), key))
                .map(ColumnDef::label)
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
        return switch (code) {
            case "fr" -> "Français";
            case "en" -> "English";
            case "de" -> "Deutsch";
            case "es" -> "Español";
            case "it" -> "Italiano";
            case "nl" -> "Nederlands";
            case "pt" -> "Português";
            case "ar" -> "العربية";
            case "ca" -> "Català";
            case "eu" -> "Euskara";
            case "el" -> "Ελληνικά";
            case "la" -> "Latina";
            case "zh" -> "中文";
            case "ja" -> "日本語";
            case "ru" -> "Русский";
            case "pl" -> "Polski";
            case "tr" -> "Türkçe";
            case "sv" -> "Svenska";
            case "da" -> "Dansk";
            case "no", "nb", "nn" -> "Norsk";
            case "fi" -> "Suomi";
            case "cs" -> "Čeština";
            case "hu" -> "Magyar";
            case "ro" -> "Română";
            case "uk" -> "Українська";
            case "he" -> "עברית";
            case "fa" -> "فارسی";
            case "hi" -> "हिन्दी";
            case "ko" -> "한국어";
            case "gl" -> "Galego";
            case "cy" -> "Cymraeg";
            default -> code.toUpperCase(Locale.ROOT);
        };
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
        }
    }

    public void clearQuery() {
        query = "";
    }

    public void toggleSort(String column) {
        if (Strings.CS.equals(sortColumn, column)) {
            sortAscending = !sortAscending;
            return;
        }
        sortColumn = column;
        sortAscending = true;
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

    public void submitCreateThesaurus() throws IOException {
        if (!isCanCreateThesaurus()) {
            return;
        }
        if (createEditor == null || StringUtils.isBlank(createEditor.getTitle())) {
            MessageUtils.showErrorMessage("Le nom du thésaurus est obligatoire.");
            return;
        }
        try {
            String thesaurusId = newThesaurusService.create(
                    createEditor.toRequest(),
                    StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "user"),
                    userSession.getCurrentUserId()
            );
            MessageUtils.showInformationMessage("Thésaurus créé avec succès");
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
            case "access" -> Comparator.comparing(
                    row -> StringUtils.defaultString(row.access()), String.CASE_INSENSITIVE_ORDER);
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

    private record ColumnDef(String key, String label, boolean filterable, boolean grow) {
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
