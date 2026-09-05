package fr.cnrs.opentheso.v2.concept.search.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptSearchBean")
@RequiredArgsConstructor
public class ConceptSearchBean implements Serializable {

    private static final String SHOW_RESULT_SEARCH_BAR = "showResultSearchBar();";

    private final transient ConceptSearchService conceptSearchService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusPreferenceService thesaurusPreferenceService;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient ConceptNavigationSupport conceptNavigationSupport;

    private String searchLang;
    private String searchValue;
    private ConceptSearchSuggestion selectedSuggestion;
    private List<ConceptSearchResult> results = Collections.emptyList();
    private List<ThesaurusLanguage> availableLanguages = Collections.emptyList();

    private boolean exactMatch;
    private boolean startWithMatch;
    private boolean noteMatch;
    private boolean identifierMatch;
    private boolean resultsVisible;
    private boolean singleResultSelected;

    public void syncFromContext() {
        searchLang = thesaurusContext.resolveWorkLanguage();
        loadLanguages();
    }

    public void onLanguageChange() {
        results = Collections.emptyList();
        resultsVisible = false;
        singleResultSelected = false;
        selectedSuggestion = null;

        if ("all".equalsIgnoreCase(searchLang)) {
            // Recherche multi-langue : on ne change pas la langue d'affichage des arbres.
            return;
        }

        thesaurusContext.changeWorkLanguage(searchLang);
        conceptNavigationSupport.reloadAfterLanguageChange();
    }

