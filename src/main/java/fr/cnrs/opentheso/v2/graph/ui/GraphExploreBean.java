package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ConceptSelectionSource;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@Named("v2GraphExploreBean")
@RequiredArgsConstructor
public class GraphExploreBean implements Serializable {

    private final ThesaurusContext thesaurusContext;
    private final ConceptSelectionSource conceptSelectionSource;
    private final ApplicationUriService applicationUriService;
    private final GraphVisualizationUrlService graphVisualizationUrlService;

    private String conceptIdFromUri;
    private String treeDataUrl;

    public void loadThesaurusGraph() {
        thesaurusContext.syncFromViewParams();
        String baseUrl = applicationUriService.resolveApplicationRootUrl();
        treeDataUrl = graphVisualizationUrlService.buildThesaurusTreeDataUrl(
                baseUrl,
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public void loadBranchGraph() {
        thesaurusContext.syncFromViewParams();
        String conceptId = resolveConceptId();
        String baseUrl = applicationUriService.resolveApplicationRootUrl();
        treeDataUrl = graphVisualizationUrlService.buildBranchTreeDataUrl(
                baseUrl,
                thesaurusContext.resolveThesaurusId(),
                conceptId,
                thesaurusContext.resolveWorkLanguage()
        );
    }

    private String resolveConceptId() {
        if (StringUtils.isNotBlank(conceptIdFromUri)) {
            return conceptIdFromUri.trim();
        }
        if (StringUtils.isNotBlank(thesaurusContext.getIdConceptFromUri())) {
            return thesaurusContext.getIdConceptFromUri();
        }
        return conceptSelectionSource.getSelectedConceptId().orElse(null);
    }
}
