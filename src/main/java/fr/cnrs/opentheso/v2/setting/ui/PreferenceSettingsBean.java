package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Slf4j
@Getter
@ViewScoped
@Named("v2PreferenceSettingsBean")
public class PreferenceSettingsBean implements Serializable {

    private final UserSession userSession;
    private final ThesaurusContext thesaurusContext;
    private final LanguageBean languageBean;
    private final ThesaurusPreferenceService thesaurusPreferenceService;

    private PreferenceEditor editor;

    public PreferenceSettingsBean(
            UserSession userSession,
            ThesaurusContext thesaurusContext,
            LanguageBean languageBean,
            ThesaurusPreferenceService thesaurusPreferenceService
    ) {
        this.userSession = userSession;
        this.thesaurusContext = thesaurusContext;
        this.languageBean = languageBean;
        this.thesaurusPreferenceService = thesaurusPreferenceService;
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (!canManage()) {
            editor = null;
            return;
        }
        reloadEditor();
    }

    public void updateSelectedServer(String serverKey) {
        if (!canManage() || editor == null) {
            return;
        }
        switch (serverKey) {
            case "ark" -> {
                editor.setUseArkLocal(false);
                editor.setUseHandle(false);
                editor.setUseOpenArk(false);
            }
            case "arklocal" -> {
                editor.setUseArk(false);
                editor.setUseHandle(false);
                editor.setUseOpenArk(false);
            }
            case "handle" -> {
                editor.setUseArk(false);
                editor.setUseArkLocal(false);
                editor.setUseOpenArk(false);
            }
            case "openark" -> {
                editor.setUseArk(false);
                editor.setUseArkLocal(false);
                editor.setUseHandle(false);
            }
            default -> {
                return;
            }
        }
        IdentifierServerType serverType = resolveIdentifierServerType();
        try {
            ThesaurusPreferences updated = thesaurusPreferenceService.updateIdentifierServer(
                    thesaurusContext.getCurrentThesaurusId(),
                    serverType,
                    languageBean.getIdLangue()
            );
            editor = PreferenceEditor.from(updated);
            editor.setUseArk(serverType == IdentifierServerType.ARK);
            editor.setUseArkLocal(serverType == IdentifierServerType.ARK_LOCAL);
            editor.setUseHandle(serverType == IdentifierServerType.HANDLE);
            editor.setUseOpenArk(serverType == IdentifierServerType.OPENARK);
        } catch (InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    private IdentifierServerType resolveIdentifierServerType() {
        if (editor.isUseOpenArk()) {
            return IdentifierServerType.OPENARK;
        }
        if (editor.isUseHandle()) {
            return IdentifierServerType.HANDLE;
        }
        if (editor.isUseArkLocal()) {
            return IdentifierServerType.ARK_LOCAL;
        }
        if (editor.isUseArk()) {
            return IdentifierServerType.ARK;
        }
        return IdentifierServerType.NONE;
    }

    public void save() {
        if (!canManage() || editor == null) {
            return;
        }
        try {
            ThesaurusPreferences saved = thesaurusPreferenceService.savePreferences(
                    thesaurusContext.getCurrentThesaurusId(),
                    editor.toModel(thesaurusContext.getCurrentThesaurusId()),
                    editor.getNewPassArk(),
                    editor.getNewPassHandle(),
                    editor.getNewDeeplApiKey(),
                    editor.getNewApiKeyOpenArk(),
                    languageBean.getIdLangue()
            );
            editor = PreferenceEditor.from(saved);
            editor.setNewPassArk(null);
            editor.setNewPassHandle(null);
            editor.setNewDeeplApiKey(null);
            editor.setNewApiKeyOpenArk(null);
            MessageUtils.showInformationMessage("Préférences enregistrées avec succès");
            PrimeFaces.current().ajax().update("containerIndex messageIndex");
        } catch (SettingAccessDeniedException | InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void clearNewPasswords() {
        if (editor != null) {
            editor.setNewPassArk(null);
            editor.setNewPassHandle(null);
            editor.setNewDeeplApiKey(null);
            editor.setNewApiKeyOpenArk(null);
        }
    }

    public boolean isScreenAvailable() {
        return canManage() && editor != null;
    }

    private void reloadEditor() {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferences(
                thesaurusContext.getCurrentThesaurusId(),
                languageBean.getIdLangue()
        );
        editor = PreferenceEditor.from(preferences);
    }

    private boolean canManage() {
        if (!userSession.hasRoleAsAdmin()) {
            return false;
        }
        return thesaurusContext.getCurrentThesaurusId() != null;
    }
}
