package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.terms.NodeBT;
import fr.cnrs.opentheso.models.concept.NodeConcept;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.bean.leftbody.TreeNodeData;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;


@Getter
@Setter
@Slf4j
@SessionScoped
@RequiredArgsConstructor
@Named(value = "dragAndDrop")
public class DragAndDrop implements Serializable {

    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final Tree tree;
    private final GroupService groupService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptService conceptService;
    private final FacetService facetService;
    private final RelationService relationService;

    private boolean copyOn, isValidPaste, isDragAndDrop, isDropToRoot, isGroupToCut;
    private List<NodeBT> nodeBTsToCut;
    private NodeConcept nodeConceptDrag, nodeConceptDrop;
    private List<BTNode> groupNodeBtToCut;
    private List<TreeNode> nodesToConfirme;
    private List<GroupNode> groupNodeToAdd, groupNodeToCut;
    private List<NodeGroup> nodeGroupsToCut,  nodeGroupsToAdd;
    private TreeNode dragNode, dropNode;


    public void clear(){
        if(nodeBTsToCut != null){
            nodeBTsToCut.clear();
            nodeBTsToCut = null;
        }
        if(nodeGroupsToCut != null){
            nodeGroupsToCut.clear();
            nodeGroupsToCut = null;
        }   
        if(nodeGroupsToAdd != null){
            nodeGroupsToAdd.clear();
            nodeGroupsToAdd = null;
        }
        if(nodeConceptDrag != null){
            nodeConceptDrag.clear();
            nodeConceptDrag = null;
        }    
        if(nodeConceptDrop != null){
            nodeConceptDrop.clear();
            nodeConceptDrop = null;
        }             
        
        dragNode = null;
        dropNode = null;        
    }

    public void reset() {
        dragNode = null;
        dropNode = null;
        if(nodeBTsToCut != null)
            nodeBTsToCut.clear();
        if(nodeGroupsToCut != null)
            nodeGroupsToCut.clear();
        if(nodeGroupsToAdd != null)
            nodeGroupsToAdd.clear();          
        copyOn = false;
        isValidPaste = false;
        
        if(nodeConceptDrag != null)
            nodeConceptDrag.clear();
        if(nodeConceptDrop != null)
            nodeConceptDrop.clear();
        nodeConceptDrop = null;
        
        isDragAndDrop = false;
        isDropToRoot = false;
        isGroupToCut = false;
    }

    public void setBTsToCut() {
        if (nodeConceptDrag == null) {
            return;
        }
        if (nodeBTsToCut == null) {
            nodeBTsToCut = new ArrayList<>();
        }
        for (NodeBT nodeBT : nodeConceptDrag.getNodeBT()) {
            NodeBT nodeBT1 = new NodeBT();
            nodeBT1.setIdConcept(nodeBT.getIdConcept());
            nodeBT1.setTitle(nodeBT.getTitle());
            nodeBT1.setSelected(true);
            nodeBTsToCut.add(nodeBT1);
        }
    }

    public List<NodeBT> setBTsToCut(NodeConcept concept) {

        List<NodeBT> Bts = new ArrayList<>();

        for (NodeBT nodeBT : concept.getNodeBT()) {
            NodeBT nodeBT1 = new NodeBT();
            nodeBT1.setIdConcept(nodeBT.getIdConcept());
            nodeBT1.setTitle(nodeBT.getTitle());
            nodeBT1.setSelected(true);
            Bts.add(nodeBT1);
        }
        return Bts;
    }

    public void setGroupsToCut() {
        if (nodeConceptDrag == null) {
            return;
        }
        if(nodeGroupsToCut == null)
            nodeGroupsToCut = new ArrayList<>();
        for (NodeGroup nodeGroup : nodeConceptDrag.getNodeConceptGroup()) {
            NodeGroup nodeGroup1 = new NodeGroup();
            nodeGroup1.getConceptGroup().setIdGroup(nodeGroup.getConceptGroup().getIdGroup());
            nodeGroup1.setLexicalValue(nodeGroup.getLexicalValue());
            nodeGroup1.setSelected(true);
            nodeGroupsToCut.add(nodeGroup1);
        }
        if(nodeGroupsToAdd == null)
            nodeGroupsToAdd = new ArrayList<>();
        if(nodeConceptDrop != null) {
            for (NodeGroup nodeGroup : nodeConceptDrop.getNodeConceptGroup()) {
                NodeGroup nodeGroup1 = new NodeGroup();
                nodeGroup1.getConceptGroup().setIdGroup(nodeGroup.getConceptGroup().getIdGroup());
                nodeGroup1.setLexicalValue(nodeGroup.getLexicalValue());
                nodeGroup1.setSelected(true);
                nodeGroupsToAdd.add(nodeGroup1);
            }
        }
    }

