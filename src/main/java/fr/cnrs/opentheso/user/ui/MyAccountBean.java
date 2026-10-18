package fr.cnrs.opentheso.user.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.user.model.UserProfile;
import fr.cnrs.opentheso.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.user.service.UserApiKeyService;
import fr.cnrs.opentheso.user.service.UserPasswordService;
import fr.cnrs.opentheso.user.service.UserProfileService;
import fr.cnrs.opentheso.user.validation.PasswordPolicy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("myAccountBean")
public class MyAccountBean implements Serializable {

    private static final String HEADER_UPDATE_TARGET = "containerIndex";

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient UserProfileService userProfileService;
    private final transient UserApiKeyService userApiKeyService;
    private final transient UserPasswordService userPasswordService;

    private UserProfile profile;
    private List<ProjectRoleOverview> projectRoles = Collections.emptyList();
    private String editableUsername;
    private String editableEmail;
    private boolean editableAlertMail;
    private String newPassword;
    private String passwordConfirmation;
    private String apiKeyPlain;
    private boolean identitySaveError;
    private String identitySaveMessage;
    private boolean loaded;

    public MyAccountBean(
            UserSession userSession,
            LocaleBean localeBean,
            UserProfileService userProfileService,
            UserApiKeyService userApiKeyService,
            UserPasswordService userPasswordService
    ) {
        this.userSession = userSession;
        this.localeBean = localeBean;
        this.userProfileService = userProfileService;
        this.userApiKeyService = userApiKeyService;
        this.userPasswordService = userPasswordService;
    }

    public void load() {
        doLoad();
    }

    public String getEditableUsername() {
        doLoad();
        return editableUsername;
    }

    public String getEditableEmail() {
        doLoad();
        return editableEmail;
    }

    public List<ProjectRoleOverview> getProjectRoles() {
        doLoad();
        return projectRoles;
    }

    public void updateUsername() {
        applyProfileUpdate(
                userId -> userProfileService.updateUsername(userId, editableUsername),
                localeBean.getMsg("profile.usernameChangedSuccess"),
                () -> userSession.refreshDisplayName(profile.username())
        );
    }

    public void updateEmail() {
        applyProfileUpdate(
                userId -> userProfileService.updateEmail(userId, editableEmail),
                localeBean.getMsg("profile.emailChangedSuccess"),
                () -> userSession.refreshEmail(profile.email())
        );
    }

    public void updateAlertMail() {
        applyProfileUpdate(
                userId -> userProfileService.updateAlertMail(userId, editableAlertMail),
                localeBean.getMsg("profile.alertChangedSuccess"),
                () -> userSession.refreshAlertMail(profile.alertMail())
        );
    }

