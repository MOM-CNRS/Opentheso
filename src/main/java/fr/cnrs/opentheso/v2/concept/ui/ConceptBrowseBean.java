package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNode;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptBrowseBean")
public class ConceptBrowseBean implements Serializable {

    private final ConceptReadService conceptReadService;
    private final ThesaurusContext thesaurusContext;

    private TreeNode<ConceptTreeNode> treeRoot;
    private List<ConceptGroup> groups;

    private String selectedConceptId;
    private ConceptDetail selectedConcept;

    private String searchQuery;
    private List<ConceptTreeNode> searchResults;

    public ConceptBrowseBean(ConceptReadService conceptReadService, ThesaurusContext thesaurusContext) {
        this.conceptReadService = conceptReadService;
        this.thesaurusContext = thesaurusContext;
    }

    public void init() {
        thesaurusContext.syncFromViewParams();
        String thesaurusId = thesaurusContext.getCurrentThesaurusId();
        if (thesaurusId == null) {
            return;
        }
        buildTree(thesaurusId);
        if (thesaurusContext.getIdConceptFromUri() != null) {
            loadConcept(thesaurusContext.getIdConceptFromUri());
        }
    }

    private void buildTree(String thesaurusId) {
        String lang = thesaurusContext.getWorkLanguage();
        treeRoot = new DefaultTreeNode<>("root", null, null);

        if (conceptReadService.thesaurusHasGroups(thesaurusId)) {
            groups = conceptReadService.loadGroups(thesaurusId, lang);
            for (ConceptGroup group : groups) {
                TreeNode<ConceptTreeNode> groupNode = new DefaultTreeNode<>(
                        "group",
                        new ConceptTreeNode(group.id(), thesaurusId, group.label(), group.notation(), "", false),
                        treeRoot
                );
                List<ConceptTreeNode> topConcepts = conceptReadService.loadTopConcepts(group.id(), thesaurusId, lang);
                for (ConceptTreeNode concept : topConcepts) {
                    addConceptNode(groupNode, concept);
                }
            }
        } else {
            List<ConceptTreeNode> topConcepts = conceptReadService.loadTopConceptsWithoutGroup(thesaurusId, lang);
            for (ConceptTreeNode concept : topConcepts) {
                addConceptNode(treeRoot, concept);
            }
        }
    }

    private void addConceptNode(TreeNode<ConceptTreeNode> parent, ConceptTreeNode concept) {
        TreeNode<ConceptTreeNode> node = new DefaultTreeNode<>("concept", concept, parent);
        if (concept.hasChildren()) {
            new DefaultTreeNode<>(null, node);
        }
    }

    public void onNodeExpand(org.primefaces.event.NodeExpandEvent event) {
        @SuppressWarnings("unchecked")
        TreeNode<ConceptTreeNode> node = (TreeNode<ConceptTreeNode>) event.getTreeNode();
        ConceptTreeNode concept = node.getData();
        if (concept == null || !concept.hasChildren()) {
            return;
        }
        node.getChildren().clear();
        String thesaurusId = thesaurusContext.getCurrentThesaurusId();
        String lang = thesaurusContext.getWorkLanguage();
        List<ConceptTreeNode> children = conceptReadService.loadChildren(concept.id(), thesaurusId, lang);
        for (ConceptTreeNode child : children) {
            addConceptNode(node, child);
        }
    }

    public void onConceptSelect(org.primefaces.event.NodeSelectEvent event) {
        @SuppressWarnings("unchecked")
        TreeNode<ConceptTreeNode> node = (TreeNode<ConceptTreeNode>) event.getTreeNode();
        if (node.getData() == null) {
            return;
        }
        loadConcept(node.getData().id());
    }

    public void loadConcept(String conceptId) {
        selectedConceptId = conceptId;
        selectedConcept = conceptReadService.loadDetail(
                conceptId,
                thesaurusContext.getCurrentThesaurusId(),
                thesaurusContext.getWorkLanguage()
        ).orElse(null);
    }

    public void search() {
        searchResults = conceptReadService.search(
                thesaurusContext.getCurrentThesaurusId(),
                thesaurusContext.getWorkLanguage(),
                searchQuery
        );
    }

    public void selectFromSearch(ConceptTreeNode result) {
        loadConcept(result.id());
    }

    public boolean hasNonPreferredLabels() {
        return selectedConcept != null &&
               selectedConcept.labels().stream().anyMatch(l -> !l.preferred());
    }

    public boolean hasBroaderRelations() {
        return selectedConcept != null &&
               selectedConcept.relations().stream().anyMatch(r -> r.role().startsWith("BT"));
    }

    public boolean hasNarrowerRelations() {
        return selectedConcept != null &&
               selectedConcept.relations().stream().anyMatch(r -> r.role().startsWith("NT"));
    }

    public boolean hasAssociativeRelations() {
        return selectedConcept != null &&
               selectedConcept.relations().stream().anyMatch(r -> r.role().equals("TA"));
    }
}
