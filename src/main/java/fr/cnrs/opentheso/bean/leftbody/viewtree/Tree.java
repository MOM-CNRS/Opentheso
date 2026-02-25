package fr.cnrs.opentheso.bean.leftbody.viewtree;

import fr.cnrs.opentheso.bean.concept.DragAndDrop;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.facet.EditFacet;
import fr.cnrs.opentheso.bean.leftbody.TreeNodeData;
import fr.cnrs.opentheso.bean.leftbody.DataService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import fr.cnrs.opentheso.services.PathService;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.models.concept.Path;
import fr.cnrs.opentheso.models.concept.NodeConceptTree;
import fr.cnrs.opentheso.bean.alignment.AlignmentBean;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.leftbody.LeftBodySetting;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.bean.rightbody.RightBodySetting;
import fr.cnrs.opentheso.services.ResourceService;
import fr.cnrs.opentheso.services.TermService;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 *
 * @author miledrousset
 */
@Getter
@Setter
@SessionScoped
@Named(value = "tree")
@RequiredArgsConstructor
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Tree implements Serializable {

    private final RightBodySetting rightBodySetting;
    private final LeftBodySetting leftBodySetting;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final IndexSetting indexSetting;
    private final EditFacet editFacet;
    private final CurrentUser currentUser;
    private final AlignmentBean alignmentBean;
    private final PropositionBean propositionBean;
    private final PathService pathService;
    private final DragAndDrop dragAndDrop;
    private final ConceptService conceptService;
    private final ResourceService resourceService;
    private final TermService termService;
    private final FacetService facetService;

    private DataService dataService;
    private TreeNode selectedNode;
    private List<TreeNode> clickselectedNodes;
    private TreeNode root;
    private String idTheso, idConceptParent, idLang, idConceptSelected;
    private TreeNodeData treeNodeDataSelect;
    private ArrayList<TreeNode> selectedNodes; // enregistre les noeuds séléctionnés apres une recherche
    private boolean manySiblings = false;


    public void reset() {
        if (selectedNodes != null) {
            selectedNodes.clear();
            selectedNodes = null;
        }
        root = null;
        selectedNode = null;
        rightBodySetting.init();
        dataService = null;
        treeNodeDataSelect = null;
        idTheso = null;
        idConceptParent = null;
        idLang = null;
        manySiblings = false;
    }

    public void initialise(String idThesaurus, String idLang) {
        manySiblings = false;
        this.idTheso = idThesaurus;
        this.idLang = idLang;
        selectedTheso.setSelectedLang(idLang);
        selectedTheso.setCurrentLang(idLang);

        loadConceptTree();

        selectedNodes = new ArrayList<>();
        leftBodySetting.setIndex("0");
    }

    public void loadConceptTree() {
        dataService = new DataService();
        root = dataService.createRoot();
        addFirstNodes();
    }

    public boolean isDragAndDrop(NodeUser nodeUser) {
        if (nodeUser == null) {
            return false;
        }
        if (roleOnThesaurusBean == null) {
            return false;
        }
        return roleOnThesaurusBean.isSuperAdmin() || roleOnThesaurusBean.isAdminOnThisThesaurus() || roleOnThesaurusBean.isManagerOnThisThesaurus();
    }

    private void addFirstNodes() {

        TreeNodeData data;
        // la liste est triée par alphabétique ou notation
        var nodeConceptTrees = conceptService.getTopConcepts(idTheso, idLang, selectedTheso.isSortByNotation(),
                ObjectUtils.isEmpty(currentUser.getNodeUser()));

        if (nodeConceptTrees.size() >= 2000) {
            manySiblings = true;
        }

        for (NodeConceptTree nodeConceptTree : nodeConceptTrees) {
            data = new TreeNodeData(
                    nodeConceptTree.getIdConcept(),
                    StringUtils.isEmpty(nodeConceptTree.getTitle()) ? "("+nodeConceptTree.getIdConcept()+")" : nodeConceptTree.getTitle(),
                    selectedTheso.isSortByNotation() ? nodeConceptTree.getNotation() : null,
                    //nodeConceptTree.getNotation(),
                    false,//isgroup
                    false,//isSubGroup
                    false,//isConcept
                    true,//isTopConcept
                    "topTerm"
            );
            if (conceptService.haveChildren(idTheso, nodeConceptTree.getIdConcept())) {
                if (nodeConceptTree.getStatusConcept().equalsIgnoreCase("dep")) {
                    dataService.addNodeWithChild("deprecated", data, root);
                } else {
                    dataService.addNodeWithChild("concept", data, root);
                }
                continue;
            }

            if (facetService.isConceptHaveFacet(nodeConceptTree.getIdConcept(), idTheso)) {
                if (nodeConceptTree.getStatusConcept().equalsIgnoreCase("dep")) {
                    dataService.addNodeWithChild("deprecated", data, root);
                } else {
                    dataService.addNodeWithChild("concept", data, root);
                }
                continue;
            }

            if (nodeConceptTree.getStatusConcept().equalsIgnoreCase("dep")) {
                dataService.addNodeWithoutChild("deprecated", data, root);
            } else {
                dataService.addNodeWithoutChild("file", data, root);
            }
        }

    }

    public void onNodeExpand(NodeExpandEvent event) {
        DefaultTreeNode node = (DefaultTreeNode) event.getTreeNode();
        onNodeExpand_(node);
    }

    private void onNodeExpand_(DefaultTreeNode node) {
        leftBodySetting.setIndex("0");
        manySiblings = false;

        if (node.getChildCount() == 1 && ((TreeNode) node.getChildren().get(0)).getData().toString().equals("DUMMY")) {
            node.getChildren().remove(0);
            idConceptParent = ((TreeNodeData) node.getData()).getNodeId();
            if ("facet".equals(node.getType())) {
                addMembersOfFacet(node);
            } else {
                addConceptsChild(node);
            }
        }
    }

    public void reloadSelectedConcept(){
        initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
        if(selectedNode != null) {
            expandTreeToPath(((TreeNodeData) selectedNode.getData()).getNodeId(),
                    selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getCurrentLang());
        }
        if (PrimeFaces.current().isAjaxRequest()) {
            PrimeFaces.current().ajax().update("containerIndex:formLeftTab:tabTree:tree");
            PrimeFaces.current().ajax().update("containerIndex:languageSelect");
            PrimeFaces.current().executeScript("srollToSelected()");
        }
    }

    /// l'évennement ne focntionne pas avec tree dynamic="true"
    public void onNodeCollapse(NodeCollapseEvent event) {
        leftBodySetting.setIndex("0");
        event.getTreeNode().setExpanded(false);
    }

    private void addMembersOfFacet(TreeNode parent) {
        var nodeIdValues = facetService.getAllMembersOfFacetSorted(((TreeNodeData) parent.getData()).getNodeId(),
                selectedTheso.getCurrentLang(), idTheso);

        for (NodeIdValue nodeIdValue : nodeIdValues) {
            TreeNodeData data = new TreeNodeData(
                    nodeIdValue.getId(),
                    nodeIdValue.getValue().isEmpty() ? "(" + nodeIdValue.getId() + ")" : nodeIdValue.getValue(),
                    null,
                    false,
                    false,
                    true,
                    false,
                    "facetMember");
            data.setIdFacetParent(((TreeNodeData) parent.getData()).getNodeId());
            boolean haveConceptChild = conceptService.haveChildren(idTheso, nodeIdValue.getId());
            if (haveConceptChild) {
                if (conceptService.isDeprecated(nodeIdValue.getId(), idTheso)) {
                    dataService.addNodeWithChild("deprecated", data, parent);
                } else {
                    dataService.addNodeWithChild("concept", data, parent);
                }
            } else {
                if (conceptService.isDeprecated(nodeIdValue.getId(), idTheso)) {
                    dataService.addNodeWithoutChild("deprecated", data, parent);
                } else {
                    dataService.addNodeWithoutChild("file", data, parent);
                }
            }
        }
    }

    /**
     * Nouvelle méthode en utilisant Plpgsql beaucoup plus rapide
     */
    private void addConceptsChild(TreeNode parent) {

        List<NodeConceptTree> nodeConceptTrees = resourceService.getConceptsNTForTree(idTheso, ((TreeNodeData) parent.getData()).getNodeId(),
                selectedTheso.getCurrentLang(), selectedTheso.isSortByNotation(), !ObjectUtils.isEmpty(currentUser.getNodeUser()));

        if (nodeConceptTrees.size() >= 2000) {
            manySiblings = true;
        }

        for (NodeConceptTree nodeConceptTree : nodeConceptTrees) {
            if (nodeConceptTree.getIdConcept() == null) {
                continue;
            }
            if (nodeConceptTree.isFacet()) {
                TreeNodeData data = new TreeNodeData(
                        nodeConceptTree.getIdConcept(),
                        StringUtils.isEmpty(nodeConceptTree.getTitle()) ? "(" + nodeConceptTree.getIdConcept() + ")" : nodeConceptTree.getTitle(),
                        null,
                        false,
                        false,
                        true,
                        false,
                        "facet"
                );
                if (nodeConceptTree.isHaveChildren()) {
                    dataService.addNodeWithChild("facet", data, parent);
                } else {
                    dataService.addNodeWithoutChild("facet", data, parent);
                }
            }
            if (nodeConceptTree.isTerm()) {
                TreeNodeData data = new TreeNodeData(
                        nodeConceptTree.getIdConcept(),
                        StringUtils.isEmpty(nodeConceptTree.getTitle())? "(" + nodeConceptTree.getIdConcept() + ")" : nodeConceptTree.getTitle(),
                        nodeConceptTree.getNotation(),
                        false,//isgroup
                        false,//isSubGroup
                        true,//isConcept
                        false,//isTopConcept
                        "term"
                );
                if (nodeConceptTree.isHaveChildren()) {
                    if (nodeConceptTree.getStatusConcept().equalsIgnoreCase("dep")) {
                        dataService.addNodeWithChild("deprecated", data, parent);
                    } else {
                        dataService.addNodeWithChild("concept", data, parent);
                    }
                } else {
                    if (nodeConceptTree.getStatusConcept() != null
                            && nodeConceptTree.getStatusConcept().equalsIgnoreCase("dep")) {
                        dataService.addNodeWithoutChild("deprecated", data, parent);
                    } else {
                        dataService.addNodeWithoutChild("file", data, parent);
                    }
                }
            }
        }
    }

    public List<NodeTree> searchFacettesForTree(String conceptParentId, String idTheso, String idLang) {
        List<NodeTree> facaets = new ArrayList<>();
        List<NodeIdValue> nodeIdValues = facetService.getAllIdValueFacetsOfConcept(conceptParentId, idTheso, idLang);
        nodeIdValues.forEach(facette -> {
            TreeNodeData data = new TreeNodeData(
                    facette.getId(),
                    facette.getValue().isEmpty() ? "(" + facette.getId() + ")" : facette.getValue(),
                    null,
                    false,
                    false,
                    true,
                    false,
                    "facet"
            );

            NodeTree nodeTree = new NodeTree();
            nodeTree.setIdConcept(data.getNodeId());
            nodeTree.setIdParent(conceptParentId);
            nodeTree.setPreferredTerm(data.getName());
            facaets.add(nodeTree);
        });
        return facaets;
    }

    /////// pour l'ajout d'un fils supplementaire après un ajout de concept
    public void addNewChild(TreeNode parent, String idConcept, String idTheso, String idLang, String notation) {

        String label = termService.getLexicalValueOfConcept(idConcept, idTheso, idLang);
        if (label == null || label.isEmpty()) {
            label = "(" + idConcept + ")";
        }
        var data = new TreeNodeData(idConcept, label, notation,
                false,//isgroup
                false,//isSubGroup
                true,//isConcept
                false,//isTopConcept
                "term");

        if (conceptService.haveChildren(idTheso, idConcept)) {
            dataService.addNodeWithChild("concept", data, parent);
        } else {
            dataService.addNodeWithoutChild("file", data, parent);
        }

    }

    public void addNewFacet(TreeNode parent, String facetName, String idFacet) {
        if (parent.getChildCount() == 1 && ((TreeNode)parent.getChildren().get(0)).getData().toString().equals("DUMMY")) {
            return;
        }
        TreeNodeData data = new TreeNodeData(
                idFacet,
                facetName,
                "",
                false,//isgroup
                false,//isSubGroup
                true,//isConcept
                false,//isTopConcept
                "facet");

        dataService.addNodeWithoutChild("facet", data, parent);
    }

    private void addFacettes(TreeNode parent) {
        List<NodeIdValue> nodeIdValues = facetService.getAllIdValueFacetsOfConcept(
                ((TreeNodeData) parent.getData()).getNodeId(), idTheso, selectedTheso.getCurrentLang());
        nodeIdValues.forEach(facette -> {
            TreeNodeData data = new TreeNodeData(
                    facette.getId(),
                    facette.getValue().isEmpty() ? "(" + facette.getId() + ")" : facette.getValue(),
                    null,
                    false,
                    false,
                    true,
                    false,
                    "facet"
            );

            if (facetService.isFacetHaveMembers(facette.getId(), idTheso)) {
                dataService.addNodeWithChild("facet", data, parent);
            } else {
                dataService.addNodeWithoutChild("facet", data, parent);
            }
        });
    }

    public void selectThisFacet(String idFacet) {
        /// chargement de l'arbre jusqu'au concept Parent de la Facette
        String idConceptParentOfFacet = facetService.getIdConceptParentOfFacet(idFacet, idTheso);
        expandTreeToPath(idConceptParentOfFacet, idTheso, idLang);
        onNodeExpand_((DefaultTreeNode) selectedNode);

        // rechercher la facette dans les fils et la sélectionner
        expandToFacet(idFacet);

        indexSetting.setIsFacetSelected(true);
        editFacet.initEditFacet(idFacet, idTheso, idLang);
        rightBodySetting.setIndex("0");
    }

    private void expandToFacet(String idFacet) {
        selectedNode.setExpanded(true);
        List<TreeNode> treeNodes = selectedNode.getChildren();
        for (TreeNode treeNode : treeNodes) {
            if (((TreeNodeData) treeNode.getData()).getNodeType().equalsIgnoreCase("facet")) {
                if (((TreeNodeData) treeNode.getData()).getNodeId().equalsIgnoreCase(idFacet)) {
                    selectedNode.setSelected(false);
                    selectedNode = treeNode;
                    selectedNode.setSelected(true);
                }
            }
        }
    }

    /**
     * récupération du noeud sélectionné dans l'arbre et appliquer les actions
     */
    public void onNodeSelect(NodeSelectEvent event) {
        DefaultTreeNode node = (DefaultTreeNode) event.getTreeNode();
        onNodeSelectByNode(node);
        if(dragAndDrop.isCopyOn()) PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    /**
     * appliquer les actions sur un noeud fourni par l'utilisateur
     */
    public void onNodeSelectByNode(DefaultTreeNode node) {

        propositionBean.setRubriqueVisible(false);

        if(selectedNode == null) {
            try {
                selectedTheso.reloadSelectedTheso();
            } catch (Exception ex){}
            return;
        }
        treeNodeDataSelect = (TreeNodeData) selectedNode.getData();

        if (!"facet".equals(node.getType())) {
            indexSetting.setIsFacetSelected(false);
            idConceptParent = ((TreeNodeData) selectedNode.getData()).getNodeId();

            rightBodySetting.setShowConceptToOn();
            conceptBean.getConceptForTree(idTheso,
                    ((TreeNodeData) selectedNode.getData()).getNodeId(), idLang, currentUser);

            idConceptSelected = ((TreeNodeData) selectedNode.getData()).getNodeId();
            if (rightBodySetting.getIndex().equalsIgnoreCase("2")) {
                indexSetting.setIsValueSelected(true);

                alignmentBean.initAlignementByStep(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeFullConcept().getIdentifier(), conceptBean.getSelectedLang());
                alignmentBean.getIdsAndValues2(conceptBean.getSelectedLang(), selectedTheso.getCurrentIdTheso());
                alignmentBean.setAllAlignementFound(new ArrayList<>());
                alignmentBean.setAllAlignementVisible(true);
                alignmentBean.setPropositionAlignementVisible(false);
                alignmentBean.setManageAlignmentVisible(false);
                alignmentBean.setComparaisonVisible(false);
            } else {
                rightBodySetting.setIndex("0");
            }
        } else {
            indexSetting.setIsFacetSelected(true);
            editFacet.initEditFacet(((TreeNodeData) node.getData()).getNodeId(), idTheso, idLang);
        }

        PrimeFaces.current().ajax().update("indexTitle");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("containerIndex:formLeftTab:tabTree:graph");
    }

    public boolean isURLAvailable(String urlString) {
        if (StringUtils.isNotEmpty(urlString)) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void onTabConceptChange(TabChangeEvent event) {
        if (event.getTab().getId().equals("viewTabAlignement")) {
            rightBodySetting.setIndex("2");
            indexSetting.setIsValueSelected(true);

            alignmentBean.setAllAlignementVisible(true);
            alignmentBean.setPropositionAlignementVisible(false);
            alignmentBean.setManageAlignmentVisible(false);
            alignmentBean.setComparaisonVisible(false);

            alignmentBean.initAlignementByStep(selectedTheso.getCurrentIdTheso(),
                    conceptBean.getNodeFullConcept().getIdentifier(),
                    conceptBean.getSelectedLang());

            alignmentBean.getIdsAndValues2(conceptBean.getSelectedLang(), selectedTheso.getCurrentIdTheso());
        }
        if (event.getTab().getId().equals("viewTabGroup")) {
            rightBodySetting.setIndex("1");
            indexSetting.setIsValueSelected(true);
        }
        if (event.getTab().getId().equals("viewTabConcept")) {
            conceptBean.getConcept(selectedTheso.getCurrentIdTheso(),
                    conceptBean.getNodeFullConcept().getIdentifier(),
                    conceptBean.getSelectedLang(), currentUser);

            rightBodySetting.setIndex("0");
            indexSetting.setIsValueSelected(true);
        }
    }

    public void onTabTreeChange(TabChangeEvent event) {
        if (event.getTab().getId().equals("viewTabTree")) {
            leftBodySetting.setIndex("0");
        }
        if (event.getTab().getId().equals("viewTabList")) {
            leftBodySetting.setIndex("1");
        }
        if (event.getTab().getId().equals("viewTabGroups")) {
            leftBodySetting.setIndex("2");
        }
        if (event.getTab().getId().equals("viewTabConceptTree")) {
            leftBodySetting.setIndex("3");
        }
    }

    public boolean isGrapheLinkVisible() {
        return !StringUtils.isEmpty(idConceptSelected);
    }

    /**
     * permet de déplier l'arbre suivant le Path ou les paths en paramètre
     */
    public void expandTreeToPath(String idConcept, String idTheso, String idLang) {
        List<String> graphPaths = pathService.getGraphOfConcept(idConcept, idTheso);
        List<List<String>> paths = pathService.getPathFromGraph(graphPaths);

        if (root == null) {
            initialise(idTheso, idLang);
        }
        if (paths == null || paths.isEmpty()) {
            return;
        }

        // cas de changement de langue pendant la navigation dans les concepts
        // il faut reconstruire l'arbre dès le début
        if (idLang != null && !idLang.equalsIgnoreCase(this.idLang)) {
            initialise(idTheso, idLang);
        }

        if (!paths.isEmpty()) {
            // pour déselectionner les noeuds avant de séléctionner le neoud trouvé
            selectedNodes.forEach((selectedNode1) -> {
                selectedNode1.setSelected(false);
            });
            if (selectedNode != null) {
                selectedNode.setSelected(false);
            }
            selectedNodes.clear();
        }

        TreeNode treeNodeParent = root;
        treeNodeParent.setExpanded(true);
        for (List<String> path : paths) {
            for (String idC : path) {
                treeNodeParent = selectChildNode(treeNodeParent, idC);
                if (treeNodeParent == null) {
                    // erreur de cohérence
                    return;
                }
                // compare le dernier élément au concept en cours, si oui, on expand pas, sinon, erreur ...
                if (!((TreeNodeData) treeNodeParent.getData()).getNodeId().equalsIgnoreCase(path.get(path.size() - 1))) {
                    treeNodeParent.setExpanded(true);
                }                
            }
            treeNodeParent.setSelected(true);
            selectedNodes.add(treeNodeParent);
            selectedNode = treeNodeParent;
            treeNodeParent = root;            
        }
        PrimeFaces.current().executeScript("srollToSelected();");
    }

    public void expandTreeToPath2(String idConcept, String idTheso, String idLang, String idFacette) {

        List<Path> paths = pathService.getPathOfConcept(idConcept, idTheso);
        
        paths.get(0).getPath().add(idFacette);

        if (root == null) {
            initialise(idTheso, idLang);
        }

        if (idLang != null && !idLang.equalsIgnoreCase(this.idLang)) {
            initialise(idTheso, idLang);
        }

        if (!paths.isEmpty()) {
            // pour déselectionner les noeuds avant de séléctionner le neoud trouvé
            selectedNodes.forEach((selectedNode1) -> {
                selectedNode1.setSelected(false);
            });
            if (selectedNode != null) {
                selectedNode.setSelected(false);
            }

            selectedNodes.clear();
        }

        TreeNode treeNodeParent = root;
        treeNodeParent.setExpanded(true);
        for (Path thisPath : paths) {
            for (String idC : thisPath.getPath()) {
                treeNodeParent = selectChildNode(treeNodeParent, idC);
                if (treeNodeParent == null) {
                    // erreur de cohérence
                    return;
                }
                // compare le dernier élément au concept en cours, si oui, on expand pas, sinon, erreur ...
                if (!((TreeNodeData) treeNodeParent.getData()).getNodeId().equalsIgnoreCase(thisPath.getPath().get(thisPath.getPath().size() - 1))) {
                    treeNodeParent.setExpanded(true);
                }
            }
            treeNodeParent.setSelected(true);
            selectedNodes.add(treeNodeParent);
            selectedNode = treeNodeParent;
            treeNodeParent = root;
        }
        leftBodySetting.setIndex("0");
        PrimeFaces.current().executeScript("srollToSelected();");
    }

    public boolean isGraphNotVisible() {
        return selectedNode == null || selectedNode.isLeaf();
    }

    public String getColor() {
        if (isGraphNotVisible()) {
            return "#d1d3e2";
        } else {
            return "#F47B2A";
        }
    }

    /**
     * permet de déplier l'arbre suivant le Path ou les paths en paramètre On
     * reconstruit l'arbre dès le début suite à des modifications
     */
    public void initAndExpandTreeToPath(String idConcept, String idTheso, String idLang) {

        List<Path> paths = pathService.getPathOfConcept(idConcept, idTheso);

        initialise(idTheso, idLang);

        if (!paths.isEmpty()) {
            // pour déselectionner les noeuds avant de séléctionner le neoud trouvé
            selectedNodes.forEach((selectedNode1) -> {
                selectedNode1.setSelected(false);
            });
            if (selectedNode != null) {
                selectedNode.setSelected(false);
            }
            selectedNodes.clear();
        }

        TreeNode treeNodeParent = root;
        treeNodeParent.setExpanded(true);
        for (Path thisPath : paths) {
            for (String idC : thisPath.getPath()) {
                treeNodeParent = selectChildNode(treeNodeParent, idC);
                if (treeNodeParent == null) {
                    // erreur de cohérence
                    return;
                }
                // compare le dernier élément au concept en cours, si oui, on expand pas, sinon, erreur ...
                if (!((TreeNodeData) treeNodeParent.getData()).getNodeId()
                        .equalsIgnoreCase(thisPath.getPath().get(thisPath.getPath().size() - 1))) {
                    treeNodeParent.setExpanded(true);
                }
            }
            treeNodeParent.setSelected(true);
            selectedNodes.add(treeNodeParent);
            selectedNode = treeNodeParent;
            treeNodeParent = root;
        }
        leftBodySetting.setIndex("0");
    }

    /**
     * permet de controler si la branche est chargée, on se positionne sur le
     * concept, sinon, on récupère les fils et on se positionne sur le concept
     * Cas d'un noeud Facette : on zappe le neoud puisque le concept est sous
     * cette facette, ensuite, on se positionne sur le concept
     */
    private TreeNode selectChildNode(TreeNode treeNodeParent, String idConceptChildToFind) {
        // test si les fils ne sont pas construits
        if (treeNodeParent.getChildCount() == 1 && ((TreeNode) treeNodeParent.getChildren().get(0)).getData().toString().equals("DUMMY")) {
            treeNodeParent.getChildren().remove(0);

            if ("facet".equals(treeNodeParent.getType())) {
                addMembersOfFacet(treeNodeParent);
            } else {
                addConceptsChild(treeNodeParent);
            }
        }
        List<TreeNode> treeNodes = treeNodeParent.getChildren();

        for (TreeNode treeNode : treeNodes) {
            //// permet de sauter le noeud de type Facette et passer au concept membre
            if (((TreeNodeData) treeNode.getData()).getNodeType().equalsIgnoreCase("facet")) {
                try {
                    //String idFacet = ((TreeNodeData) treeNode.getData()).getNodeId();
                    if (facetService.isFacetHaveThisMember(((TreeNodeData) treeNode.getData()).getNodeId(), idConceptChildToFind, idTheso)) {
                        if (treeNode.getChildCount() == 1 && ((TreeNode) treeNode.getChildren().get(0)).getData().toString().equals("DUMMY")) {
                            treeNode.getChildren().remove(0);
                            addMembersOfFacet(treeNode);
                        }
                        return selectChildNodeFromFacet(treeNode, idConceptChildToFind);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            //// fin 
            if (((TreeNodeData) treeNode.getData()).getNodeId().equalsIgnoreCase(idConceptChildToFind)) {
                return treeNode;
            }
        }
        // pas de noeud trouvé dans les fils
        return null;
    }

    private TreeNode selectChildNodeFromFacet(TreeNode treeNodeFacet, String idConceptChildToFind) {
        List<TreeNode> treeNodes = treeNodeFacet.getChildren();

        for (TreeNode treeNode : treeNodes) {
            if (((TreeNodeData) treeNode.getData()).getNodeId().equalsIgnoreCase(idConceptChildToFind)) {
                treeNodeFacet.setExpanded(true);
                return treeNode;
            }
        }
        // pas de noeud trouvé dans les fils
        return null;
    }

    public void expandAllNode() {
        dataService = null;
        dataService = new DataService();
        root = dataService.createRoot();
        addFirstNodes();
        List<TreeNode> treeNodes = root.getChildren();

        for (TreeNode treeNode : treeNodes) {
            expandedAllRecursively(treeNode, true);
        }
    }

    private void expandedAllRecursively(TreeNode node, boolean expanded) {
        if (node.getChildCount() == 1 && ((TreeNode) node.getChildren().get(0)).getData().toString().equals("DUMMY")) {
            node.getChildren().remove(0);
            addConceptsChild(node);
        }
    }
}
