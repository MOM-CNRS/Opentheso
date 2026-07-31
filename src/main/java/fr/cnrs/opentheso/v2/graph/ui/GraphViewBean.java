package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.v2.graph.policy.GraphAccessPolicy;
import fr.cnrs.opentheso.v2.graph.service.GraphConceptSearchService;
import fr.cnrs.opentheso.v2.graph.service.GraphNeo4jExportService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewCommandService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewReadService;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.component.chip.Chip;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named("v2GraphViewBean")
public class GraphViewBean implements Serializable {

    private final UserSession userSession;
    private final GraphAccessPolicy graphAccessPolicy;
    private final GraphViewReadService graphViewReadService;
    private final GraphViewCommandService graphViewCommandService;
    private final GraphVisualizationUrlService graphVisualizationUrlService;
    private final GraphNeo4jExportService graphNeo4jExportService;
    private final GraphConceptSearchService graphConceptSearchService;
    private final ThesaurusSelectionService thesaurusSelectionService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final ConceptReadService conceptReadService;
    private final ApplicationUriService applicationUriService;

    private int selectedViewId = -1;
    private String newViewName;
    private String newViewDescription;
    private String selectedIdTheso;
    private List<GraphExportEntry> newViewExports = new ArrayList<>();
    private ConceptSearchSuggestion searchSelected;
    private List<GraphViewSummary> graphViews = new ArrayList<>();

    public boolean isScreenAvailable() {
        return graphAccessPolicy.canAccessModule(userSession);
    }

    public void load() {
        if (!isScreenAvailable()) {
            return;
        }
        refreshViews();
    }

    public void refreshViews() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            graphViews = List.of();
            return;
        }
        graphViews = graphViewReadService.reloadViewsForUser(userId);
        attachVisualizationUrls(graphViews);
        selectedIdTheso = null;
        searchSelected = null;
    }

    public void initNewViewDialog() {
        selectedViewId = -1;
        newViewName = null;
        newViewDescription = null;
        newViewExports = new ArrayList<>();
    }

    public void initEditViewDialog(String id) {
        var view = graphViewReadService.loadView(id);
        if (view == null) {
            return;
        }
        selectedViewId = view.getId();
        newViewName = view.getName();
        newViewDescription = view.getDescription();
        newViewExports = new ArrayList<>(view.getExports());
    }

    public List<ConceptSearchSuggestion> getAutoComplete(String value) {
        if (StringUtils.isBlank(selectedIdTheso)) {
            return List.of();
        }
        return graphConceptSearchService.searchForRelation(value, selectedIdTheso);
    }

    public void onSelectThesaurus(AjaxBehaviorEvent event) {
        String idTheso = ((Chip) event.getSource()).getLabel();
        String title = thesaurusSelectionService.resolve(idTheso).title();
        MessageUtils.showInformationMessage("Thesaurus : " + title);
    }

    public void onSelectThesaurusConcept(AjaxBehaviorEvent event) {
        var idValue = ((Chip) event.getSource()).getLabel().split(",");
        var idThesaurus = idValue[0].trim();
        var idConcept = idValue[1].trim();
        String title = thesaurusSelectionService.resolve(idThesaurus).title();
        String lang = thesaurusWorkLanguageService.resolveForThesaurus(idThesaurus);
        String conceptLabel = conceptReadService.loadSummary(idThesaurus, idConcept, lang)
                .map(summary -> summary.preferredLabel())
                .orElse(idConcept);
        MessageUtils.showInformationMessage("Thesaurus : " + title);
        MessageUtils.showInformationMessage("Concept : " + conceptLabel);
    }

    public String generateGraphVisualizationUrl(String viewId) throws URISyntaxException {
        for (GraphViewSummary view : graphViews) {
            if (String.valueOf(view.getId()).equals(viewId) && StringUtils.isNotBlank(view.getVisualizationUrl())) {
                return view.getVisualizationUrl();
            }
        }
        String lang = graphVisualizationUrlService.resolveWorkLanguageForThesaurus(selectedThesoFromView(viewId));
        return graphVisualizationUrlService.buildVisualizationUrl(viewId, applicationUriService.resolveApplicationBaseUrl(), lang);
    }

    public void exportToNeo4J(String viewId) {
        graphNeo4jExportService.exportView(viewId, applicationUriService.resolveApplicationBaseUrl());
        refreshViews();
    }

    public void removeView(String viewId) {
        graphViewCommandService.deleteView(viewId);
        MessageUtils.showInformationMessage("Vue supprimée avec succès");
        refreshViews();
    }

    public void addDataToNewViewList() {
        if (selectedViewId == -1 || StringUtils.isBlank(selectedIdTheso)) {
            return;
        }

        String conceptId = searchSelected == null || StringUtils.isBlank(searchSelected.conceptId())
                ? null
                : searchSelected.conceptId();

        if (!graphViewCommandService.addExportEntry(selectedViewId, selectedIdTheso, conceptId)) {
            MessageUtils.showWarnMessage("Cette combinaison existe déjà !");
            refreshViews();
            return;
        }

        newViewExports.add(new GraphExportEntry(selectedIdTheso, conceptId));
        refreshViews();
        initEditViewDialog(String.valueOf(selectedViewId));
    }

    public void applyView() {
        if (StringUtils.isBlank(newViewName) || StringUtils.isBlank(newViewDescription)) {
            MessageUtils.showErrorMessage("Une vue doit possèder un nom et une description");
            return;
        }

        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }

        if (selectedViewId == -1) {
            selectedViewId = graphViewCommandService.createView(newViewName, newViewDescription, userId);
            MessageUtils.showInformationMessage("Vue créée avec succès");
        } else {
            graphViewCommandService.updateView(selectedViewId, newViewName, newViewDescription);
            MessageUtils.showInformationMessage("Vue modifiée avec succès");
        }
        refreshViews();
    }

    public void removeExportedDataRow(String thesaurusId, String conceptId) {
        newViewExports.stream()
                .filter(entry -> conceptId == null
                        ? entry.thesaurusId().equals(thesaurusId) && entry.conceptId() == null
                        : thesaurusId.equals(entry.thesaurusId()) && conceptId.equals(entry.conceptId()))
                .findFirst()
                .ifPresent(entry -> {
                    graphViewCommandService.removeExportEntry(selectedViewId, entry.thesaurusId(), entry.conceptId());
                    newViewExports.remove(entry);
                });
        refreshViews();
        if (selectedViewId != -1) {
            initEditViewDialog(String.valueOf(selectedViewId));
        }
    }

    private void attachVisualizationUrls(List<GraphViewSummary> views) {
        if (views == null || views.isEmpty()) {
            return;
        }
        String baseUrl = applicationUriService.resolveApplicationBaseUrl();
        for (GraphViewSummary view : views) {
            try {
                String thesaurusId = view.hasExports() ? view.getExports().get(0).thesaurusId() : null;
                String lang = graphVisualizationUrlService.resolveWorkLanguageForThesaurus(thesaurusId);
                view.setVisualizationUrl(graphVisualizationUrlService.buildVisualizationUrl(view, baseUrl, lang));
            } catch (Exception ignored) {
                view.setVisualizationUrl(null);
            }
        }
    }

    private String selectedThesoFromView(String viewId) {
        var view = graphViewReadService.loadView(viewId);
        if (view == null || !view.hasExports()) {
            return null;
        }
        return view.getExports().get(0).thesaurusId();
    }
}