    public List<NodeGroup> setGroupsToCut(NodeConcept concept) {

        List<NodeGroup> groupsToCut = new ArrayList<>();

        for (NodeGroup nodeGroup : concept.getNodeConceptGroup()) {
            NodeGroup nodeGroup1 = new NodeGroup();
            nodeGroup1.getConceptGroup().setIdGroup(nodeGroup.getConceptGroup().getIdGroup());
            nodeGroup1.setLexicalValue(nodeGroup.getLexicalValue());
            nodeGroup1.setSelected(true);
            groupsToCut.add(nodeGroup1);
        }

        return groupsToCut;
    }

    public List<NodeGroup> setGroupsToAdd(NodeConcept concept) {

        List<NodeGroup> groupsToAdd = new ArrayList<>();
        for (NodeGroup nodeGroup : concept.getNodeConceptGroup()) {
            NodeGroup nodeGroup1 = new NodeGroup();
            nodeGroup1.getConceptGroup().setIdGroup(nodeGroup.getConceptGroup().getIdGroup());
            nodeGroup1.setLexicalValue(nodeGroup.getLexicalValue());
            nodeGroup1.setSelected(true);
            groupsToAdd.add(nodeGroup1);
        }
        return groupsToAdd;
    }

    /**
     * permet de préparer le concept ou la branche pour le déplacement vers un autre endroit #M
     */
    public void onStartCut(NodeConcept nodeConcept) {

        if (nodeConcept == null) {
            return;
        }

        reset();
        this.dragNode = tree.getSelectedNode();

        nodeConceptDrag = nodeConcept;
        copyOn = true;

        if (nodeConceptDrag.getTerm() != null) {
            MessageUtils.showInformationMessage("Couper " + nodeConceptDrag.getTerm().getLexicalValue() 
                    + " (" + nodeConceptDrag.getConcept().getIdConcept() + ")");
        }
    }

    /**
     * permet de coller la branche copiée précédement sous le concept en cours
     * déplacements valides: - d'un concept à un concept - de la racine à un
     * concept ou TopConcept #MR
     * Ne marche que pour Couper/coller (pas de Drag and drop)
     */
    public void paste(NodeConcept droppedConcept) {
        if(droppedConcept != null) {
            this.nodeConceptDrop = droppedConcept;
            if(dropNode != null && !nodeConceptDrop.getConcept().getIdConcept().equalsIgnoreCase(((TreeNodeData) dropNode.getData()).getNodeId() )) {
                pasteWitoutTreeControl();
                return;            
            }
        } else
            dropNode = tree.getRoot();


        if(isDropToRoot) {
            dropNode = tree.getRoot();
        }

        // controle de cohérence entre les noeuds sélectionnés dans l'arbre et les concepts à déplacer
        if(!nodeConceptDrag.getConcept().getIdConcept().equalsIgnoreCase( ((TreeNodeData) dragNode.getData()).getNodeId() )) {
            pasteWitoutTreeControl();
            return;
        }

       isDragAndDrop = false;
       pasteWithTreeControl();
    }
    
    
    private void pasteWithTreeControl(){
        if(!isDropToRoot) {
            validatePaste();
            if(!isValidPaste) {
                MessageUtils.showWarnMessage("Action non permise !!!");
                return;
            }         
        }
        /// préparer le noeud à couper
        setBTsToCut();
        
        /// préparer les collections à couper        
        setGroupsToCut();

        // cas de déplacement vers une facette 
        if(((TreeNodeData) dropNode.getData()).getNodeType().equalsIgnoreCase("facet")) {
            // cas d'une branche, pas permis
            if(!nodeConceptDrag.getNodeNT().isEmpty()) {
                MessageUtils.showWarnMessage("Action non permise !!!");
                rollBackAfterErrorOrCancelDragDrop();
                return;
            }
            String idFacet = ((TreeNodeData) dropNode.getData()).getNodeId();

            if(idFacet == null) {
                MessageUtils.showWarnMessage("Action non permise !!!");
                rollBackAfterErrorOrCancelDragDrop();
                return;                
            }

            facetService.addConceptToFacet(idFacet, selectedTheso.getCurrentIdTheso(), ((TreeNodeData) dragNode.getData()).getNodeId());
            MessageUtils.showInformationMessage("Concept ajouté à la facette");
        } else {
            // on vérifie si le noeud à déplacer est un membre d'une facette, alors on supprime cette appartenance
            if(((TreeNodeData) dragNode.getData()).getNodeType().equalsIgnoreCase("facetmember")){
                try {
                    String idFacetParent = ((TreeNodeData) dragNode.getData()).getIdFacetParent();
                    facetService.deleteConceptFromFacet(idFacetParent, ((TreeNodeData) dragNode.getData()).getNodeId(),
                            selectedTheso.getCurrentIdTheso());
                } catch (Exception e) {
                    MessageUtils.showWarnMessage("Action non permise !!!");
                    rollBackAfterErrorOrCancelDragDrop();
                    return;                      
                }
                
            }
            
            // cas de changement de groupe, on propose de garder / supprimer / ajouter le groupe
            if(isDroppedToAnotherGroup()) {
                // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                ///choix de l'option pour deplacer la collection ou non 
                isGroupToCut = true;
                PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDrop");
                PrimeFaces.current().executeScript("PF('dragAndDrop').show();");
                return;
            }
            
            // on controle s'il y a plusieurs branches, 
            if (nodeConceptDrag.getNodeBT().size() < 2) {
                // sinon, on applique le changement direct 
                drop();
            } else {
                // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDrop");
                PrimeFaces.current().executeScript("PF('dragAndDrop').show();");
            }
        }        
    }
    
