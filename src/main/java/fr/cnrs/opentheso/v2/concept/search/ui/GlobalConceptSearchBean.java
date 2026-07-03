package fr.cnrs.opentheso.v2.concept.search.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchService;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.SelectEvent;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("v2GlobalConceptSearchBean")
@RequiredArgsConstructor
public class GlobalConceptSearchBean implements Serializable {

    private final ConceptSearchService conceptSearchService;
    private final ConsultationShellBean consultationShellBean;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final UserSession userSession;
    private final V2LocaleBean v2LocaleBean;

    private String searchLang;
    private String searchValue;
    private ConceptSearchSuggestion selectedSuggestion;
    private List<ConceptSearchResult> results = Collections.emptyList();

    private boolean exactMatch;
    private boolean startWithMatch;
    private boolean noteMatch;
    private boolean identifierMatch;
    private boolean singleResultSelected;
    private boolean searchInSpecificThesaurus;

    public void init() {
        if (StringUtils.isBlank(searchLang)) {
            searchLang = v2LocaleBean.getIdLangue();
        }
    }

    public List<ConceptSearchSuggestion> complete(String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        searchValue = query.trim();
        List<String> thesaurusIds = consultationShellBean.getSearchableThesaurusIds();
        if (thesaurusIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<ConceptSearchSuggestion> suggestions = new LinkedHashSet<>();
        for (String thesaurusId : thesaurusIds) {
            suggestions.addAll(conceptSearchService.autocomplete(
                    searchValue,
                    activeMode(),
                    thesaurusId,
                    resolveLangForThesaurus(thesaurusId),
                    !userSession.isLoggedIn()
            ));
            if (suggestions.size() >= 20) {
                break;
            }
        }
        return new ArrayList<>(suggestions);
    }

    public void onSuggestionSelect(SelectEvent<ConceptSearchSuggestion> event) throws IOException {
        ConceptSearchSuggestion suggestion = event.getObject();
        if (suggestion == null) {
            return;
        }
        selectConcept(suggestion.conceptId(), resolveThesaurusIdFromSuggestion(suggestion));
    }

    public void applySearch() throws IOException {
        List<String> thesaurusIds = consultationShellBean.getSearchableThesaurusIds();
        if (thesaurusIds.isEmpty()) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("candidat.save.msg9"));
            return;
        }
        if (StringUtils.isBlank(searchValue)) {
            searchValue = "";
        }
        searchInSpecificThesaurus = thesaurusIds.size() == 1;
        LinkedHashSet<ConceptSearchResult> merged = new LinkedHashSet<>();
        for (String thesaurusId : thesaurusIds) {
            merged.addAll(conceptSearchService.search(
                    searchValue,
                    activeMode(),
                    thesaurusId,
                    resolveLangForThesaurus(thesaurusId),
                    !userSession.isLoggedIn()
            ));
        }
        results = new ArrayList<>(merged);
        if (results.isEmpty()) {
            MessageUtils.showWarnMessage(v2LocaleBean.getMsg("search.noResult") + " !");
            return;
        }
        if (results.size() == 1) {
            ConceptSearchResult result = results.get(0);
            selectConcept(result.getConceptId(), result.getThesaurusId());
            singleResultSelected = true;
            return;
        }
        singleResultSelected = false;
        org.primefaces.PrimeFaces.current().executeScript("showResultSearchBar();");
    }

    public void selectResult(ConceptSearchResult result) throws IOException {
        if (result == null || StringUtils.isBlank(result.getConceptId())) {
            return;
        }
        selectConcept(result.getConceptId(), result.getThesaurusId());
        singleResultSelected = true;
    }

    public boolean isSelectedItem() {
        return singleResultSelected;
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

    public String resolveThesaurusTitle(String thesaurusId) {
        return consultationShellBean.resolveThesaurusTitle(thesaurusId);
    }

    private void selectConcept(String conceptId, String thesaurusId) throws IOException {
        if (StringUtils.isAnyBlank(conceptId, thesaurusId)) {
            return;
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath()
                + "/v2/thesaurus?idt=" + thesaurusId.trim()
                + "&idc=" + conceptId.trim());
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

    private String resolveLangForThesaurus(String thesaurusId) {
        if ("all".equalsIgnoreCase(searchLang)) {
            return null;
        }
        if (StringUtils.isNotBlank(searchLang)) {
            return searchLang;
        }
        return thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }

    private String resolveThesaurusIdFromSuggestion(ConceptSearchSuggestion suggestion) {
        List<String> thesaurusIds = consultationShellBean.getSearchableThesaurusIds();
        if (thesaurusIds.size() == 1) {
            return thesaurusIds.get(0);
        }
        for (String thesaurusId : thesaurusIds) {
            List<ConceptSearchSuggestion> matches = conceptSearchService.autocomplete(
                    suggestion.preferredLabel(),
                    activeMode(),
                    thesaurusId,
                    resolveLangForThesaurus(thesaurusId),
                    !userSession.isLoggedIn()
            );
            if (matches.stream().anyMatch(item -> item.refId().equals(suggestion.refId()))) {
                return thesaurusId;
            }
        }
        return thesaurusIds.isEmpty() ? null : thesaurusIds.get(0);
    }
}
