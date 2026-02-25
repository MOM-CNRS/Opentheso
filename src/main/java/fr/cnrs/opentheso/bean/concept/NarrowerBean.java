package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.ConceptRelation;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.terms.NodeNT;
import fr.cnrs.opentheso.models.relations.NodeTypeRelation;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.util.List;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import jakarta.enterprise.context.SessionScoped;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "narrowerBean")
public class NarrowerBean implements Serializable {

    private final Tree tree;
    private final ConceptView conceptBean;
    private final CurrentUser currentUser;
    private final SearchService searchService;
    private final SelectedTheso selectedTheso;
    private final ConceptService conceptService;
    private final RelationService relationService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    private NodeSearchMini searchSelected;
    private List<NodeNT> nodeNTs;
    private List<NodeTypeRelation> typesRelationsNT;
    private String selectedRelationRole;
    private boolean applyToBranch;


    public void reset() {
        nodeNTs = conceptBean.getNodeConcept().getNodeNT();
        searchSelected = null;
        applyToBranch = false;
    }

    public void initForChangeRelations() {
        typesRelationsNT = relationService.getTypesRelationsNT();
        nodeNTs = conceptBean.getNodeConcept().getNodeNT();
        applyToBranch = false;
    }

    /**
     * permet de retourner la liste des concepts possibles
     * pour ajouter une relation NT
     * (en ignorant les relations interdites)
     * on ignore les concepts de type TT
     * on ignore les concepts de type RT
     */
    public List<NodeSearchMini> getAutoComplet(String value) {
        
        if (selectedTheso.getCurrentIdTheso() != null && conceptBean.getSelectedLang() != null) {
           return searchService.searchAutoCompletionForRelation(value, conceptBean.getSelectedLang(),
                    selectedTheso.getCurrentIdTheso(), true);
        }
        return List.of();
    }
    
    public void addNewNarrowerLink() {

        if (searchSelected == null || searchSelected.getIdConcept() == null || searchSelected.getIdConcept().isEmpty()) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        /// vérifier la cohérence de la relation
        if (isAddRelationNTValid(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(), searchSelected.getIdConcept())) {
            MessageUtils.showErrorMessage("Relation non permise !");
            return;
        }

        relationService.addRelationNT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), searchSelected.getIdConcept(), currentUser.getNodeUser().getIdUser());

        // on vérifie si le concept qui a été ajouté était TopTerme, alors on le rend plus TopTerm pour éviter les boucles à l'infini
        if (conceptService.isTopConcept(searchSelected.getIdConcept(), selectedTheso.getCurrentIdTheso())) {
            if (!conceptService.setTopConcept(searchSelected.getIdConcept(), selectedTheso.getCurrentIdTheso(), false)) {
                MessageUtils.showErrorMessage("Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de coorection de cohérence !");
                return;
            }
        }
        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                conceptBean.getSelectedLang(), currentUser);

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                currentUser.getNodeUser().getIdUser());

        MessageUtils.showInformationMessage("Relation ajoutée avec succès");

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptBean.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        tree.initAndExpandTreeToPath(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(),
                conceptBean.getSelectedLang());

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");

        PrimeFaces.current().executeScript("srollToSelected();");

        PrimeFaces.current().executeScript("PF('addNarrowerLink').hide();");
        reset();
    }

    /**
     * permet de supprimer une relation NT au concept
     */
    public void deleteNarrowerLink(ConceptRelation nodeNT) {

        if (nodeNT == null || nodeNT.getIdConcept() == null || nodeNT.getIdConcept().isEmpty()) {
            MessageUtils.showErrorMessage(" pas de sélection !");
            return;
        }

        relationService.deleteRelationNT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso(), nodeNT.getIdConcept(), currentUser.getNodeUser().getIdUser());

        // on vérifie si le concept qui a été enlevé n'a plus de BT, on le rend TopTerme
        if (!relationService.isConceptHaveRelationBT(nodeNT.getIdConcept(), selectedTheso.getCurrentIdTheso())) {
            if (!conceptService.setTopConcept(nodeNT.getIdConcept(), selectedTheso.getCurrentIdTheso(), true)) {
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

        MessageUtils.showInformationMessage("Relation supprimée avec succès");
        reset();

        tree.initAndExpandTreeToPath(conceptBean.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), conceptBean.getSelectedLang());

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("conceptForm:listNarrowerLink");
        PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");

        PrimeFaces.current().executeScript("srollToSelected();");
        PrimeFaces.current().executeScript("PF('deleteNarrowerLink').hide();");
    }

    public void applyRelationToBranch(String idConceptParent) {

        var inverseRelation = getRelationCode(selectedRelationRole);
        applyRelationToBranch__(idConceptParent, selectedRelationRole, inverseRelation, currentUser.getNodeUser().getIdUser());
        MessageUtils.showInformationMessage(" Relation modifiée avec succès");
        initForChangeRelations();
    }

    public void updateRelation(ConceptRelation nodeNT) {

        var inverseRelation = getRelationCode(nodeNT.getRole());
        if (!relationService.updateRelationNT(conceptBean.getNodeConcept().getConcept().getIdConcept(),
                nodeNT.getIdConcept(), selectedTheso.getCurrentIdTheso(), nodeNT.getRole(), inverseRelation,
                currentUser.getNodeUser().getIdUser())) {
            MessageUtils.showErrorMessage("Erreur modifiant la relation pour le concept !");
            return;
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

        MessageUtils.showInformationMessage("Relation modifiée avec succès");
        initForChangeRelations();

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("conceptForm:changeRelationForm");
    }

    private boolean isAddRelationNTValid(String idThesaurus, String idConcept, String idConceptToAdd) {

        return idConcept.equalsIgnoreCase(idConceptToAdd)
                || relationService.isConceptHaveRelationRT(idConcept, idConceptToAdd, idThesaurus)
                || relationService.isConceptHaveRelationNTorBT(idConcept, idConceptToAdd, idThesaurus);
             //   || relationService.isConceptHaveBrother(idConcept, idConceptToAdd, idThesaurus);
    }

    private void applyRelationToBranch__(String idConceptParent, String relation, String inverseRelation, int idUser) {

        var conceptsFils = conceptService.getListChildrenOfConcept(idConceptParent, selectedTheso.getCurrentIdTheso());
        for (String idConcept : conceptsFils) {
            relationService.updateRelationNT(idConceptParent, idConcept, selectedTheso.getCurrentIdTheso(),
                    relation, inverseRelation, idUser);
            applyRelationToBranch__(idConcept, relation, inverseRelation, idUser);
        }
    }

    private String getRelationCode(String role) {
        return switch (role) {
            case "NT" -> "BT";
            case "NTG" -> "BTG";
            case "NTP" -> "BTP";
            case "NTI" -> "BTI";
            default -> "BT";
        };
    }
}

