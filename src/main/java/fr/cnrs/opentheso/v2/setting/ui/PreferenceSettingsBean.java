package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Slf4j
@Getter
@ViewScoped
@Named("v2PreferenceSettingsBean")
public class PreferenceSettingsBean implements Serializable {

    private final UserSession userSession;
    private final RightsService rightsService;
    private final ThesaurusContext thesaurusContext;
    private final V2LocaleBean localeBean;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ConsultationShellBean consultationShellBean;
    private final ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;

    private PreferenceEditor editor;

    public PreferenceSettingsBean(
            UserSession userSession,
            RightsService rightsService,
            ThesaurusContext thesaurusContext,
            V2LocaleBean localeBean,
            ThesaurusPreferenceService thesaurusPreferenceService,
            ConsultationShellBean consultationShellBean,
            ThesaurusSearchLanguageSync thesaurusSearchLanguageSync
    ) {
        this.userSession = userSession;
        this.rightsService = rightsService;
        this.thesaurusContext = thesaurusContext;
        this.localeBean = localeBean;
        this.thesaurusPreferenceService = thesaurusPreferenceService;
        this.consultationShellBean = consultationShellBean;
        this.thesaurusSearchLanguageSync = thesaurusSearchLanguageSync;
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
                    localeBean.getIdLangue()
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

        if(thesaurusPreferenceService.isPreferredNameExist(
                thesaurusContext.getCurrentThesaurusId(),
                editor.getPreferredName())){
            MessageUtils.showErrorMessage("PreferredName existe déjà, veuillez en choisir un autre ! ");
            return;
        }

        if (editor.isUseOpenArk()) {
            String openArkError = validateOpenArkEditor();
            if (openArkError != null) {
                MessageUtils.showErrorMessage(openArkError);
                return;
            }
        }

        try {
            ThesaurusPreferences saved = thesaurusPreferenceService.savePreferences(
                    thesaurusContext.getCurrentThesaurusId(),
                    editor.toModel(thesaurusContext.getCurrentThesaurusId()),
                    editor.getNewPassArk(),
                    editor.getNewPassHandle(),
                    editor.getNewDeeplApiKey(),
                    editor.getNewApiKeyOpenArk(),
                    localeBean.getIdLangue()
            );
            editor = PreferenceEditor.from(saved);
            editor.setNewPassArk(null);
            editor.setNewPassHandle(null);
            editor.setNewDeeplApiKey(null);
            editor.setNewApiKeyOpenArk(null);
            // Applique la langue par défaut à la consultation (V2 + sélecteur legacy search.xhtml).
            if (StringUtils.isNotBlank(saved.sourceLang())) {
                thesaurusSearchLanguageSync.applyAfterSourceLanguageChange(
                        thesaurusContext.getCurrentThesaurusId(),
                        saved.sourceLang()
                );
            }
            consultationShellBean.refreshHeaderCatalog();
            MessageUtils.showInformationMessage("Préférences enregistrées avec succès");
        } catch (SettingAccessDeniedException | InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    private String validateOpenArkEditor() {
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
                thesaurusContext.getCurrentThesaurusId(),
                localeBean.getIdLangue()
        );
        boolean hasExistingKey = current != null && StringUtils.isNotBlank(current.apiKeyOpenArk());
        if (!hasExistingKey && StringUtils.isBlank(editor.getNewApiKeyOpenArk())) {
            return "OpenArk : clé API obligatoire";
        }
        return null;
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
                localeBean.getIdLangue()
        );
        editor = PreferenceEditor.from(preferences);
    }

    private boolean canManage() {
        String thesaurusId = thesaurusContext.getCurrentThesaurusId();
        if (thesaurusId == null) {
            return false;
        }
        return rightsService.can(userSession, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus(thesaurusId));
    }
}
