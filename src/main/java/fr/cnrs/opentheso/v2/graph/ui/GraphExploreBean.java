package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.connect.Connect;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Getter
@ViewScoped
@Named("v2GraphExploreBean")
@RequiredArgsConstructor
public class GraphExploreBean implements Serializable {

    private final SelectedTheso selectedTheso;
    private final Tree tree;
    private final Connect connect;
    private final GraphVisualizationUrlService graphVisualizationUrlService;

    private String thesaurusId;
    private String conceptId;
    private String language;
    private String baseUrl;
    private String treeDataUrl;

    public void loadThesaurusGraph() {
        syncContext();
        treeDataUrl = graphVisualizationUrlService.buildThesaurusTreeDataUrl(baseUrl, thesaurusId, language);
    }

    public void loadBranchGraph() {
        syncContext();
        treeDataUrl = graphVisualizationUrlService.buildBranchTreeDataUrl(baseUrl, thesaurusId, conceptId, language);
    }

    private void syncContext() {
        thesaurusId = selectedTheso.getCurrentIdTheso();
        conceptId = tree.getIdConceptSelected();
        language = selectedTheso.getCurrentLang();
        baseUrl = connect.getLocalUri();
    }
}
