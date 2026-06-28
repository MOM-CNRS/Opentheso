package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.v2.graph.policy.GraphAccessPolicy;
import fr.cnrs.opentheso.v2.graph.service.GraphNeo4jExportService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewCommandService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewReadService;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CurrentUser currentUser;
    private final GraphViewReadService graphViewReadService;
    private final GraphViewCommandService graphViewCommandService;
    private final GraphVisualizationUrlService graphVisualizationUrlService;
    private final GraphNeo4jExportService graphNeo4jExportService;
    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;
    private final TermService termService;
    private final SearchService searchService;

    private int selectedViewId = -1;
    private String newViewName;
    private String newViewDescription;
    private String selectedIdTheso;
    private List<GraphExportEntry> newViewExports = new ArrayList<>();
    private NodeSearchMini searchSelected;
    private List<GraphViewSummary> graphViews = new ArrayList<>();

    public boolean isScreenAvailable() {
        return GraphAccessPolicy.canAccessModule(userSession);
    }

    public void load() {
        if (!isScreenAvailable()) {
            return;
        }
        refreshViews();
    }

    public void refreshViews() {
        graphViews = graphViewReadService.reloadViewsForUser(currentUser.getNodeUser().getIdUser());
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

    public List<NodeSearchMini> getAutoComplete(String value) {
        if (StringUtils.isBlank(selectedIdTheso)) {
            return List.of();
        }
        String idLang = preferenceService.getWorkLanguageOfThesaurus(selectedIdTheso);
        if (idLang == null) {
            return List.of();
        }
        return searchService.searchAutoCompletionForRelation(value, idLang, selectedIdTheso, true);
    }

    public void onSelectThesaurus(AjaxBehaviorEvent event) {
        String idTheso = ((Chip) event.getSource()).getLabel();
        String idLang = preferenceService.getWorkLanguageOfThesaurus(idTheso);
        MessageUtils.showInformationMessage("Thesaurus : " + thesaurusService.getTitleOfThesaurus(idTheso, idLang));
    }

    public void onSelectThesaurusConcept(AjaxBehaviorEvent event) {
        var idValue = ((Chip) event.getSource()).getLabel().split(",");
        var idThesaurus = idValue[0].trim();
        var idConcept = idValue[1].trim();
        var idLang = preferenceService.getWorkLanguageOfThesaurus(idThesaurus);
        MessageUtils.showInformationMessage("Thesaurus : " + thesaurusService.getTitleOfThesaurus(idThesaurus, idLang));
        MessageUtils.showInformationMessage("Concept : "
                + termService.getLexicalValueOfConcept(idConcept, idThesaurus, idLang));
    }

    public String generateGraphVisualizationUrl(String viewId) throws URISyntaxException {
        String lang = graphVisualizationUrlService.resolveWorkLanguageForThesaurus(selectedThesoFromView(viewId));
        return graphVisualizationUrlService.buildVisualizationUrl(viewId, resolveOpenthesoBaseUrl(), lang);
    }

    public void exportToNeo4J(String viewId) {
        graphNeo4jExportService.exportView(viewId, resolveOpenthesoBaseUrl());
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

        String conceptId = searchSelected == null || StringUtils.isBlank(searchSelected.getIdConcept())
                ? null
                : searchSelected.getIdConcept();

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

        if (selectedViewId == -1) {
            int userId = currentUser.getNodeUser().getIdUser();
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

    private String resolveOpenthesoBaseUrl() {
        var request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        var protocol = request.isSecure() ? "https://" : "http://";
        return protocol + request.getHeader("host") + request.getContextPath();
    }

    private String selectedThesoFromView(String viewId) {
        var view = graphViewReadService.loadView(viewId);
        if (view == null || !view.hasExports()) {
            return null;
        }
        return view.getExports().get(0).thesaurusId();
    }
}
