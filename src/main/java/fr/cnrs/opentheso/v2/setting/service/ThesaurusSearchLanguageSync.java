package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.concept.search.ui.ConceptSearchBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Aligne la langue / la liste des langues de consultation après une modification
 * en édition V2 (langue source, ajout/suppression de traduction).
 * <ul>
 *   <li>V2 : {@link ThesaurusContext} + {@code v2ConceptSearchBean.searchLang}</li>
 *   <li>Legacy : {@code selectedThesaurus} → sélecteur {@code search.xhtml#languageSelect}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusSearchLanguageSync {

    private final ThesaurusContext thesaurusContext;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ObjectProvider<ConceptSearchBean> conceptSearchBeanProvider;

    public void applyAfterSourceLanguageChange(String thesaurusId, String language) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(language)) {
            return;
        }
        String thesoId = thesaurusId.trim();
        String lang = language.trim();

        if (isCurrentV2Thesaurus(thesoId)) {
            thesaurusContext.changeWorkLanguage(lang);
            syncV2SearchLanguageSelector(lang);
        }

        if (!isCurrentLegacyThesaurus(thesoId)) {
            log.debug(
                    "Langue source {} enregistrée pour {}, hors thésaurus sélectionné en session ({})",
                    lang, thesoId, legacySelectedThesaurusId()
            );
            return;
        }

        selectedTheso.setSelectedLang(lang);
        selectedTheso.setCurrentLang(lang);
        roleOnThesaurusBean.initNodePref(thesoId);
        log.debug("Sélecteur de langue (search) aligné sur {} pour le thésaurus {}", lang, thesoId);
    }

    /**
     * Comme le legacy au clic sur une traduction de concept :
     * change la langue de consultation et aligne le sélecteur de recherche V2
     * ({@code v2SearchLanguage}) + legacy ({@code languageSelect}).
     */
    public void applyConsultationLanguageFromConceptTranslation(String language) {
        if (StringUtils.isBlank(language) || "all".equalsIgnoreCase(language)) {
            return;
        }
        String lang = language.trim();
        thesaurusContext.changeWorkLanguage(lang);
        syncV2SearchLanguageSelector(lang);

        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        selectedTheso.setSelectedIdTheso(thesaurusId);
        selectedTheso.setCurrentIdTheso(thesaurusId);
        selectedTheso.setSelectedLang(lang);
        selectedTheso.setCurrentLang(lang);
    }

    /**
     * Après ajout / suppression d'une langue du thésaurus : recharge les listes des
     * sélecteurs de recherche (legacy + V2) et corrige la langue courante si elle a été
     * supprimée.
     */
    public void applyAfterLanguageListChange(String thesaurusId, String removedLanguageCode) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        String thesoId = thesaurusId.trim();
        String removed = StringUtils.trimToNull(removedLanguageCode);

        thesaurusPreferenceService.evictPreferencesCache();

        if (isCurrentV2Thesaurus(thesoId)
                && removed != null
                && removed.equalsIgnoreCase(thesaurusContext.resolveWorkLanguage())) {
            String fallback = thesaurusWorkLanguageService.resolveForThesaurus(thesoId);
            thesaurusContext.changeWorkLanguage(fallback);
            syncV2SearchLanguageSelector(fallback);
        }

        // Toujours aligner SelectedTheso quand le thésaurus édité est celui de la session V2
        // ou legacy : sinon search.xhtml garde nodeLangs obsolète après retour / switch d'UI.
        if (isCurrentV2Thesaurus(thesoId) || isCurrentLegacyThesaurus(thesoId)) {
            selectedTheso.setSelectedIdTheso(thesoId);
            selectedTheso.setCurrentIdTheso(thesoId);
            roleOnThesaurusBean.initNodePref(thesoId);
            selectedTheso.refreshUsedLanguages();
            log.debug("Liste des langues (search.xhtml) rafraîchie pour le thésaurus {}", thesoId);
        }

        ConceptSearchBean conceptSearchBean = conceptSearchBeanProvider.getIfAvailable();
        if (conceptSearchBean != null && isCurrentV2Thesaurus(thesoId)) {
            conceptSearchBean.reloadAvailableLanguages();
            log.debug("Liste des langues (search-bar V2) rafraîchie pour le thésaurus {}", thesoId);
        }
    }

    private void syncV2SearchLanguageSelector(String lang) {
        ConceptSearchBean conceptSearchBean = conceptSearchBeanProvider.getIfAvailable();
        if (conceptSearchBean != null) {
            conceptSearchBean.setSearchLang(lang);
        }
    }

    private boolean isCurrentV2Thesaurus(String thesaurusId) {
        return thesaurusId.equalsIgnoreCase(StringUtils.defaultString(thesaurusContext.resolveThesaurusId()));
    }

    private boolean isCurrentLegacyThesaurus(String thesaurusId) {
        String selectedId = legacySelectedThesaurusId();
        return StringUtils.isNotBlank(selectedId) && thesaurusId.equalsIgnoreCase(selectedId);
    }

    private String legacySelectedThesaurusId() {
        return StringUtils.firstNonBlank(
                selectedTheso.getCurrentIdTheso(),
                selectedTheso.getSelectedIdTheso()
        );
    }
}
