package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.v2.user.service.UserApiKeyService;
import fr.cnrs.opentheso.v2.user.service.UserPasswordService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import fr.cnrs.opentheso.v2.user.validation.PasswordPolicy;
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
@Named("v2MyAccountBean")
public class MyAccountBean implements Serializable {

    private static final String HEADER_UPDATE_TARGET = "containerIndex";

    private final UserSession userSession;
    private final V2LocaleBean localeBean;
    private final UserProfileService userProfileService;
    private final UserApiKeyService userApiKeyService;
    private final UserPasswordService userPasswordService;

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
            V2LocaleBean localeBean,
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
            if (changingPassword) {
                userPasswordService.changePassword(userId, newPassword, passwordConfirmation);
            }
            loaded = true;
            syncFormFromProfile();
            clearPasswordFields();
            identitySaveMessage = localeBean.getMsg(
                    changingPassword
                            ? "v2.profile.identity.savedWithPassword"
                            : "v2.profile.identity.saved"
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
        return localeBean.getMsg("v2.profile.role." + roleName);
    }

    public void regenerateApiKey() {
        Integer userId = requireConnectedUserId();
        if (userId == null) {
            return;
        }
        try {
            var result = userApiKeyService.regenerateApiKey(userId);
            profile = result.profile();
            apiKeyPlain = result.plainTextKey();
            MessageUtils.showInformationMessage(localeBean.getMsg("profile.apiKeySavedSuccess"));
        } catch (ApiKeyRegenerationException e) {
            MessageUtils.showErrorMessage(e.getMessage());
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

    public boolean canRegenerateApiKey() {
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
