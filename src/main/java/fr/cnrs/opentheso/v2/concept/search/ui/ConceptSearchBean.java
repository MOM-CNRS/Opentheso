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

    private final ConceptSearchService conceptSearchService;
    private final ThesaurusContext thesaurusContext;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final UserSession userSession;
    private final V2LocaleBean v2LocaleBean;
    private final ConceptNavigationSupport conceptNavigationSupport;

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
            PrimeFaces.current().executeScript("showResultSearchBar();");
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
                thesaurusContext.resolveThesaurusId(), searchLang));
    }

    public void runPolyhierarchySearch() {
        runPreprogrammed(conceptSearchService.searchPolyhierarchy(
                thesaurusContext.resolveThesaurusId(), searchLang));
    }

    public void runMultiGroupsSearch() {
        runPreprogrammed(conceptSearchService.searchMultiGroups(
                thesaurusContext.resolveThesaurusId(), searchLang));
    }

    public void runWithoutGroupsSearch() {
        runPreprogrammed(conceptSearchService.searchWithoutGroups(
                thesaurusContext.resolveThesaurusId(), searchLang));
    }

    public void runDuplicatesSearch() {
        runPreprogrammed(conceptSearchService.searchDuplicates(
                thesaurusContext.resolveThesaurusId(), searchLang));
    }

    public void runForbiddenRelationshipsSearch() {
        runPreprogrammed(conceptSearchService.searchForbiddenRelationships(
                thesaurusContext.resolveThesaurusId(), searchLang));
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
            PrimeFaces.current().executeScript("showResultSearchBar();");
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
            var preferences = thesaurusPreferenceService
                    .loadPreferencesOrNull(thesaurusId, thesaurusContext.resolveWorkLanguage());
            availableLanguages = preferences != null && preferences.languages() != null
                    ? preferences.languages()
                    : Collections.emptyList();
        } catch (RuntimeException ex) {
            availableLanguages = Collections.emptyList();
        }
    }
}
