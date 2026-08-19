package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSettingsPersistService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named("v2ThesaurusPreferenceBean")
@ViewScoped
@RequiredArgsConstructor
public class ThesaurusPreferenceBean implements Serializable {

    private final SettingsAccess settingsAccess;
    private final ThesaurusContext thesaurusContext;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusSettingsPersistService persistService;
    private final ThesaurusCorpusBean corpusBean;
    private final ThesaurusAlignmentBean alignmentBean;

    private PreferenceEditor preference;
    private boolean preferenceLoaded;
    @Getter
    private String preferenceSaveMessage;
    @Getter
    private boolean preferenceSaveError;

    public void loadPage() {
        preferenceLoaded = false;
        ensurePreferencesLoaded();
        corpusBean.load();
        alignmentBean.load();
    }

    public PreferenceEditor getPreference() {
        ensurePreferencesLoaded();
        return preference;
    }

    public Integer getIdentifierAlphanumeric() {
        return 1;
    }

    public Integer getIdentifierNumeric() {
        return 2;
    }

    public String getPreferencePermalink() {
        PreferenceEditor editor = getPreference();
        if (editor == null || StringUtils.isBlank(editor.getPreferredName())) {
            return "";
        }
        return "/api/theso/" + editor.getPreferredName();
    }

    public void savePreferences() {
        preferenceSaveMessage = null;
        preferenceSaveError = false;
        if (!settingsAccess.isCanEdit()) {
            preferenceSaveError = true;
            preferenceSaveMessage = "Action non autorisée";
            return;
        }
        PreferenceEditor editor = getPreference();
        if (editor == null) {
            preferenceSaveError = true;
            preferenceSaveMessage = "Aucune préférence à enregistrer.";
            return;
        }
        String thesaurusId = settingsAccess.getThesaurusId();
        if (thesaurusPreferenceService.isPreferredNameExist(thesaurusId, editor.getPreferredName())) {
            preferenceSaveError = true;
            preferenceSaveMessage = "PreferredName existe déjà, veuillez en choisir un autre ! ";
            return;
        }
        if (editor.isUseOpenArk()) {
            String openArkError = validateOpenArkEditor(editor);
            if (openArkError != null) {
                preferenceSaveError = true;
                preferenceSaveMessage = openArkError;
                return;
            }
        }
        try {
            ThesaurusPreferences saved = persistService.saveAll(
                    thesaurusId,
                    settingsAccess.currentUserId(),
                    editor,
                    preferenceWorkLanguage(),
                    corpusBean.toPersistDraft(),
                    alignmentBean.toPersistDraft()
            );
            preference = PreferenceEditor.from(saved);
            preference.setNewPassArk(null);
            preference.setNewPassHandle(null);
            preference.setNewDeeplApiKey(null);
            preference.setNewApiKeyOpenArk(null);
            preferenceLoaded = true;
            corpusBean.load();
            alignmentBean.load();
            preferenceSaveMessage = "Paramètres enregistrés avec succès";
        } catch (SettingAccessDeniedException | InvalidSettingDataException e) {
            preferenceSaveError = true;
            preferenceSaveMessage = e.getMessage();
        }
    }

    private void ensurePreferencesLoaded() {
        if (preferenceLoaded && preference != null) {
            return;
        }
        String thesaurusId = settingsAccess.getThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            preference = new PreferenceEditor();
            return;
        }
        String workLang = preferenceWorkLanguage();
        ThesaurusPreferences prefs = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLang);
        if (prefs == null) {
            preference = new PreferenceEditor();
            preference.setLanguages(new ArrayList<>(loadPreferenceLanguages(thesaurusId, workLang)));
            preferenceLoaded = true;
            return;
        }
        preference = PreferenceEditor.from(prefs);
        if (preference.getLanguages() == null || preference.getLanguages().isEmpty()) {
            preference.setLanguages(new ArrayList<>(loadPreferenceLanguages(thesaurusId, workLang)));
        }
        preferenceLoaded = true;
    }

    private String preferenceWorkLanguage() {
        return StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
    }

    private List<ThesaurusLanguage> loadPreferenceLanguages(String thesaurusId, String workLang) {
        List<ThesaurusLanguage> loaded = thesaurusPreferenceService.loadUsedLanguages(thesaurusId, workLang);
        return loaded != null ? loaded : Collections.emptyList();
    }

    private String validateOpenArkEditor(PreferenceEditor editor) {
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
                settingsAccess.getThesaurusId(),
                preferenceWorkLanguage()
        );
        boolean hasExistingKey = current != null && StringUtils.isNotBlank(current.apiKeyOpenArk());
        if (!hasExistingKey && StringUtils.isBlank(editor.getNewApiKeyOpenArk())) {
            return "OpenArk : clé API obligatoire";
        }
        return null;
    }
}
