package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
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
@RequestScoped
@RequiredArgsConstructor
public class PreviewThesaurusBean implements Serializable {

    public static final String THESAURUS_ID = "th17";
    public static final String THESAURUS_TITLE = "Pactols_Lieux";

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final V2LocaleBean v2LocaleBean;

    private Integer conceptCount;
    private List<ThesaurusLanguage> languages;
    private String selectedLang;
    private boolean sessionReady;

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

    public void onLanguageChange() {
        if (StringUtils.isNotBlank(selectedLang)) {
            thesaurusContext.changeWorkLanguage(selectedLang);
        }
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
