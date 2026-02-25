package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.terms.NodeBT;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "broaderBean")
public class BroaderBean implements Serializable {

    private final Tree tree;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final ConceptService conceptService;
    private final RelationService relationService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final SearchService searchService;

    private NodeSearchMini searchSelected;
    private List<NodeBT> nodeBTs;


    /**
     * Permet de retourner la liste des concepts possibles pour ajouter une
     * relation NT (en ignorant les relations interdites) on ignore les concepts
     * de type TT on ignore les concepts de type RT
     */
    public List<NodeSearchMini> getAutoComplet(String value) {
        List<NodeSearchMini> liste = new ArrayList<>();
        if (selectedTheso.getCurrentIdTheso() != null && conceptBean.getSelectedLang() != null) {
            liste = searchService.searchAutoCompletionForRelation(value, conceptBean.getSelectedLang(),
                    selectedTheso.getCurrentIdTheso(), true);
        }
        return liste;
    }

    public void addNewBroaderLink() {

        if (searchSelected == null || searchSelected.getIdConcept() == null || searchSelected.getIdConcept().isEmpty()) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        /// vérifier la cohérence de la relation
        if (isAddRelationBTValid(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                searchSelected.getIdConcept())) {

            MessageUtils.showErrorMessage("Relation non permise !");
            return;
        }

        relationService.addRelationBT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), searchSelected.getIdConcept(), currentUser.getNodeUser().getIdUser());

        // on vérifie si le concept qui a été ajouté était TopTerme, alors on le rend plus TopTerm pour éviter les boucles à l'infini
        if (conceptService.isTopConcept(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso())) {
            if (!conceptService.setTopConcept(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), false)) {
                MessageUtils.showErrorMessage("Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de coorection de cohérence !");
                return;
            }
        }

        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                conceptBean.getSelectedLang(), currentUser);

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(), currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptBean.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());
        
        MessageUtils.showInformationMessage("Relation ajoutée avec succès");

        reset();

        tree.initAndExpandTreeToPath(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), conceptBean.getSelectedLang());

        PrimeFaces.current().executeScript("srollToSelected();");
        PrimeFaces.current().executeScript("PF('addBroaderLink').hide();");
        PrimeFaces.current().ajax().update("containerIndex");
    }

    public void deleteBroaderLink(NodeBT nodeBT) {

        if (nodeBT == null || nodeBT.getIdConcept() == null || nodeBT.getIdConcept().isEmpty()) {
            MessageUtils.showErrorMessage("Aucune relation n'est sélectionnée !");
            return;
        }

        relationService.deleteRelationBT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), nodeBT.getIdConcept(), currentUser.getNodeUser().getIdUser());

        // on vérifie si le concept en cours n'a plus de BT, on le rend TopTerme
        if (!relationService.isConceptHaveRelationBT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso())) {
            if (!conceptService.setTopConcept(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), true)) {
                MessageUtils.showErrorMessage("Erreur en passant le concept et TopConcept, veuillez utiliser les outils de correction de cohérence !");
                return;
            }
        }

        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                conceptBean.getSelectedLang(), currentUser);

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(), currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptBean.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        MessageUtils.showInformationMessage(" Relation supprimée avec succès");

        reset();

        tree.initAndExpandTreeToPath(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(),
                conceptBean.getSelectedLang());

        PrimeFaces.current().ajax().update("formRightTab:viewTabConcept:idConceptBroader");
        PrimeFaces.current().ajax().update("formRightTab:viewTabConcept:deleteBroaderLinkForm");
        PrimeFaces.current().executeScript("srollToSelected();");
        PrimeFaces.current().executeScript("PF('deleteBroaderLink').hide();");
    }

    public void reset() {
        nodeBTs = conceptBean.getNodeConcept().getNodeBT();
        searchSelected = null;
    }

    private boolean isAddRelationBTValid(  String idTheso, String idConcept, String idConceptToAdd) {

        return idConcept.equalsIgnoreCase(idConceptToAdd)
                || relationService.isConceptHaveRelationRT(idConcept, idConceptToAdd, idTheso)
                || relationService.isConceptHaveRelationNTorBT(idConcept, idConceptToAdd, idTheso);
              //  || relationService.isConceptHaveBrother(idConcept, idConceptToAdd, idTheso);
    }

}