    private void pasteWitoutTreeControl(){
        setBTsToCut();        
        validatePaste();
        if(!isValidPaste) {
            MessageUtils.showWarnMessage("Action non permise !!!");
            rollBackAfterErrorOrCancelDragDrop();
            return;
        }        
        
        
        if(isDropToRoot) {
            // cas de déplacement d'un concept à la racine   
            if(!moveFromConceptToRoot()) return;
        } else {
            if (nodeConceptDrop == null) {
                return;
            }

            if (nodeConceptDrop.getConcept().getIdConcept().equalsIgnoreCase(nodeConceptDrag.getConcept().getIdConcept())) {
                MessageUtils.showWarnMessage("Impossible de coller au même endroit ");
                return;
            }

            // cas de déplacement d'un concept à concept        
            if ((!nodeConceptDrag.getConcept().isTopConcept())) {
                if(!moveFromConceptToConcept()) return;
            }

            // cas de déplacement de la racine à un concept
            if ((nodeConceptDrag.getConcept().isTopConcept())) {
                if(!moveFromRootToConcept()) return;
            }
        }
        
        reloadConcept();
        reloadTree();
        
        if(isDropToRoot)
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> Root");
        else
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue()+ " -> " 
                    + nodeConceptDrop.getTerm().getLexicalValue());
        reset();        
    }
    
    
    public void validatePaste() {
        
        isValidPaste = false;

        var descendingConcepts = conceptService.getIdsOfBranch(nodeConceptDrag.getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso());
        if((descendingConcepts != null) && (!descendingConcepts.isEmpty())) {
            if(descendingConcepts.contains(nodeConceptDrop.getConcept().getIdConcept())){
                MessageUtils.showErrorMessage("Opération non autorisée, elle peut provoquer des boucles infinies !!! ");   
                return;
            }
        }

        var listBT = relationService.getListIdOfBT(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso());
        if((listBT != null) && (!listBT.isEmpty())) {
            if(listBT.contains(nodeConceptDrop.getConcept().getIdConcept())){
                MessageUtils.showErrorMessage("Opération non autorisée, elle peut provoquer des boucles infinies !!! ");        
                return;
            }
        }
        if(isMoveConceptToConceptValid(selectedTheso.getCurrentIdTheso(),
                nodeConceptDrag.getConcept().getIdConcept(),
                nodeConceptDrop.getConcept().getIdConcept())) {
            MessageUtils.showErrorMessage("Relation non permise !");
            isValidPaste = false;
            return;
        }
        isValidPaste = true;
    }

    /**
     * Fonction pour récupérer l'évènement drag drop de l'arbre
     */
    public void onDragDrop(TreeDragDropEvent event) {
        reset();
        dragNode = (TreeNode) event.getDragNode();
        dropNode = (TreeNode) event.getDropNode();

        // à corriger pour traiter le déplacement des facettes par Drag and Drop
        if("facet".equalsIgnoreCase(dragNode.getType())){
            if("facet".equalsIgnoreCase(dropNode.getType())){
                MessageUtils.showErrorMessage("Déplacement non permis !");
                isValidPaste = false;
                return;
            }
            facetService.updateFacetParent(((TreeNodeData) dropNode.getData()).getNodeId(),
                    ((TreeNodeData) dragNode.getData()).getNodeId(), selectedTheso.getCurrentIdTheso());

            tree.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang());
            tree.expandTreeToPath2(((TreeNodeData) dropNode.getParent().getData()).getNodeId(), selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getSelectedLang(), ((TreeNodeData) dragNode.getData()).getNodeId());

            PrimeFaces.current().ajax().update("formRightTab:facetView");
            PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");

            MessageUtils.showWarnMessage("Facette déplacée avec succès !!!");
            return;
        }

        nodeConceptDrag = conceptService.getConceptOldVersion(((TreeNodeData) dragNode.getData()).getNodeId(),
                selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang(), -1, -1);

        isDragAndDrop = true;

        /// préparer le noeud à couper
        setBTsToCut();

        if (dropNode.getParent() == null) {
            // déplacement à la racine
            isDropToRoot = true;
        } else {
            nodeConceptDrop = conceptService.getConceptOldVersion(((TreeNodeData) dropNode.getData()).getNodeId(),
                    selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang(), -1, -1);
            /// Vérifier si le dépalcement est valide (controle des relations interdites)
            if(nodeConceptDrop != null) {
                if(isMoveConceptToConceptValid(selectedTheso.getCurrentIdTheso(),
                        nodeConceptDrag.getConcept().getIdConcept(),
                        nodeConceptDrop.getConcept().getIdConcept())) {
                    MessageUtils.showErrorMessage("Relation non permise !");
                    isValidPaste = false;
                    reloadTree();
                    return;
                }
            }
        }


        /// préparer les collections à couper
        setGroupsToCut();

        // cas de déplacement vers une facette
        if(((TreeNodeData) dropNode.getData()).getNodeType().equalsIgnoreCase("facet")) {
            // cas d'une branche, pas permis
            if(!nodeConceptDrag.getNodeNT().isEmpty()) {
                MessageUtils.showWarnMessage("Action non permise !!!");
                rollBackAfterErrorOrCancelDragDrop();
                return;
            }
            String idFacet = ((TreeNodeData) dropNode.getData()).getNodeId();

            if(idFacet == null) {
                MessageUtils.showWarnMessage("Action non permise !!!");
                rollBackAfterErrorOrCancelDragDrop();
                return;
            }

            facetService.addConceptToFacet(idFacet, selectedTheso.getCurrentIdTheso(), ((TreeNodeData) dragNode.getData()).getNodeId());
            MessageUtils.showInformationMessage("Concept ajouté à la facette");
        } else {
            // on vérifie si le noeud à déplacer est un membre d'une facette, alors on supprime cette appartenance
            if(((TreeNodeData) dragNode.getData()).getNodeType().equalsIgnoreCase("facetmember")){
                try {
                    String idFacetParent = ((TreeNodeData) dragNode.getData()).getIdFacetParent();
                    facetService.deleteConceptFromFacet(idFacetParent, ((TreeNodeData) dragNode.getData()).getNodeId(),
                            selectedTheso.getCurrentIdTheso());
                } catch (Exception e) {
                    MessageUtils.showWarnMessage("Action non permise !!!");
                    rollBackAfterErrorOrCancelDragDrop();
                    return;
                }

            }

            // cas de changement de groupe, on propose de garder / supprimer / ajouter le groupe
            if(isDroppedToAnotherGroup()) {
                // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                ///choix de l'option pour deplacer la collection ou non
                isGroupToCut = true;
                PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDrop");
                PrimeFaces.current().executeScript("PF('dragAndDrop').show();");
                return;
            }

            // on controle s'il y a plusieurs branches,
            if (nodeConceptDrag.getNodeBT().size() < 2) {
                // sinon, on applique le changement direct
                drop();
            } else {
                // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDrop");
                PrimeFaces.current().executeScript("PF('dragAndDrop').show();");
            }
        }
    }

    public void onDragDropMultiple(TreeDragDropEvent event) {
        reset();
        dragNode = (TreeNode) event.getDragNode();
        dropNode = (TreeNode) event.getDropNode();


        nodesToConfirme = new ArrayList<>();

        List<TreeNode> nodes = tree.getClickselectedNodes();
        groupNodeToAdd = new ArrayList<>();
        groupNodeToCut = new ArrayList<>();
        groupNodeBtToCut = new ArrayList<>();

        for (TreeNode node : tree.getClickselectedNodes()) {
            // à corriger pour traiter le déplacement des facettes par Drag and Drop
            if ("facet".equalsIgnoreCase(node.getType())) {
                if ("facet".equalsIgnoreCase(dropNode.getType())) {
                    MessageUtils.showErrorMessage("Déplacement non permis !");
                    isValidPaste = false;
                    return;
                }
                facetService.updateFacetParent(((TreeNodeData) dropNode.getData()).getNodeId(),
                        ((TreeNodeData) node.getData()).getNodeId(), selectedTheso.getCurrentIdTheso());

                tree.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang());
                tree.expandTreeToPath2(((TreeNodeData) dropNode.getParent().getData()).getNodeId(),
                        selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang(),
                        ((TreeNodeData) node.getData()).getNodeId());

                PrimeFaces.current().ajax().update("formRightTab:facetView");
                PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");
                MessageUtils.showWarnMessage("Facette déplacée avec succès !!!");
                return;
            }

            nodeConceptDrag = conceptService.getConceptOldVersion(((TreeNodeData) node.getData()).getNodeId(), selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getCurrentLang(), -1, -1);

            isDragAndDrop = true;
            nodesToConfirme.add(node);

            /// préparer le noeud à couper
            setBTsToCut();
            //setBTsToCut();
            List<NodeBT> nodeBTs = setBTsToCut(nodeConceptDrag);
            for (NodeBT nodeBT : nodeBTs) {
                groupNodeBtToCut.add(new BTNode(node, nodeBT));
            }

            if (dropNode.getParent() == null) {
                // déplacement à la racine
                isDropToRoot = true;
            } else {
                nodeConceptDrop = conceptService.getConceptOldVersion(((TreeNodeData) dropNode.getData()).getNodeId(), selectedTheso.getCurrentIdTheso(),
                        selectedTheso.getCurrentLang(), -1, -1);
                /// Vérifier si le dépalcement est valide (controle des relations interdites)
                if (nodeConceptDrop != null) {
                    if (isMoveConceptToConceptValid(selectedTheso.getCurrentIdTheso(),
                            nodeConceptDrag.getConcept().getIdConcept(), nodeConceptDrop.getConcept().getIdConcept())) {
                        isValidPaste = false;
                        reloadTree();
                        return;
                    }
                }
            }

            // préparer les collections à couper
            setGroupsToCut();
            //setGroupsToCut();
            List<NodeGroup> groupsToCut = setGroupsToCut(nodeConceptDrag);
            for (NodeGroup nodeGroup : groupsToCut) {
                groupNodeToCut.add(new GroupNode(node, nodeGroup));
            }
            List<NodeGroup> groupsToAdd = setGroupsToAdd(nodeConceptDrag);
            for (NodeGroup nodeGroup : groupsToAdd) {
                groupNodeToAdd.add(new GroupNode(node, nodeGroup));
            }

            // cas de déplacement vers une facette
            if (((TreeNodeData) dropNode.getData()).getNodeType().equalsIgnoreCase("facet")) {
                // cas d'une branche, pas permis
                if (!nodeConceptDrag.getNodeNT().isEmpty()) {
                    MessageUtils.showWarnMessage("Action non permise !!!");
                    rollBackAfterErrorOrCancelDragDrop();
                    return;
                }
                String idFacet = ((TreeNodeData) dropNode.getData()).getNodeId();

                if (idFacet == null) {
                    MessageUtils.showWarnMessage("Action non permise !!!");
                    rollBackAfterErrorOrCancelDragDrop();
                    return;
                }

                facetService.addConceptToFacet(idFacet, selectedTheso.getCurrentIdTheso(), ((TreeNodeData) node.getData()).getNodeId());
                MessageUtils.showInformationMessage("Concept ajouté à la facette");
            } else {
                // on vérifie si le noeud à déplacer est un membre d'une facette, alors on supprime cette appartenance
                if (((TreeNodeData) node.getData()).getNodeType().equalsIgnoreCase("facetmember")) {
                    try {
                        String idFacetParent = ((TreeNodeData) node.getData()).getIdFacetParent();
                        facetService.deleteConceptFromFacet(idFacetParent, ((TreeNodeData) node.getData()).getNodeId(),
                                selectedTheso.getCurrentIdTheso());
                    } catch (Exception e) {
                        MessageUtils.showWarnMessage("Action non permise !!!");
                        rollBackAfterErrorOrCancelDragDrop();
                        return;
                    }

                }

                // cas de changement de groupe, on propose de garder / supprimer / ajouter le groupe
                if (isDroppedToAnotherGroup()) {
                    // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                    ///choix de l'option pour deplacer la collection ou non
                    isGroupToCut = true;
                    nodesToConfirme.add(node);
                    PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDropMultiple");
                    PrimeFaces.current().executeScript("PF('dragAndDropMultiple').show();");
                    continue;
                }

                // on controle s'il y a plusieurs branches,
                if (nodeConceptDrag.getNodeBT().size() < 2) {
                    // sinon, on applique le changement direct
                    drop();
                } else {
                    nodesToConfirme.add(node);
                    // si oui, on affiche une boite de dialogue pour choisir les branches à couper
                    PrimeFaces.current().ajax().update("containerIndex:formLeftTab:idDragAndDropMultiple");
                    PrimeFaces.current().executeScript("PF('dragAndDropMultiple').show();");
                }
            }
        }
    }
    
    public void drop() {
        if(isDropToRoot) {
            // cas de déplacement d'un concept à la racine   
            if(!moveFromConceptToRoot()) return;
        } else {
            if (nodeConceptDrop == null) {
                return;
            }
            
            /// Vérifier si le dépalcement est valide (controle des relations interdites)
            if(isMoveConceptToConceptValid(selectedTheso.getCurrentIdTheso(),
                    nodeConceptDrag.getConcept().getIdConcept(), nodeConceptDrop.getConcept().getIdConcept())) {

                MessageUtils.showErrorMessage("Relation non permise !");
                isValidPaste = false;
                reloadTree();
                return;    
            }            

            // cas de déplacement d'un concept à concept        
            if ((!nodeConceptDrag.getConcept().isTopConcept())) {
                if(!moveFromConceptToConcept()) return;
            }

            // cas de déplacement de la racine à un concept
            if ((nodeConceptDrag.getConcept().isTopConcept())) {
                if(!moveFromRootToConcept()) return;
            }
        }
        if(isGroupToCut) {
           addAndCutGroup();
        }

        reloadConcept();
        reloadTree();
        
        if(isDropToRoot)
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> " + "Root");
        else
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue()
                    + " -> " + nodeConceptDrop.getTerm().getLexicalValue());

        PrimeFaces.current().executeScript("PF('dragAndDrop').hide();");
        reset();
    }

    public void dropMultiple() {

        for (TreeNode node : nodesToConfirme) {

            nodeConceptDrag = nodeConceptDrop = conceptService.getConceptOldVersion(
                    ((TreeNodeData) node.getData()).getNodeId(), selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getCurrentLang(), -1, -1);

            if (nodeConceptDrop == null) {
                continue;
            }

            if (dropNode.getParent() == null) {
                // cas de déplacement d'un concept à la racine
                if (!moveFromConceptToRootMultiple()) {
                    return;
                }
            } else {
                /// Vérifier si le dépalcement est valide (controle des relations interdites)
                // cas de déplacement d'un concept à concept
                if ((!nodeConceptDrag.getConcept().isTopConcept())) {
                    if (!moveFromConceptToConcept()) {
                        continue;
                    }
                }

                // cas de déplacement de la racine à un concept
                if ((nodeConceptDrag.getConcept().isTopConcept())) {
                    if (!moveFromRootToConcept()) {
                        continue;
                    }
                }
            }

            addAndCutGroupMultiple();

            if (dropNode.getParent() == null) {
                MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> Root");
            } else {
                MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> "
                        + nodeConceptDrop.getTerm().getLexicalValue());
            }
        }

        reloadConcept();
        reloadTree();
        PrimeFaces.current().executeScript("PF('dragAndDrop').hide();");
    }
    
    private boolean isDroppedToAnotherGroup(){
        if(nodeConceptDrag == null || nodeConceptDrop == null) return false;
        if(nodeConceptDrag.getNodeConceptGroup() == null || nodeConceptDrop.getNodeConceptGroup() == null) return false;
        
        //cas où les deux concepts n'ont pas de collections, donc pas de changement de collection
        if(nodeConceptDrag.getNodeConceptGroup().isEmpty() && nodeConceptDrop.getNodeConceptGroup().isEmpty())
            return false;
        
        //cas où l'un des deux concepts n'a pas de collection, alors il y a changement de collection
        if(nodeConceptDrag.getNodeConceptGroup().isEmpty() || nodeConceptDrop.getNodeConceptGroup().isEmpty())
            return true;
        
        //cas où il y a plusieurs collections, alors il peut y avoir changement de collection        
        if(nodeConceptDrag.getNodeConceptGroup().size() > 1 || nodeConceptDrop.getNodeConceptGroup().size() >1)
            return true;
        
        //cas où les deux collections sont identiques ou non 
        return !nodeConceptDrag.getNodeConceptGroup().get(0).getConceptGroup().getIdGroup().equalsIgnoreCase(
                nodeConceptDrop.getNodeConceptGroup().get(0).getConceptGroup().getIdGroup());
    }

    /**
     * permet de retourner les noms des collections/groupes
     */
    public String getLabelOfGroupes() {
        if (nodeConceptDrag == null) {
            return null;
        }
        String labels = "";
        boolean first = true;
        for (NodeGroup nodeGroup : nodeConceptDrag.getNodeConceptGroup()) {
            if (!first) {
                labels = labels + ", " + nodeGroup.getLexicalValue();
            } else {
                labels = nodeGroup.getLexicalValue();
                first = false;
            }
        }
        return labels;
    }

    private boolean moveFromConceptToConceptMultiple(){
        // cas de déplacement d'un concept à concept
        ArrayList<String> oldBtToDelete = new ArrayList<>();
        for (BTNode nodeBT : groupNodeBtToCut) {
            if (((TreeNodeData) nodeBT.getNode().getData()).getNodeId().equals(nodeConceptDrag.getConcept().getIdConcept())
                    && nodeBT.nodeBT.isSelected()) {
                // on prépare les BT sélectionné pour la suppression
                oldBtToDelete.add(nodeBT.nodeBT.getIdConcept());
            }
        }
        if (oldBtToDelete.isEmpty()) {
            MessageUtils.showWarnMessage(nodeConceptDrag.getTerm().getLexicalValue() + " - Aucun parent n'est sélectionné pour déplacement");
            return false;
        }
        if (!conceptService.moveBranchFromConceptToConcept(nodeConceptDrag.getConcept().getIdConcept(), oldBtToDelete,
                nodeConceptDrop.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser())) {

            MessageUtils.showErrorMessage(nodeConceptDrag.getTerm().getLexicalValue() + " - Erreur pendant la suppression des branches !!");
        }
        return true;
    }

    private boolean moveFromConceptToRootMultiple(){
        List<String> oldBtToDelete = new ArrayList<>();

        for (BTNode nodeBT : groupNodeBtToCut) {
            if (((TreeNodeData) nodeBT.getNode().getData()).getNodeId()
                    .equals(nodeConceptDrag.getConcept().getIdConcept())) {
                oldBtToDelete.add(nodeConceptDrag.getConcept().getIdConcept());
            }
        }
        // cas incohérent mais à corriger, c'est un concept qui est topTorm mais qui n'a pas l'info
        if (oldBtToDelete.isEmpty()) {
            if (!conceptService.setTopConcept(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), true)) {
                MessageUtils.showWarnMessage("Erreur pendant le déplacement dans la base de données ");
                return false;
            }
            return true;
        }

        for (String oldIdBT : oldBtToDelete) {
            if (!conceptService.moveBranchFromConceptToRoot(nodeConceptDrag.getConcept().getIdConcept(), oldIdBT,
                    selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser())) {
                MessageUtils.showErrorMessage(" Erreur pendant le déplacement dans la base de données ");
                return false;
            }
        }
        return true;
    }

    private void addAndCutGroupMultiple() {
        var allId = conceptService.getIdsOfBranch(nodeConceptDrop.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso());
        if( (allId == null) || (allId.isEmpty())) return;

        for (GroupNode nodeGroup : groupNodeToCut) {
            if (((TreeNodeData) nodeGroup.getNode().getData()).getNodeId()
                    .equals(nodeConceptDrag.getConcept().getIdConcept())
                    && nodeGroup.getNodeGroup().isSelected()) {
                for (String idConcept : allId) {
                    groupService.deleteRelationConceptGroupConcept(nodeGroup.getNodeGroup().getConceptGroup().getIdGroup(),
                            idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
            if (((TreeNodeData) nodeGroup.getNode().getData()).getNodeId().equals(nodeConceptDrag.getConcept().getIdConcept())
                    && !nodeGroup.getNodeGroup().isSelected()) {
                for (String idConcept : allId) {
                    groupService.addConceptGroupConcept(nodeGroup.getNodeGroup().getConceptGroup().getIdGroup(),
                            idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
        }
        for (GroupNode nodeGroup : groupNodeToAdd) {
            if (((TreeNodeData) nodeGroup.getNode().getData()).getNodeId().equals(nodeConceptDrag.getConcept().getIdConcept())
                    && !nodeGroup.getNodeGroup().isSelected()) {
                for (String idConcept : allId) {
                    groupService.deleteRelationConceptGroupConcept(nodeGroup.getNodeGroup().getConceptGroup().getIdGroup(),
                            idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
            if (((TreeNodeData) nodeGroup.getNode().getData()).getNodeId().equals(nodeConceptDrag.getConcept().getIdConcept())
                    && nodeGroup.getNodeGroup().isSelected()) {
                for (String idConcept : allId) {
                    groupService.addConceptGroupConcept(nodeGroup.getNodeGroup().getConceptGroup().getIdGroup(),
                            idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
        }
    }

    /**
     * deplacement entre concepts
     */
    private boolean moveFromConceptToConcept(){
        // cas de déplacement d'un concept à concept
        List<String> oldBtToDelete = new ArrayList<>();
        for (NodeBT nodeBT : nodeBTsToCut) {
            if (nodeBT.isSelected()) {
                // on prépare les BT sélectionné pour la suppression
                oldBtToDelete.add(nodeBT.getIdConcept());
            }
        }
        if (oldBtToDelete.isEmpty()) {
            MessageUtils.showWarnMessage("aucun parent n'est sélectionné pour déplacement ");
            return false;
        }
        if (!conceptService.moveBranchFromConceptToConcept(nodeConceptDrag.getConcept().getIdConcept(),
                oldBtToDelete, nodeConceptDrop.getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser())) {
            MessageUtils.showErrorMessage(" Erreur pendant la suppression des branches !!");
            return false;
        }
        return true;
    }
    
    private boolean moveFromRootToConcept() {
        if (!conceptService.moveBranchFromRootToConcept(nodeConceptDrag.getConcept().getIdConcept(),
                nodeConceptDrop.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser())) {
            MessageUtils.showErrorMessage(" Erreur pendant le déplacement dans la base de données ");
            return false;
        }
        return true;
    }
    
    private boolean moveFromConceptToRoot(){

        List<String> oldBtToDelete = new ArrayList<>();
        
        for (NodeBT nodeBT : nodeBTsToCut) {
            oldBtToDelete.add(nodeBT.getIdConcept());
        }
        // cas incohérent mais à corriger, c'est un concept qui est topTorm mais qui n'a pas l'info
        if (oldBtToDelete.isEmpty()) {
            if (!conceptService.setTopConcept(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), true)) {
                MessageUtils.showWarnMessage("Erreur pendant le déplacement dans la base de données ");
                return false;
            }
            return true;
        } 
        
        for (String oldIdBT : oldBtToDelete) {
            if (!conceptService.moveBranchFromConceptToRoot(nodeConceptDrag.getConcept().getIdConcept(), oldIdBT,
                    selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser())) {
                MessageUtils.showErrorMessage(" Erreur pendant le déplacement dans la base de données ");
                return false;
            }
        }
        return true;
    }

    private void reloadConcept(){

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), nodeConceptDrag.getConcept().getIdConcept(), currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(nodeConceptDrag.getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());
        
        // si le concept n'est pas déployé à doite, alors on ne fait rien
        if(conceptBean.getNodeConcept() != null){
            conceptBean.getConcept(selectedTheso.getCurrentIdTheso(), nodeConceptDrag.getConcept().getIdConcept(),
                    conceptBean.getSelectedLang(), currentUser);
            PrimeFaces.current().ajax().update("containerIndex:rightTab:conceptView");
        }
    }

    private void reloadTree(){

        String lang = selectedTheso.getCurrentLang();
        if (!StringUtils.isEmpty(conceptBean.getSelectedLang())) {
            lang = conceptBean.getSelectedLang();
        }

        tree.initAndExpandTreeToPath(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), lang);

        PrimeFaces.current().ajax().update("messageIndex");
        PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");
        PrimeFaces.current().executeScript("srollToSelected();");
    }

    private void addAndCutGroup() {
        List<String> allId = conceptService.getIdsOfBranch(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso());
        if( (allId == null) || (allId.isEmpty())) return;         
        
        for (NodeGroup nodeGroup : nodeGroupsToCut) {
            if(nodeGroup.isSelected()) {
                for (String idConcept : allId) {
                    groupService.deleteRelationConceptGroupConcept(nodeGroup.getConceptGroup().getIdGroup(),
                        idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
            if(!nodeGroup.isSelected()) {
                for (String idConcept : allId) {
                    groupService.addConceptGroupConcept(nodeGroup.getConceptGroup().getIdGroup(), idConcept, selectedTheso.getCurrentIdTheso());
                }
            }            
        }
        for (NodeGroup nodeGroup : nodeGroupsToAdd) {
            if(!nodeGroup.isSelected()) {
                for (String idConcept : allId) {
                    groupService.deleteRelationConceptGroupConcept(nodeGroup.getConceptGroup().getIdGroup(), idConcept, selectedTheso.getCurrentIdTheso());
                }
            }
            if(nodeGroup.isSelected()) {
                for (String idConcept : allId) {
                    groupService.addConceptGroupConcept(nodeGroup.getConceptGroup().getIdGroup(),
                        idConcept, selectedTheso.getCurrentIdTheso());
                }
            }            
        }
    }
  
    public void rollBackAfterErrorOrCancelDragDrop() {

        reloadTree();
        reset();
        MessageUtils.showInformationMessage( "Déplacement annulé ");
        PrimeFaces.current().executeScript("PF('dragAndDrop').hide();");
    }

    public class GroupNode {

        private TreeNode node;
        private NodeGroup nodeGroup;

        public GroupNode(TreeNode node, NodeGroup nodeGroup) {
            this.node = node;
            this.nodeGroup = nodeGroup;
        }

        public TreeNode getNode() {
            return node;
        }

        public void setNode(TreeNode node) {
            this.node = node;
        }

        public NodeGroup getNodeGroup() {
            return nodeGroup;
        }

        public void setNodeGroup(NodeGroup nodeGroup) {
            this.nodeGroup = nodeGroup;
        }
    }

    public class BTNode {

        private TreeNode node;
        private NodeBT nodeBT;

        public BTNode(TreeNode node, NodeBT nodeBT) {
            this.node = node;
            this.nodeBT = nodeBT;
        }

        public TreeNode getNode() {
            return node;
        }

        public void setNode(TreeNode node) {
            this.node = node;
        }

        public NodeBT getNodeBT() {
            return nodeBT;
        }

        public void setNodeBT(NodeBT nodeBT) {
            this.nodeBT = nodeBT;
        }
    }

    public String getNodeIdFromData(Object node) {
        TreeNodeData data = treeNodeDataOf(node);
        return data != null ? StringUtils.defaultString(data.getNodeId()) : "";
    }

    public String getNodeNameFromData(Object node) {
        TreeNodeData data = treeNodeDataOf(node);
        return data != null ? StringUtils.defaultString(data.getName()) : "";
    }

    public void validateCheck() {
        // L'état des cases est lié au modèle ; la validité du collage est contrôlée au drop.
    }

    private TreeNodeData treeNodeDataOf(Object node) {
        if (node instanceof TreeNode treeNode && treeNode.getData() instanceof TreeNodeData data) {
            return data;
        }
        if (node instanceof TreeNodeData data) {
            return data;
        }
        return null;
    }

    public boolean isMoveConceptToConceptValid(String idTheso, String idConcept, String idConceptToAdd) {

        return idConcept.equalsIgnoreCase(idConceptToAdd)
                || relationService.isConceptHaveRelationRT(idConcept, idConceptToAdd, idTheso)
                || relationService.isConceptHaveRelationNTorBT(idConcept, idConceptToAdd, idTheso);
    }
}
