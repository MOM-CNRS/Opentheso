package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurusOption;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2AllUsersBean")
public class AllUsersBean implements Serializable {

    private final UserSession userSession;
    private final V2LocaleBean localeBean;
    private final AdminCatalogService adminCatalogService;
    private final AdminUserService adminUserService;
    private final UserProfileService userProfileService;

    private List<AdminUserMembership> users = Collections.emptyList();
    private List<ProjectSummary> projects = Collections.emptyList();
    private List<AssignableRole> roles = Collections.emptyList();

    private String newUsername;
    private String newEmail;
    private boolean newAlertMail;
    private Integer newRoleId;
    private Integer newProjectId;
    private boolean newLimitedOnThesaurus;
    private List<AdminThesaurusOption> newProjectThesauri = Collections.emptyList();
    private List<String> newSelectedThesaurusIds = Collections.emptyList();
    private String newPassword;
    private String newPasswordConfirmation;

    private Integer selectedUserId;
    private String editUsername;
    private String editEmail;
    private boolean editAlertMail;
    private boolean editHasApiKey;
    private boolean editKeyNeverExpire;
    private LocalDate editApiKeyExpiresAt;
    private String resetPassword;
    private String resetPasswordConfirmation;

    public AllUsersBean(
            UserSession userSession,
            V2LocaleBean localeBean,
            AdminCatalogService adminCatalogService,
            AdminUserService adminUserService,
            fr.cnrs.opentheso.v2.user.service.UserProfileService userProfileService
    ) {
        this.userSession = userSession;
        this.localeBean = localeBean;
        this.adminCatalogService = adminCatalogService;
        this.adminUserService = adminUserService;
        this.userProfileService = userProfileService;
    }

    public void load() {
        if (!userSession.canAccessSuperAdminScreen()) {
            clearState();
            return;
        }
        users = new ArrayList<>(adminCatalogService.listAllUsers(true));
        projects = new ArrayList<>(adminCatalogService.listAllProjects(true));
        roles = adminCatalogService.listAssignableRoles(true);
    }

    public void prepareCreateDialog() {
        newUsername = null;
        newEmail = null;
        newAlertMail = false;
        newRoleId = null;
        newProjectId = null;
        newLimitedOnThesaurus = false;
        newProjectThesauri = Collections.emptyList();
        newSelectedThesaurusIds = Collections.emptyList();
        newPassword = null;
        newPasswordConfirmation = null;
    }

    public void onNewProjectChange() {
        newLimitedOnThesaurus = false;
        newSelectedThesaurusIds = Collections.emptyList();
        if (newProjectId == null || !userSession.canAccessSuperAdminScreen()) {
            newProjectThesauri = Collections.emptyList();
            return;
        }
        newProjectThesauri = adminCatalogService.listThesauriOfProject(true, newProjectId);
    }

    public void prepareEditDialog(int userId) {
        selectedUserId = userId;
        var profile = userProfileService.getProfile(userId);
        editUsername = profile.username();
        editEmail = profile.email();
        editAlertMail = profile.alertMail();
        // Autorisation ≠ présence d'une clé générée (hasApiKey).
        editHasApiKey = ApiKeyPolicy.isSectionVisible(profile);
        editKeyNeverExpire = profile.keyNeverExpire();
        editApiKeyExpiresAt = profile.keyExpiresAt();
    }

    public void onEditHasApiKeyChange() {
        if (editHasApiKey && !editKeyNeverExpire && editApiKeyExpiresAt == null) {
            editKeyNeverExpire = true;
        }
        if (!editHasApiKey) {
            editKeyNeverExpire = false;
            editApiKeyExpiresAt = null;
        }
    }

    public void preparePasswordDialog(int userId) {
        selectedUserId = userId;
        resetPassword = null;
        resetPasswordConfirmation = null;
    }

    public void prepareDeleteDialog(int userId, String username) {
        selectedUserId = userId;
        editUsername = username;
    }

    public void createUser() {
        if (!requireSuperAdmin()) {
            return;
        }
        try {
            adminUserService.createUser(
                    true,
                    newUsername,
                    newEmail,
                    newAlertMail,
                    newRoleId,
                    newProjectId,
                    newLimitedOnThesaurus,
                    newSelectedThesaurusIds,
                    newPassword,
                    newPasswordConfirmation
            );
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.userCreatedSuccess"));
            prepareCreateDialog();
            load();
            PrimeFaces.current().executeScript("PF('v2CreateUser').hide();");
            refreshPage();
        } catch (InvalidProfileDataException | InvalidPasswordException | InvalidProjectDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void updateUser() {
        if (!requireSuperAdmin() || selectedUserId == null) {
            return;
        }
        try {
            adminUserService.updateUser(true, selectedUserId, editUsername, editEmail, editAlertMail);
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.usernameChangedSuccess"));
            load();
            PrimeFaces.current().executeScript("PF('v2EditUser').hide();");
            refreshPage();
        } catch (InvalidProfileDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void updateApiKey() {
        if (!requireSuperAdmin() || selectedUserId == null) {
            return;
        }
        try {
            adminUserService.updateApiKeySettings(
                    true, selectedUserId, editHasApiKey, editKeyNeverExpire, editApiKeyExpiresAt);
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.apiKeyUpdatedSuccess"));
            PrimeFaces.current().executeScript("PF('v2EditUser').hide();");
        } catch (InvalidProfileDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void resetUserPassword() {
        if (!requireSuperAdmin() || selectedUserId == null) {
            return;
        }
        try {
            adminUserService.updatePassword(true, selectedUserId, resetPassword, resetPasswordConfirmation);
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.passwordChangedSuccess"));
            PrimeFaces.current().executeScript("PF('v2ResetPassword').hide();");
        } catch (InvalidPasswordException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void deleteUser() {
        if (!requireSuperAdmin() || selectedUserId == null) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            adminUserService.deleteUser(true, selectedUserId, callerId);
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.userDeletedSuccess"));
            load();
            PrimeFaces.current().executeScript("PF('v2DeleteUser').hide();");
            refreshPage();
        } catch (InvalidProfileDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public boolean isSuperAdminScreen() {
        return userSession.canAccessSuperAdminScreen();
    }

    private boolean requireSuperAdmin() {
        if (userSession.canAccessSuperAdminScreen()) {
            return true;
        }
        MessageUtils.showErrorMessage(localeBean.getMsg("project.accessDeniedDetail"));
        return false;
    }

    private void clearState() {
        users = Collections.emptyList();
        projects = Collections.emptyList();
        roles = Collections.emptyList();
    }

    private void refreshPage() {
        PrimeFaces.current().ajax().update("containerIndex");
    }
}