    public void saveIdentity() {
        identitySaveError = false;
        identitySaveMessage = "";
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            identitySaveError = true;
            identitySaveMessage = localeBean.getMsg("profile.userNotConnected");
            return;
        }
        boolean changingPassword = StringUtils.isNotBlank(newPassword)
                || StringUtils.isNotBlank(passwordConfirmation);
        try {
            if (changingPassword) {
                PasswordPolicy.validate(newPassword, passwordConfirmation);
            }
            profile = userProfileService.updateIdentity(userId, editableUsername, editableEmail);
            userSession.refreshDisplayName(profile.username());
            userSession.refreshEmail(profile.email());
            profile = userProfileService.updateAlertMail(userId, editableAlertMail);
            userSession.refreshAlertMail(profile.alertMail());
            if (changingPassword) {
                userPasswordService.changePassword(userId, newPassword, passwordConfirmation);
            }
            loaded = true;
            syncFormFromProfile();
            clearPasswordFields();
            identitySaveMessage = localeBean.getMsg(
                    changingPassword
                            ? "profile.identity.savedWithPassword"
                            : "profile.identity.saved"
            );
        } catch (InvalidProfileDataException | InvalidPasswordException e) {
            identitySaveError = true;
            identitySaveMessage = e.getMessage();
        }
    }

    public String labelForRole(String roleName) {
        if (StringUtils.isBlank(roleName)) {
            return "";
        }
        return localeBean.getMsg("profile.role." + roleName);
    }

    /**
     * Rôles distincts occupés sur les thésaurus (libellés localisés), triés du plus élevé au plus bas.
     */
    public List<String> getUniqueRoleLabels() {
        doLoad();
        if (projectRoles == null || projectRoles.isEmpty()) {
            return Collections.emptyList();
        }
        var uniqueRoleNames = new java.util.LinkedHashSet<String>();
        for (ProjectRoleOverview project : projectRoles) {
            if (project.thesaurusRoles() == null) {
                continue;
            }
            for (var role : project.thesaurusRoles()) {
                if (StringUtils.isNotBlank(role.roleName())) {
                    uniqueRoleNames.add(role.roleName().trim());
                }
            }
        }
        return uniqueRoleNames.stream()
                .sorted(java.util.Comparator.comparingInt(MyAccountBean::roleRank))
                .map(this::labelForRole)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private static int roleRank(String roleName) {
        return switch (roleName) {
            case "superAdmin" -> 1;
            case "admin" -> 2;
            case "manager" -> 3;
            case "contributor" -> 4;
            default -> 50;
        };
    }

    public String getApiKeyDisplay() {
        if (StringUtils.isNotBlank(apiKeyPlain)) {
            return apiKeyPlain;
        }
        if (profile != null && profile.hasApiKey()) {
            return localeBean.getMsg("profile.api.masked");
        }
        return localeBean.getMsg("profile.api.none");
    }

    public void regenerateApiKey() {
        identitySaveError = false;
        identitySaveMessage = "";
        Integer userId = requireConnectedUserId();
        if (userId == null) {
            identitySaveError = true;
            identitySaveMessage = localeBean.getMsg("profile.userNotConnected");
            return;
        }
        try {
            var result = userApiKeyService.regenerateApiKey(userId);
            profile = result.profile();
            apiKeyPlain = result.plainTextKey();
            identitySaveMessage = localeBean.getMsg("profile.apiKeySavedSuccess");
        } catch (ApiKeyRegenerationException e) {
            identitySaveError = true;
            identitySaveMessage = e.getMessage();
        }
    }

    public boolean isKeyExpired() {
        return ApiKeyPolicy.isExpired(profile);
    }

    public boolean isSuperAdmin() {
        return profile != null && profile.superAdmin();
    }

    public boolean isKeyNeverExpire() {
        return profile != null && profile.keyNeverExpire();
    }

    public java.time.LocalDate getKeyExpiresAt() {
        return profile != null ? profile.keyExpiresAt() : null;
    }

    public boolean isApiKeySectionVisible() {
        return ApiKeyPolicy.isSectionVisible(profile);
    }

    public boolean isCanRegenerateApiKey() {
        return userApiKeyService.canRegenerateApiKey(profile);
    }

    private void doLoad() {
        if (loaded) {
            return;
        }
        loaded = true;
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            log.warn("Tentative de chargement Mon compte v2 sans utilisateur connecté");
            clearState();
            return;
        }
        var profileWithRoles = userProfileService.getProfileWithRoles(userId);
        profile = profileWithRoles.profile();
        projectRoles = profileWithRoles.projectRoles();
        syncFormFromProfile();
    }

    private void applyProfileUpdate(
            IntFunction<UserProfile> updateAction,
            String successMessage,
            Runnable refreshSessionView
    ) {
        Integer userId = requireConnectedUserId();
        if (userId == null) {
            return;
        }
        try {
            profile = updateAction.apply(userId);
            refreshSessionView.run();
            loaded = true;
            syncFormFromProfile();
            MessageUtils.showInformationMessage(successMessage);
            refreshHeader();
        } catch (InvalidProfileDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    private void syncFormFromProfile() {
        if (profile == null) {
            editableUsername = null;
            editableEmail = null;
            editableAlertMail = false;
            apiKeyPlain = null;
            return;
        }
        editableUsername = profile.username();
        editableEmail = profile.email();
        editableAlertMail = profile.alertMail();
        apiKeyPlain = null;
    }

    private void clearPasswordFields() {
        newPassword = null;
        passwordConfirmation = null;
    }

    private void clearState() {
        profile = null;
        projectRoles = Collections.emptyList();
        syncFormFromProfile();
        clearPasswordFields();
    }

    private Integer requireConnectedUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(localeBean.getMsg("profile.userNotConnected"));
        }
        return userId;
    }

    private void refreshHeader() {
        PrimeFaces.current().ajax().update(HEADER_UPDATE_TARGET);
    }
}
