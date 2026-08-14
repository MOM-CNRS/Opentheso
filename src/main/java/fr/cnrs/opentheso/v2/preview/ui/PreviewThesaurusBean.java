package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Façade preview : lit / écrit {@link ThesaurusContext}.
 * Tant qu'aucun thésaurus n'est en session, sélectionne temporairement {@code th17}.
 */
@Named("v2PreviewThesaurusBean")
@ViewScoped
@RequiredArgsConstructor
public class PreviewThesaurusBean implements Serializable {

    public static final String THESAURUS_ID = "th17";
    public static final String THESAURUS_TITLE = "Pactols_Lieux";

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final ThesaurusHomeWriteService thesaurusHomeWriteService;
    private final UserSession userSession;
    private final RightsService rightsService;
    private final V2LocaleBean v2LocaleBean;

    private Integer conceptCount;
    private List<ThesaurusLanguage> languages;
    private String selectedLang;
    private boolean sessionReady;
    private String homePageHtml;
    private boolean homePageHtmlLoaded;
    private PreferenceEditor preference;
    private boolean preferenceLoaded;

    @Getter
    @Setter
    private String homeHtml;
    @Getter
    private boolean editing;
    @Getter
    private String saveMessage;
    @Getter
    private boolean saveError;

    public void ensureSessionThesaurus() {
        if (sessionReady) {
            return;
        }
        sessionReady = true;
        if (StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId())) {
            return;
        }
        String sourceLang = StringUtils.defaultIfBlank(
                thesaurusWorkLanguageService.resolveForThesaurus(THESAURUS_ID),
                "fr"
        );
        thesaurusContext.selectThesaurus(THESAURUS_ID, THESAURUS_TITLE, sourceLang);
    }

    public String getId() {
        ensureSessionThesaurus();
        return thesaurusContext.resolveThesaurusId();
    }

    public String getTitle() {
        ensureSessionThesaurus();
        return StringUtils.defaultString(thesaurusContext.getCurrentThesaurusTitle());
    }

    public int getConceptCount() {
        if (conceptCount == null) {
            conceptCount = thesaurusHomeQueryRepository.countValidConcepts(getId());
        }
        return conceptCount;
    }

    public String getConceptCountLabel() {
        int count = getConceptCount();
        String formatted = NumberFormat.getIntegerInstance(Locale.FRANCE).format(count);
        return formatted + (count > 1 ? " concepts" : " concept");
    }

    public List<ThesaurusLanguage> getLanguages() {
        ensureLanguagesLoaded();
        return languages;
    }

    public String getSelectedLang() {
        ensureLanguagesLoaded();
        String current = StringUtils.firstNonBlank(selectedLang, thesaurusContext.resolveWorkLanguage());
        if (languageExists(current)) {
            return current;
        }
        if (languages.isEmpty()) {
            return current;
        }
        String fallback = languages.get(0).code();
        thesaurusContext.changeWorkLanguage(fallback);
        return fallback;
    }

    public void setSelectedLang(String selectedLang) {
        this.selectedLang = selectedLang;
    }

    public String getSelectedLangLabel() {
        String code = getSelectedLang();
        if (languages == null || StringUtils.isBlank(code)) {
            return code;
        }
        return languages.stream()
                .filter(lang -> code.equalsIgnoreCase(lang.code()))
                .map(ThesaurusLanguage::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(code);
    }

    public void onLanguageChange() {
        if (StringUtils.isNotBlank(selectedLang)) {
            thesaurusContext.changeWorkLanguage(selectedLang);
        }
        invalidateHomePageHtml();
        if (editing) {
            homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        }
    }

    public PreferenceEditor getPreference() {
        ensurePreferencesLoaded();
        return preference;
    }

    public String getPreferencePermalink() {
        PreferenceEditor editor = getPreference();
        if (editor == null || StringUtils.isBlank(editor.getPreferredName())) {
            return "";
        }
        return "/api/theso/" + editor.getPreferredName();
    }

    public String getHomePageHtml() {
        if (!homePageHtmlLoaded) {
            homePageHtml = StringUtils.defaultString(
                    thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage())
            );
            homePageHtmlLoaded = true;
        }
        return homePageHtml;
    }

    public boolean isHomePageHtmlPresent() {
        return StringUtils.isNotBlank(getHomePageHtml());
    }

    public boolean isCanEdit() {
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
    }

    public void startEditing() {
        saveMessage = null;
        saveError = false;
        if (!isCanEdit()) {
            return;
        }
        homeHtml = thesaurusHomeWriteService.loadHtml(getId(), thesaurusContext.resolveWorkLanguage());
        editing = true;
    }

    public void cancelEditing() {
        editing = false;
        homeHtml = null;
        saveMessage = null;
        saveError = false;
    }

    public void saveHomeHtml() {
        saveMessage = null;
        saveError = false;
        if (!isCanEdit()) {
            saveError = true;
            saveMessage = "Action non autorisée";
            return;
        }
        boolean ok = thesaurusHomeWriteService.saveHtml(
                getId(),
                thesaurusContext.resolveWorkLanguage(),
                homeHtml
        );
        if (!ok) {
            saveError = true;
            saveMessage = "L'enregistrement a échoué.";
            return;
        }
        editing = false;
        homeHtml = null;
        invalidateHomePageHtml();
        saveMessage = "Description enregistrée.";
    }

    private void invalidateHomePageHtml() {
        homePageHtml = null;
        homePageHtmlLoaded = false;
    }

    private void ensurePreferencesLoaded() {
        if (preferenceLoaded) {
            return;
        }
        preferenceLoaded = true;
        String thesaurusId = getId();
        String workLang = StringUtils.defaultIfBlank(v2LocaleBean.getIdLangue(), "fr");
        ThesaurusPreferences prefs = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLang);
        if (prefs == null) {
            preference = new PreferenceEditor();
            preference.setLanguages(getLanguages());
            return;
        }
        preference = PreferenceEditor.from(prefs);
    }

    private void ensureLanguagesLoaded() {
        if (languages != null) {
            return;
        }
        languages = thesaurusPreferenceService.loadUsedLanguages(getId(), v2LocaleBean.getIdLangue());
        if (languages == null) {
            languages = Collections.emptyList();
        }
    }

    private boolean languageExists(String lang) {
        return StringUtils.isNotBlank(lang)
                && languages.stream().anyMatch(item -> item.code().equalsIgnoreCase(lang));
    }
}
