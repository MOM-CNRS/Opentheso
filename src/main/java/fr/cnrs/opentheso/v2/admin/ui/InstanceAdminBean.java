package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_USERS, SECTION_THESAURI, SECTION_SERVER, SECTION_STATS
    );
    private static final DateTimeFormatter LAST_LOGIN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient AdminCatalogService adminCatalogService;
    private final transient AdminUserService adminUserService;

    private String section;
    private String openThesaurusId;
    private String userQuery = "";

    private List<InstanceAdminAccount> accounts = Collections.emptyList();
    private List<AdminThesaurus> thesauri = Collections.emptyList();

    private boolean createUserOpen;
    private String newUsername;
    private String newEmail;
    private String newPassword;
    private String newPasswordConfirmation;
    private boolean newAlertMail;
    private boolean newSuperAdmin;
    private String createUserError;

    @PostConstruct
    public void init() {
        if (!isAccessAllowed()) {
            clear();
            return;
        }
        reload();
        applySectionFromRequest();
    }

    public boolean isAccessAllowed() {
        return userSession.canAccessSuperAdminScreen();
    }

    public void reload() {
        if (!isAccessAllowed()) {
            clear();
            return;
        }
        boolean superAdmin = userSession.isSuperAdmin();
        accounts = adminCatalogService.listInstanceAccounts(superAdmin);
        thesauri = adminCatalogService.listAllThesauri(superAdmin, v2LocaleBean.getIdLangue());
    }

    public void openHome() {
        section = null;
        openThesaurusId = null;
        userQuery = "";
        closeCreateUser();
    }

    public void openSection(String key) {
        if (!isAccessAllowed()) {
            return;
        }
        section = key;
        openThesaurusId = null;
        userQuery = "";
        closeCreateUser();
        if (SECTION_USERS.equals(key) || SECTION_THESAURI.equals(key)) {
            reload();
        }
    }

    public void openThesaurusMembers(String thesaurusId) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        section = SECTION_THESAURI;
        openThesaurusId = thesaurusId;
        closeCreateUser();
    }

    public void goBack() {
        if (createUserOpen) {
            closeCreateUser();
            return;
        }
        if (StringUtils.isNotBlank(openThesaurusId)) {
            openThesaurusId = null;
            return;
        }
        if (StringUtils.isNotBlank(section)) {
            openHome();
        }
    }

    /** URL hub admin (navigation GET, sans AJAX). */
    public String getHomeUrl() {
        return contextPath() + "/v2/admin/instance";
    }

    /** URL d'une sous-section (?section=…). */
    public String sectionUrl(String key) {
        if (StringUtils.isBlank(key)) {
            return getHomeUrl();
        }
        return getHomeUrl() + "?section=" + encode(key);
    }

    /** URL membres d'un thésaurus (?section=thes&amp;th=…). */
    public String thesaurusMembersUrl(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return sectionUrl(SECTION_THESAURI);
        }
        return sectionUrl(SECTION_THESAURI) + "&th=" + encode(thesaurusId);
    }

    /**
     * Cible du bouton retour hors formulaire de création.
     * Navigation pleine page pour éviter l'état AJAX cassé.
     */
    public String getBackHref() {
        if (isThesaurusMembers()) {
            return sectionUrl(SECTION_THESAURI);
        }
        if (!isHome()) {
            return getHomeUrl();
        }
        return getHomeUrl();
    }

    public void openCreateUser() {
        if (!isAccessAllowed()) {
            return;
        }
        createUserOpen = true;
        createUserError = null;
        newUsername = null;
        newEmail = null;
        newPassword = null;
        newPasswordConfirmation = null;
        newAlertMail = false;
        newSuperAdmin = false;
    }

    public void closeCreateUser() {
        createUserOpen = false;
        createUserError = null;
        newUsername = null;
        newEmail = null;
        newPassword = null;
        newPasswordConfirmation = null;
        newAlertMail = false;
        newSuperAdmin = false;
    }

    public void createUser() {
        if (!isAccessAllowed()) {
            return;
        }
        createUserError = null;
        try {
            Integer roleId = newSuperAdmin ? ProjectAccessPolicy.ROLE_SUPER_ADMIN : null;
            adminUserService.createUser(new AdminUserService.CreateUserRequest(
                    userSession.isSuperAdmin(),
                    newUsername,
                    newEmail,
                    newAlertMail,
                    roleId,
                    null,
                    false,
                    Collections.emptyList(),
                    newPassword,
                    newPasswordConfirmation
            ));
            MessageUtils.showInformationMessage(v2LocaleBean.getMsg("profile.userCreatedSuccess"));
            closeCreateUser();
            reload();
        } catch (InvalidProfileDataException | InvalidPasswordException | InvalidProjectDataException e) {
            createUserError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
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
        if (createUserOpen) {
            return v2LocaleBean.getMsg("v2.admin.users.create.cancel");
        }
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

    /** Applique ?section= / ?th= (tests et init). */
    void applySectionFromRequest(String sectionParam, String thesaurusId) {
        if (StringUtils.isBlank(sectionParam) || !KNOWN_SECTIONS.contains(sectionParam)) {
            return;
        }
        openSection(sectionParam);
        if (SECTION_THESAURI.equals(sectionParam) && StringUtils.isNotBlank(thesaurusId)) {
            openThesaurusMembers(thesaurusId);
        }
    }

    private void applySectionFromRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        applySectionFromRequest(params.get("section"), params.get("th"));
    }

    private String contextPath() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return "";
        }
        return StringUtils.defaultString(facesContext.getExternalContext().getRequestContextPath());
    }

    private static String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private void clear() {
        section = null;
        openThesaurusId = null;
        userQuery = "";
        accounts = Collections.emptyList();
        thesauri = Collections.emptyList();
        closeCreateUser();
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
