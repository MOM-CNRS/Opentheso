package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Après changement de la langue source (édition V2), aligne la langue de consultation :
 * <ul>
 *   <li>V2 : {@link ThesaurusContext} → sélecteur {@code v2ConceptSearchBean.searchLang}</li>
 *   <li>Legacy : {@code selectedThesaurus.selectedLang} → sélecteur {@code search.xhtml#languageSelect}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusSearchLanguageSync {

    private final ThesaurusContext thesaurusContext;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;

    public void applyAfterSourceLanguageChange(String thesaurusId, String language) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(language)) {
            return;
        }
        String thesoId = thesaurusId.trim();
        String lang = language.trim();

        // V2 : langue de travail de la session consultation
        if (thesoId.equalsIgnoreCase(StringUtils.defaultString(thesaurusContext.resolveThesaurusId()))) {
            thesaurusContext.changeWorkLanguage(lang);
        }

        // Legacy search.xhtml : languageSelect bound to selectedThesaurus.selectedLang
        String selectedId = StringUtils.firstNonBlank(
                selectedTheso.getCurrentIdTheso(),
                selectedTheso.getSelectedIdTheso()
        );
        if (StringUtils.isBlank(selectedId) || !thesoId.equalsIgnoreCase(selectedId)) {
            log.debug(
                    "Langue source {} enregistrée pour {}, hors thésaurus sélectionné en session ({})",
                    lang, thesoId, selectedId
            );
            return;
        }

        selectedTheso.setSelectedLang(lang);
        selectedTheso.setCurrentLang(lang);
        roleOnThesaurusBean.initNodePref(thesoId);
        log.debug("Sélecteur de langue (search) aligné sur {} pour le thésaurus {}", lang, thesoId);
    }
}
