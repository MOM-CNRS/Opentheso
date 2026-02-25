package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.CopyAndPasteBetweenThesoService;
import fr.cnrs.opentheso.models.concept.NodeConcept;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.List;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "copyAndPasteBetweenThesaurus")
public class CopyAndPasteBetweenThesaurus implements Serializable {

    private final Tree tree;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final RoleOnThesaurusBean roleOnThesaurusBean;

    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final CopyAndPasteBetweenThesoService copyAndPasteBetweenThesoService;

    private boolean copyOn, validPaste, dropToRoot;
    private NodeConcept nodeConceptDrag, nodeConceptDrop;
    private List<String> conceptsToCopy;
    private String idThesaurusOrigin;
    private String identifierType = "sans";

    
    public void reset() {
        copyOn = false;
        validPaste = false;
        nodeConceptDrag = null;
        nodeConceptDrop = null;
        dropToRoot = false;
        idThesaurusOrigin = null;
        conceptsToCopy = null;
    }
    public void setIsDropToRoot(boolean isDropToRoot) {
        this.dropToRoot = isDropToRoot;
    }

    /**
     * permet de préparer le concept ou la branche pour le déplacement vers un autre endroit #MR
     *
     */
    public void onStartCopy() {
        nodeConceptDrag = conceptService.getConceptOldVersion(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getTerm().getLang(), -1, -1);
        copyOn = true;
        idThesaurusOrigin = selectedTheso.getCurrentIdTheso();
        if (nodeConceptDrag == null) {
            return;
        }

        conceptsToCopy = conceptService.getIdsOfBranch(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso());

        MessageUtils.showInformationMessage("Copier " + nodeConceptDrag.getTerm().getLexicalValue() + " ("
                + nodeConceptDrag.getConcept().getIdConcept() + ") Total = " + conceptsToCopy.size());
    }

    public void validatePaste() {
        if(selectedTheso.getCurrentIdTheso().equalsIgnoreCase(idThesaurusOrigin)) {
            return;
        }

        validPaste = false;
        // on vérifie si les ids des concepts à copier n'existent pas déjà dans le thésaurus cible.
        for (String idConcept : conceptsToCopy) {
            if(conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                MessageUtils.showErrorMessage("L'identifiant " + idConcept + " existe déjà dans le thésaurus cible, c'est interdit !!! ");
                return;                
            }
        }
        validPaste = true;
    }
    
    /**
     * permet de coller la branche copiée précédement sous le concept en cours
     * déplacements valides: - d'un concept à un concept - de la racine à un
     * concept ou TopConcept #MR
     * Ne marche que pour Couper/coller (pas de Drag and drop)
     *
     */
    public void paste() {
     
        if(dropToRoot && !copyToRoot()) {
            // cas de déplacement d'un concept à la racine   
            return;
        } else {
            if (conceptBean.getNodeConcept() == null) {
                return;
            }
            // cas de déplacement d'un concept à concept        
            if(!copyToConcept()) return;
        }

        if(dropToRoot)
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> Root");
        else
            MessageUtils.showInformationMessage(nodeConceptDrag.getTerm().getLexicalValue() + " -> "
                    + conceptBean.getNodeConcept().getTerm().getLexicalValue());
        reset();
    }    

    private boolean copyToConcept(){
        // cas de déplacement d'un concept/branche d'un autre thésaurus vers un concept
        if(nodeConceptDrag.getConcept() == null) {
            MessageUtils.showErrorMessage("Erreur de copie");
            return false;
        }

        if(!copyAndPasteBetweenThesoService.pasteBranchLikeNT(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(), idThesaurusOrigin, nodeConceptDrag.getConcept().getIdConcept(),
                identifierType, currentUser.getNodeUser().getIdUser(), roleOnThesaurusBean.getNodePreference())) {
            MessageUtils.showErrorMessage("Erreur de copie");
        }
        return true;
    }
    
    private boolean copyToRoot(){
        // cas de déplacement d'un concept/branche d'un autre thésaurus à la racine
        
        if(!copyAndPasteBetweenThesoService.pasteBranchToRoot(selectedTheso.getCurrentIdTheso(), idThesaurusOrigin,
                nodeConceptDrag.getConcept().getIdConcept(), identifierType, currentUser.getNodeUser().getIdUser(),
                roleOnThesaurusBean.getNodePreference())) {
            MessageUtils.showErrorMessage("Erreur pendant l'opération de copie");
        }
        return true; 
    }

    private void reloadTree(){

        String lang = selectedTheso.getCurrentLang();
        if (!StringUtils.isEmpty(conceptBean.getSelectedLang())) {
            lang = conceptBean.getSelectedLang();
        }

        tree.initAndExpandTreeToPath(nodeConceptDrag.getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), lang);

        PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");
        PrimeFaces.current().executeScript("srollToSelected();");
    }
  
    public void rollBackAfterErrorOrCancelDragDrop() {
        MessageUtils.showInformationMessage("Déplacement annulé ");
        PrimeFaces.current().executeScript("PF('cutAndPaste').hide();");
    }

}
