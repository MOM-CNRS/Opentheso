package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@ViewScoped
@Named("v2InstanceAdminBean")
@RequiredArgsConstructor
public class InstanceAdminBean implements Serializable {

    public static final String SECTION_USERS = "users";
    public static final String SECTION_THESAURI = "thes";
    public static final String SECTION_SERVER = "server";
    public static final String SECTION_STATS = "stats";

    private static final DateTimeFormatter LAST_LOGIN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient AdminCatalogService adminCatalogService;

    private String section;
    private String openThesaurusId;
    private String userQuery = "";

    private List<InstanceAdminAccount> accounts = Collections.emptyList();
    private List<AdminThesaurus> thesauri = Collections.emptyList();

    @PostConstruct
    public void init() {
        if (!userSession.canAccessSuperAdminScreen()) {
            clear();
            return;
        }
        reload();
    }

    public void reload() {
        if (!userSession.canAccessSuperAdminScreen()) {
            clear();
            return;
        }
        accounts = adminCatalogService.listInstanceAccounts(true);
        thesauri = adminCatalogService.listAllThesauri(true, v2LocaleBean.getIdLangue());
    }

    public void openHome() {
        section = null;
        openThesaurusId = null;
        userQuery = "";
    }

    public void openSection(String key) {
        if (!userSession.canAccessSuperAdminScreen()) {
            return;
        }
        section = key;
        openThesaurusId = null;
        userQuery = "";
        if (SECTION_USERS.equals(key) || SECTION_THESAURI.equals(key)) {
            reload();
        }
    }

    public void openThesaurusMembers(String thesaurusId) {
        if (!userSession.canAccessSuperAdminScreen() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        section = SECTION_THESAURI;
        openThesaurusId = thesaurusId;
    }

    public void goBack() {
        if (StringUtils.isNotBlank(openThesaurusId)) {
            openThesaurusId = null;
            return;
        }
        if (StringUtils.isNotBlank(section)) {
            openHome();
        }
    }

    public boolean isHome() {
        return StringUtils.isBlank(section);
    }

    public boolean isUsersSection() {
        return SECTION_USERS.equals(section);
    }

    public boolean isThesauriSection() {
        return SECTION_THESAURI.equals(section) && StringUtils.isBlank(openThesaurusId);
    }

    public boolean isThesaurusMembers() {
        return SECTION_THESAURI.equals(section) && StringUtils.isNotBlank(openThesaurusId);
    }

    public boolean isPlaceholderSection() {
        return SECTION_SERVER.equals(section) || SECTION_STATS.equals(section);
    }

    public String getPageTitle() {
        if (isThesaurusMembers()) {
            return "Membres de " + thesaurusTitle(openThesaurusId);
        }
        if (isHome()) {
            return "Administration de l'instance";
        }
        return sectionLabel(section);
    }

    public String getBackTitle() {
        if (isThesaurusMembers()) {
            return "Retour à " + sectionLabel(SECTION_THESAURI);
        }
        if (!isHome()) {
            return "Retour à l'administration";
        }
        return "Retour à la sélection de thésaurus";
    }

    public String getSectionLabel() {
        return sectionLabel(section);
    }

    public List<InstanceAdminAccount> getFilteredAccounts() {
        String needle = normalize(userQuery);
        if (needle.isEmpty()) {
            return accounts;
        }
        return accounts.stream()
                .filter(a -> normalize(a.username()).contains(needle)
                        || normalize(a.email()).contains(needle)
                        || normalize(a.organization()).contains(needle))
                .toList();
    }

    public int getAccountCount() {
        return accounts == null ? 0 : accounts.size();
    }

    public int getFilteredAccountCount() {
        return getFilteredAccounts().size();
    }

    public int getThesaurusCount() {
        return thesauri == null ? 0 : thesauri.size();
    }

    public String getHomeSubtitle() {
        return "Gérez les comptes, les thésaurus et les réglages de la plateforme.";
    }

    public String formatLastLogin(InstanceAdminAccount account) {
        if (account == null || account.lastLogin() == null) {
            return "—";
        }
        return LAST_LOGIN_FMT.format(account.lastLogin());
    }

    public String thesaurusTitle(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return "";
        }
        return thesauri.stream()
                .filter(t -> Strings.CI.equals(t.id(), thesaurusId))
                .map(AdminThesaurus::title)
                .findFirst()
                .orElse(thesaurusId);
    }

    private void clear() {
        section = null;
        openThesaurusId = null;
        userQuery = "";
        accounts = Collections.emptyList();
        thesauri = Collections.emptyList();
    }

    private static String sectionLabel(String key) {
        if (key == null) {
            return "";
        }
        return switch (key) {
            case SECTION_USERS -> "Utilisateurs de l'instance";
            case SECTION_THESAURI -> "Thésaurus & membres";
            case SECTION_SERVER -> "Paramètres serveur";
            case SECTION_STATS -> "Statistiques de l'instance";
            default -> key;
        };
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }
}
