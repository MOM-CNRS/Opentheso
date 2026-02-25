package fr.cnrs.opentheso.bean.alignment;

import fr.cnrs.opentheso.bean.candidat.CandidatBean;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentType;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "alignmentManualBean")
public class AlignmentManualBean implements Serializable {

    private final AlignmentBean alignmentBean;
    private final ConceptView conceptView;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final AlignmentService alignmentService;
    private final ConceptService conceptService;

    private String manualAlignmentSource, manualAlignmentUri;
    private int manualAlignmentType;
    private List<NodeAlignment> nodeAlignments;
    private List<NodeAlignmentType> nodeAlignmentTypes;


    public void reset() {
        log.debug("Initialisation des données nécessaire pour l'interface alignement manuel");
        nodeAlignmentTypes = alignmentService.searchAllAlignementTypes();
        manualAlignmentSource = "";
        manualAlignmentUri = "";
        manualAlignmentType = -1;
        if(conceptView.getNodeFullConcept() != null) {
            nodeAlignments = alignmentService.getAllAlignmentOfConcept(conceptView.getNodeFullConcept().getIdentifier(), selectedTheso.getCurrentIdTheso());
        }
    }

    public void showManuelAlignmentDialog() {
        reset();
        PrimeFaces.current().executeScript("PF('addManualAlignment').show();");
    }

    public void infos() {
        MessageUtils.showInformationMessage("Rédiger une aide ici pour Add Concept !");
    }    
    
    public void deleteAlignment(NodeAlignment nodeAlignment) {
        
        if(nodeAlignment == null) return;

        if(!alignmentService.deleteAlignment(nodeAlignment.getId_alignement(), selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression !");
            return;            
        }

        updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                currentUser.getNodeUser().getIdUser());

        conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Alignement supprimé avec succès");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");

        reset();
    }

    public void updateAlignement() {

        if(alignmentBean.getAlignementElementSelected() == null) return;

        if(!alignmentService.updateAlignement(alignmentBean.getAlignementElementSelected(),
                conceptView.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso())) {

            MessageUtils.showErrorMessage("Erreur pendant la modification !");
            return;            
        }

        updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                currentUser.getNodeUser().getIdUser());

        conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Alignement modifié avec succès");
        
        alignmentBean.getIdsAndValues(selectedTheso.getCurrentLang(), selectedTheso.getCurrentIdTheso());

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    public void updateAlignement(NodeAlignment nodeAlignment) {

        if(nodeAlignment == null) return;

        var alignementElement = AlignementElement.builder()
                .idAlignment(nodeAlignment.getId_alignement())
                .alignement_id_type(nodeAlignment.getAlignement_id_type())
                .conceptTarget(nodeAlignment.getConcept_target())
                .thesaurus_target(nodeAlignment.getThesaurus_target())
                .targetUri(nodeAlignment.getUri_target())
                .build();
        if(!alignmentService.updateAlignement(alignementElement, conceptView.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso())) {

            MessageUtils.showErrorMessage("Erreur pendant la modification !");
            return;
        }

        updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                currentUser.getNodeUser().getIdUser());

        conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Alignement modifié avec succès");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    public void updateAlignementFromConceptInterface(){

        if(!alignmentService.updateAlignement(alignmentBean.getAlignementElementSelected(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso())) {

            MessageUtils.showErrorMessage("Erreur pendant la modification !");
            return;            
        }

        updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                currentUser.getNodeUser().getIdUser());

        alignmentBean.getIdsAndValues2(conceptView.getSelectedLang(), selectedTheso.getCurrentIdTheso());
        
        conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Alignement modifié avec succès");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }
    
    public void addManualAlignement(CandidatBean candidatBean, String idConcept, boolean isFromConceptView){

        if(StringUtils.isEmpty(manualAlignmentUri)){
            MessageUtils.showInformationMessage("Veuillez saisir une valeur  !");
            return;
        } 

        if(!fr.cnrs.opentheso.utils.StringUtils.urlValidator(manualAlignmentUri)){
            MessageUtils.showInformationMessage("L'URL n'est pas valide !");
            return;            
        }

        idConcept = StringUtils.isEmpty(idConcept) ? candidatBean.getCandidatSelected().getIdConcepte() : idConcept;
        if(!alignmentService.addNewAlignment(currentUser.getNodeUser().getIdUser(), "", manualAlignmentSource,
                manualAlignmentUri, manualAlignmentType, idConcept, selectedTheso.getCurrentIdTheso(), 0)) {

            MessageUtils.showErrorMessage("Erreur de modification !");
            return;            
        }

        updateDateOfConcept(selectedTheso.getCurrentIdTheso(), idConcept, currentUser.getNodeUser().getIdUser());

        if (isFromConceptView) {
            conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                    conceptView.getSelectedLang(), currentUser);
        } else {
            candidatBean.getCandidatSelected().setAlignments(alignmentService.getAllAlignmentOfConcept(
                    idConcept, selectedTheso.getCurrentIdTheso()));
        }

        MessageUtils.showInformationMessage("Alignement ajouté avec succès");
        alignmentBean.getIdsAndValues(selectedTheso.getCurrentLang(), selectedTheso.getCurrentIdTheso());
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }    

    private void updateDateOfConcept(String idThesaurus, String idConcept, int idUser) {

        conceptService.updateDateOfConcept(idThesaurus, idConcept, idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .build());
    }
}
