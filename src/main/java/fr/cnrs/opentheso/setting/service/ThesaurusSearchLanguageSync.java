package fr.cnrs.opentheso.setting.service;

import fr.cnrs.opentheso.concept.search.ui.ConceptSearchBean;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Aligne la langue et la liste des langues de consultation après une modification
 * en édition V2 (langue source, ajout ou suppression de traduction).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusSearchLanguageSync {

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ObjectProvider<ConceptSearchBean> conceptSearchBeanProvider;

    public void applyAfterSourceLanguageChange(String thesaurusId, String language) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(language)) {
            return;
        }
        String thesoId = thesaurusId.trim();
        String lang = language.trim();
        if (!isCurrentThesaurus(thesoId)) {
            log.debug("Langue source {} enregistrée pour {}, hors thésaurus ouvert", lang, thesoId);
            return;
        }
        thesaurusContext.changeWorkLanguage(lang);
        syncSearchLanguageSelector(lang);
    }

    /**
     * Au clic sur une traduction de concept : change la langue de consultation
     * et aligne le sélecteur de recherche.
     */
    public void applyConsultationLanguageFromConceptTranslation(String language) {
        if (StringUtils.isBlank(language) || "all".equalsIgnoreCase(language)) {
            return;
        }
        String lang = language.trim();
        thesaurusContext.changeWorkLanguage(lang);
        syncSearchLanguageSelector(lang);
    }

    /**
     * Après ajout ou suppression d'une langue du thésaurus : recharge la liste
     * du sélecteur et corrige la langue courante si elle a été retirée.
     */
    public void applyAfterLanguageListChange(String thesaurusId, String removedLanguageCode) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        String thesoId = thesaurusId.trim();
        String removed = StringUtils.trimToNull(removedLanguageCode);

        thesaurusPreferenceService.evictPreferencesCache();
        if (!isCurrentThesaurus(thesoId)) {
            return;
        }

        if (removed != null && removed.equalsIgnoreCase(thesaurusContext.resolveWorkLanguage())) {
            String fallback = thesaurusWorkLanguageService.resolveForThesaurus(thesoId);
            thesaurusContext.changeWorkLanguage(fallback);
            syncSearchLanguageSelector(fallback);
        }

        ConceptSearchBean conceptSearchBean = conceptSearchBeanProvider.getIfAvailable();
        if (conceptSearchBean != null) {
            conceptSearchBean.reloadAvailableLanguages();
        }
    }

    private void syncSearchLanguageSelector(String lang) {
        ConceptSearchBean conceptSearchBean = conceptSearchBeanProvider.getIfAvailable();
        if (conceptSearchBean != null) {
            conceptSearchBean.setSearchLang(lang);
        }
    }

    private boolean isCurrentThesaurus(String thesaurusId) {
        return thesaurusId.equalsIgnoreCase(StringUtils.defaultString(thesaurusContext.resolveThesaurusId()));
    }
}