    public List<ConceptSearchSuggestion> complete(String query) {
        if (!isSearchAvailable() || StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        searchValue = query.trim();
        return conceptSearchService.autocomplete(
                searchValue,
                activeMode(),
                thesaurusContext.resolveThesaurusId(),
                searchLang,
                !userSession.isLoggedIn()
        );
    }

    public void onSuggestionSelect(SelectEvent<ConceptSearchSuggestion> event) {
        ConceptSearchSuggestion suggestion = event.getObject();
        if (suggestion == null) {
            return;
        }
        if (suggestion.isGroup()) {
            conceptNavigationSupport.focusGroup(suggestion.conceptId());
            resultsVisible = false;
            return;
        }
        if (suggestion.isFacet()) {
            conceptNavigationSupport.focusFacet(suggestion.conceptId());
            resultsVisible = false;
            return;
        }
        openConcept(suggestion.conceptId());
        results = conceptSearchService.search(
                searchValue,
                activeMode(),
                thesaurusContext.resolveThesaurusId(),
                searchLang,
                !userSession.isLoggedIn()
        );
        singleResultSelected = true;
        resultsVisible = !results.isEmpty();
    }

    public void applySearch() {
        if (!isSearchAvailable()) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("candidat.save.msg9"));
            return;
        }
        if (StringUtils.isBlank(searchValue)) {
            searchValue = "";
        }
        results = conceptSearchService.search(
                searchValue,
                activeMode(),
                thesaurusContext.resolveThesaurusId(),
                searchLang,
                !userSession.isLoggedIn()
        );
        if (results.isEmpty()) {
            resultsVisible = false;
            MessageUtils.showWarnMessage(v2LocaleBean.getMsg("search.noResult") + " !");
            return;
        }
        if (results.size() == 1) {
            openConcept(results.get(0).getConceptId());
            singleResultSelected = true;
        } else {
            singleResultSelected = false;
            PrimeFaces.current().executeScript(SHOW_RESULT_SEARCH_BAR);
        }
        resultsVisible = true;
    }

    public void selectResult(ConceptSearchResult result) {
        if (result == null || StringUtils.isBlank(result.getConceptId())) {
            return;
        }
        openConcept(result.getConceptId());
        singleResultSelected = true;
    }

    public void hideResults() {
        resultsVisible = false;
        PrimeFaces.current().executeScript("hideResultSearchBar();");
    }

    public void activateStartWithMatch() {
        exactMatch = false;
        noteMatch = false;
        identifierMatch = false;
    }

    public void activateExactMatch() {
        startWithMatch = false;
        noteMatch = false;
        identifierMatch = false;
    }

    public void activateNoteMatch() {
        startWithMatch = false;
        exactMatch = false;
        identifierMatch = false;
    }

    public void activateIdentifierMatch() {
        startWithMatch = false;
        exactMatch = false;
        noteMatch = false;
    }

    public void runDeprecatedSearch() {
        runPreprogrammed(conceptSearchService.searchDeprecated(
                thesaurusContext.resolveThesaurusId(), preprogrammedLang()));
    }

    public void runPolyhierarchySearch() {
        runPreprogrammed(conceptSearchService.searchPolyhierarchy(
                thesaurusContext.resolveThesaurusId(), preprogrammedLang()));
    }

    public void runMultiGroupsSearch() {
        runPreprogrammed(conceptSearchService.searchMultiGroups(
                thesaurusContext.resolveThesaurusId(), preprogrammedLang()));
    }

    public void runWithoutGroupsSearch() {
        runPreprogrammed(conceptSearchService.searchWithoutGroups(
                thesaurusContext.resolveThesaurusId(), preprogrammedLang()));
    }

    /**
     * Aligné sur legacy {@code SearchBean#searchConceptDuplicated} :
     * langue de travail du thésaurus, tri, ouverture du 1er résultat,
     * barre de résultats si plusieurs.
     */
    public void runDuplicatesSearch() {
        if (!isPreprogrammedSearchAvailable()) {
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = preprogrammedLang();
        results = conceptSearchService.searchDuplicates(thesaurusId, lang);
        if (results.isEmpty()) {
            resultsVisible = false;
            singleResultSelected = false;
            MessageUtils.showWarnMessage("Recherche doublons : Aucun résultat trouvée !");
            return;
        }
        openConcept(results.get(0).getConceptId());
        if (results.size() == 1) {
            singleResultSelected = true;
            resultsVisible = false;
        } else {
            singleResultSelected = false;
            resultsVisible = true;
            PrimeFaces.current().executeScript(SHOW_RESULT_SEARCH_BAR);
        }
    }

    public void runForbiddenRelationshipsSearch() {
        runPreprogrammed(conceptSearchService.searchForbiddenRelationships(
                thesaurusContext.resolveThesaurusId(), preprogrammedLang()));
    }

    public boolean isSearchAvailable() {
        return StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId());
    }

    public boolean isPreprogrammedSearchAvailable() {
        return userSession.isLoggedIn() && isSearchAvailable();
    }

    public void clear() {
        searchValue = null;
        selectedSuggestion = null;
        results = Collections.emptyList();
        resultsVisible = false;
        singleResultSelected = false;
    }

    /**
     * Comme legacy {@code selectedTheso.currentLang}.
     */
    private String preprogrammedLang() {
        return thesaurusContext.resolveWorkLanguage();
    }

    private void runPreprogrammed(List<ConceptSearchResult> preprogrammedResults) {
        if (!isPreprogrammedSearchAvailable()) {
            return;
        }
        results = preprogrammedResults;
        if (results.isEmpty()) {
            resultsVisible = false;
            MessageUtils.showWarnMessage(v2LocaleBean.getMsg("search.noResult") + " !");
            return;
        }
        if (results.size() == 1) {
            openConcept(results.get(0).getConceptId());
            singleResultSelected = true;
        } else {
            singleResultSelected = false;
            PrimeFaces.current().executeScript(SHOW_RESULT_SEARCH_BAR);
        }
        resultsVisible = true;
    }

    private void openConcept(String conceptId) {
        conceptNavigationSupport.openConcept(conceptId);
    }

    private ConceptSearchMode activeMode() {
        if (exactMatch) {
            return ConceptSearchMode.EXACT;
        }
        if (startWithMatch) {
            return ConceptSearchMode.START_WITH;
        }
        if (noteMatch) {
            return ConceptSearchMode.NOTE;
        }
        if (identifierMatch) {
            return ConceptSearchMode.IDENTIFIER;
        }
        return ConceptSearchMode.FULL_TEXT;
    }

    private void loadLanguages() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            availableLanguages = Collections.emptyList();
            return;
        }
        try {
            availableLanguages = thesaurusPreferenceService.loadUsedLanguages(
                    thesaurusId, thesaurusContext.resolveWorkLanguage());
            if (StringUtils.isNotBlank(searchLang)
                    && !"all".equalsIgnoreCase(searchLang)
                    && availableLanguages.stream().noneMatch(lang -> searchLang.equalsIgnoreCase(lang.code()))) {
                searchLang = thesaurusContext.resolveWorkLanguage();
            }
        } catch (RuntimeException ex) {
            availableLanguages = Collections.emptyList();
        }
    }

    /**
     * Recharge la liste des langues du sélecteur de recherche (après édition thésaurus).
     */
    public void reloadAvailableLanguages() {
        loadLanguages();
    }
}
