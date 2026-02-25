package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.bean.setting.PreferenceBean;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.leftbody.TreeNodeData;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.services.ArkService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.HandleConceptService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.exports.csv.CsvWriteHelper;
import fr.cnrs.opentheso.services.security.CryptoService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.services.HandleService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "editConcept")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EditConcept implements Serializable {

    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final LanguageBean languageBean;
    private final ConceptView conceptView;
    private final SelectedTheso selectedTheso;
    private final Tree tree;
    private final CurrentUser currentUser;
    private final HandleService handleService;
    private final PreferredTermRepository preferredTermRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final CsvWriteHelper csvWriteHelper;
    private final RelationService relationService;
    private final HandleService handleHelper;
    private final TermService termService;
    private final TermRepository termRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final GroupService groupService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final HandleConceptService handleConceptService;
    private final ArkService arkService;
    private final ConceptTypeService conceptTypeService;
    private final SearchService searchService;
    private final SelectedTheso selectedThesaurus;
    private final CryptoService cryptoService;

    private final PreferenceBean preferenceBean;

    private String prefLabel, source, notation, selectedConceptType;
    private boolean applyToBranch, reciprocalRelationship, isCreated, duplicate, forDelete, inProgress, replacedByRTRelation;
    private NodeSearchMini searchSelected;
    private List<NodeIdValue> nodeIdValues, nodeReplaceBy;
    private NodeConceptType nodeConceptTypeToDelete, nodeConceptTypeToAdd;
    private List<NodeConceptType> nodeConceptTypes;


    public void clear() {
        if (nodeReplaceBy != null) {
            nodeReplaceBy.clear();
            nodeReplaceBy = null;
        }
        inProgress = false;
        prefLabel = null;
        notation = null;
        source = null;
        selectedConceptType = null;
        nodeIdValues = null;
        nodeConceptTypeToDelete = null;
        nodeConceptTypeToAdd = new NodeConceptType();
    }

    public void reset(String label) {
        isCreated = false;
        duplicate = false;
        prefLabel = label;
        notation = "";
        forDelete = false;
        replacedByRTRelation = false;
        inProgress = false;
        nodeIdValues = null;
        nodeConceptTypeToDelete = null;
        nodeConceptTypeToAdd = new NodeConceptType();

        nodeReplaceBy = conceptView.getNodeConcept().getReplacedBy();
    }

    public void initForConceptType(){

        nodeConceptTypes = conceptTypeService.getAllTypesOfConcept(selectedTheso.getCurrentIdTheso());
        selectedConceptType = conceptView.getNodeConcept().getConcept().getConceptType();
        applyToBranch = false;
        reciprocalRelationship = false;
        nodeConceptTypeToDelete = null;
        nodeConceptTypeToAdd = new NodeConceptType();
    }

    public void removeAllConceptFromCollection(String idGroup){
        groupService.removeAllConceptsFromThisGroup(idGroup, selectedTheso.getCurrentIdTheso());

        selectedTheso.reloadGroups();
        MessageUtils.showInformationMessage("Tous les concepts ont été retirés de la collection.");
    }

    /**
     * permet de changer les informations dans la table du type de concept
     */
    public void applyChangeForConceptType(NodeConceptType nodeConceptType){
        if(nodeConceptType == null || StringUtils.isEmpty(nodeConceptType.getCode())){
            MessageUtils.showErrorMessage("pas de sélection !");
            return;
        }

        if(!conceptTypeService.updateConceptType(selectedTheso.getCurrentIdTheso(), nodeConceptType)){
            MessageUtils.showErrorMessage("Erreur pendant le changement !");
            return;
        }
        initForConceptType();
        MessageUtils.showInformationMessage("Changement réussi !");
    }
    
    /**
     * permet de supprimer un type de concept
     */
    public void deleteCustomRelationship(){
        if(nodeConceptTypeToDelete == null || StringUtils.isEmpty(nodeConceptTypeToDelete.getCode())){
            MessageUtils.showErrorMessage("pas de sélection !");
            return;            
        }

        conceptTypeService.deleteConceptType(selectedTheso.getCurrentIdTheso(), nodeConceptTypeToDelete);

        initForConceptType();
        MessageUtils.showInformationMessage("Suppression réussie !");
    }
    
    /**
     * permet d'ajouter un nouveau type de concept
     */
    public void addNewConceptType(){
        if(nodeConceptTypeToAdd == null || StringUtils.isEmpty(nodeConceptTypeToAdd.getCode())){
            MessageUtils.showErrorMessage("pas de sélection !");
            return;            
        }    
        if(StringUtils.isEmpty( selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("pas de sélection !");
            return;              
        }
        
        nodeConceptTypeToAdd.setCode(fr.cnrs.opentheso.utils.StringUtils.unaccentLowerString(nodeConceptTypeToAdd.getCode()));
        nodeConceptTypeToAdd.setCode(nodeConceptTypeToAdd.getCode().replaceAll(" ", ""));
        
        if(conceptTypeService.isConceptTypeExist(selectedTheso.getCurrentIdTheso(), nodeConceptTypeToAdd)){
            MessageUtils.showErrorMessage("le type de concept existe déjà !");
            return;
        }

        conceptTypeService.addNewConceptType(selectedTheso.getCurrentIdTheso(), nodeConceptTypeToAdd);

        initForConceptType();
        MessageUtils.showInformationMessage("Ajout réussi !");
    }    
    
    /**
     * permet de chnager le type du concept
     */
    public void updateTypeConcept(int idUser) {

        if (selectedConceptType == null || selectedConceptType.isEmpty()) {
            MessageUtils.showWarnMessage("aucune relation n'est définie !");
            return;
        }

        if(isApplyToBranch()) {
            var allId  = conceptService.getIdsOfBranch(conceptView.getNodeConcept().getConcept().getIdConcept(),
                    selectedTheso.getCurrentIdTheso());

            if( (allId == null) || (allId.isEmpty())) {
                MessageUtils.showWarnMessage("aucun concept sélectionné !");
                return;
            }
            for (String conceptId : allId) {
                conceptService.setConceptType(selectedTheso.getCurrentIdTheso(), conceptId, selectedConceptType);
            }
        } else {
            conceptService.setConceptType(selectedTheso.getCurrentIdTheso(),
                    conceptView.getNodeConcept().getConcept().getIdConcept(), selectedConceptType)  ;
        }

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(), idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptView.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConcept(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
        
        MessageUtils.showInformationMessage("Le changement a réussi");
    }
    
    
    /**
     * permet d'ajouter un nouveau top concept si le groupe = null, on ajoute un
     * TopConcept sans groupe si l'id du concept est fourni, il faut controler
     * s'il est unique
     */
    public void updateLabel(String idTheso, String idLang, int idUser) {

        duplicate = false;
        if (prefLabel == null || prefLabel.isEmpty()) {
            MessageUtils.showWarnMessage("Le label est obligatoire !");
            PrimeFaces.current().ajax().update("containerIndex:rightTab:idRenameConcept");
            return;
        }

        // vérification si le term à ajouter existe déjà, s oui, on a l'Id, sinon, on a Null
        var value = fr.cnrs.opentheso.utils.StringUtils.convertString(prefLabel.trim());
        var termFound = termRepository.findByLexicalValueAndLangAndIdThesaurus(value, idLang, idTheso);

        if (termFound.isPresent()) {
            MessageUtils.showWarnMessage("Le label '" + termFound.get().getLexicalValue() + "' existe déjà ! voulez-vous continuer ?");
            duplicate = true;
            PrimeFaces.current().ajax().update("containerIndex:renameConceptMessage");
            return;
        }

        updateForced(idTheso, idLang, idUser);
    }

    public void updateForced(String idTheso, String idLang, int idUser) {

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptView.getNodeConcept().getConcept().getIdConcept());
        String idTerm = preferredTerm.map(PreferredTerm::getIdTerm).orElse(null);
        if (idTerm == null) {
            // le concept n'a aucun label, on lui en ajoute un

            var terme = Term.builder()
                    .idThesaurus(idTheso)
                    .lang(idLang)
                    .status("")
                    .lexicalValue(prefLabel.trim())
                    .source(StringUtils.isEmpty(source) ? "" : source)
                    .build();
            termService.addTerm(terme, conceptView.getNodeConcept().getConcept().getIdConcept(), idUser);
            /*MessageUtils.showErrorMessage("Erreur de cohérence de BDD !!");
            PrimeFaces.current().ajax().update("messageIndex");
            return;*/
        } else {

            // on vérifie si la tradcution existe, on la met à jour, sinon, on en ajoute une
            if (termService.isTermExistInLangAndThesaurus(idTerm, idTheso, idLang)) {
                termService.updateTermTraduction(prefLabel, idTerm, idLang, idTheso, idUser);
            } else {
                var term = fr.cnrs.opentheso.models.terms.Term.builder()
                        .lexicalValue(prefLabel)
                        .idTerm(idTerm)
                        .lang(idLang)
                        .idThesaurus(selectedTheso.getCurrentIdTheso())
                        .source("")
                        .status("")
                        .created(new Date())
                        .modified(new Date())
                        .build();
                termService.addTermTraduction(term, idUser);
            }
        }

        conceptService.updateDateOfConcept(idTheso, conceptView.getNodeConcept().getConcept().getIdConcept(), idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptView.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConcept(idTheso, conceptView.getNodeConcept().getConcept().getIdConcept(), idLang, currentUser);

        MessageUtils.showInformationMessage("Le concept a bien été modifié");

        PrimeFaces.current().ajax().update("containerIndex::idRenameConcept");
        PrimeFaces.current().executeScript("PF('renameConcept').hide();");

        if (tree.getSelectedNode() != null) {
            // si le concept en cours n'est pas celui sélectionné dans l'arbre, on se positionne sur le concept en cours dans l'arbre
            if (!((TreeNodeData) tree.getSelectedNode().getData()).getNodeId().equalsIgnoreCase(
                    conceptView.getNodeConcept().getConcept().getIdConcept())) {
                tree.expandTreeToPath(conceptView.getNodeConcept().getConcept().getIdConcept(), idTheso, idLang);
            }
            ((TreeNodeData) tree.getSelectedNode().getData()).setName(prefLabel);
            PrimeFaces.current().ajax().update("containerIndex:formLeftTab:tabTree:tree");
        }
        reset("");
    }

    public void cancel() {
        duplicate = false;
    }

    public void infosDelete() {

        if (conceptView.getNodeConcept().getNodeNT().isEmpty()) { // pas d'enfants
            MessageUtils.showWarnMessage(languageBean.getMsg("rightbody.conceptdialog.infoDeleteConcept"));// rightbody.conceptdialog.infoDeleteConcept "La suppression du concept est définitive !!";
        } else {
            MessageUtils.showWarnMessage(languageBean.getMsg("rightbody.conceptdialog.infoDeleteBranch")); //"La suppression de la branche est définitive !!";
        }
        forDelete = true;
    }

    public void deleteConcept() {
        if(!selectedTheso.getCurrentIdTheso().equalsIgnoreCase(conceptView.getNodeConcept().getConcept().getIdThesaurus())) {
            MessageUtils.showWarnMessage("Attention !! un autre onglet utilise un autre thésaurus, veuillez le fermer pour éviter les erreurs");
            PrimeFaces.current().executeScript("window.location.reload();");
            return;
        }
        if (roleOnThesaurusBean.getNodePreference() == null) {
            MessageUtils.showErrorMessage("Le thésaurus n'a pas de préférences !");
            return;
        }

        if (conceptView.getNodeConcept().getNodeNT().isEmpty()) {
            // suppression du concept
            if (!conceptService.deleteConcept(conceptView.getNodeConcept().getConcept().getIdConcept(),
                    selectedThesaurus.getCurrentIdTheso())) {
                MessageUtils.showErrorMessage("La suppression a échoué !!");
                return;
            }
        } else {
            /// suppression d'une branche
            if(!conceptService.deleteBranchConcept(conceptView.getNodeConcept().getConcept().getIdConcept(),
                    selectedThesaurus.getCurrentIdTheso())){
                MessageUtils.showErrorMessage("La suppression a échoué, vérifier la poly-hiérarchie pour le concept");
                return;                
            }
        }

        // mise à jour
        if (tree.getSelectedNode() != null) {
            // si le concept en cours n'est pas celui sélectionné dans l'arbre, on se positionne sur le concept en cours dans l'arbre
            if (!((TreeNodeData) tree.getSelectedNode().getData()).getNodeId().equalsIgnoreCase(
                    conceptView.getNodeConcept().getConcept().getIdConcept())) {
                tree.expandTreeToPath(conceptView.getNodeConcept().getConcept().getIdConcept(),
                        selectedThesaurus.getCurrentIdTheso(), selectedTheso.getCurrentLang());
            }
            TreeNode parent = tree.getSelectedNode().getParent();
            if (parent != null) {
                parent.getChildren().remove(tree.getSelectedNode());
                PrimeFaces.current().ajax().update("formLeftTab:tabTree:tree");
            }
        }

        if (CollectionUtils.isNotEmpty(conceptView.getNodeConcept().getNodeBT())){// !conceptView.getNodeConcept().getNodeBT().isEmpty()) {
            conceptView.getConcept(selectedThesaurus.getCurrentIdTheso(), conceptView.getNodeConcept().getNodeBT().get(0).getIdConcept(),
                    selectedTheso.getCurrentLang(), currentUser);
        }

        MessageUtils.showInformationMessage("Le concept a bien été supprimé");

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().executeScript("PF('deleteConcept').hide();");
        reset("");
    }

///////////////////////////////////////////////////////////////////////////////
//////// gestion des concepts dépérciés    
///////////////////////////////////////////////////////////////////////////////    
    public void deprecateConcept(String idConcept, String idTheso, int idUser) {

        if (!conceptService.deprecateConcept(idConcept, idTheso, idUser)) {
            MessageUtils.showErrorMessage("Le concept n'a pas été déprécié !");
            return;
        }

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), idConcept, idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idConcept)
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConceptForTree(idTheso, idConcept, conceptView.getSelectedLang(), currentUser);
        
        
        if (tree.getSelectedNode() != null) {
            // si le concept en cours n'est pas celui sélectionné dans l'arbre, on se positionne sur le concept en cours dans l'arbre
            if (!((TreeNodeData) tree.getSelectedNode().getData()).getNodeId().equalsIgnoreCase(
                    conceptView.getNodeConcept().getConcept().getIdConcept())) {
                tree.expandTreeToPath(conceptView.getNodeConcept().getConcept().getIdConcept(), idTheso, conceptView.getSelectedLang());
            }
            ((DefaultTreeNode) tree.getSelectedNode()).setType("deprecated");
            PrimeFaces.current().ajax().update("containerIndex:formLeftTab:tabTree:tree");
        }         

        MessageUtils.showInformationMessage("Le concept est maintenant obsolète");
        PrimeFaces.current().ajax().update("containerIndex:formLeftTab");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }
    
    public void approveConcept(String idConcept, String idTheso, int idUser){

        if (!conceptService.approveConcept(idConcept, idTheso, idUser)) {
            MessageUtils.showErrorMessage("le concept n'a pas été approuvé !");
            return;
        }
        if (replacedByRTRelation) {
            if (conceptView.getNodeConcept().getReplacedBy() != null && !conceptView.getNodeConcept().getReplacedBy().isEmpty()) {
                for (NodeIdValue nodeIdValue : nodeReplaceBy) {
                    relationService.addRelationRT(idConcept, idTheso, nodeIdValue.getId(), idUser);
                }
            }
        }

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), idConcept, idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idConcept)
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConceptForTree(idTheso, idConcept, conceptView.getSelectedLang(), currentUser);
        
        if (tree.getSelectedNode() != null) {
            // si le concept en cours n'est pas celui sélectionné dans l'arbre, on se positionne sur le concept en cours dans l'arbre
            if (!((TreeNodeData) tree.getSelectedNode().getData()).getNodeId().equalsIgnoreCase(
                    conceptView.getNodeConcept().getConcept().getIdConcept())) {
                tree.expandTreeToPath(conceptView.getNodeConcept().getConcept().getIdConcept(), idTheso, conceptView.getSelectedLang());
            }
            if((tree.getSelectedNode()).getChildCount() == 0)
                (tree.getSelectedNode()).setType("file");
            else 
                (tree.getSelectedNode()).setType("concept");

            PrimeFaces.current().ajax().update("containerIndex:formLeftTab:tabTree:tree");
        }
        
        MessageUtils.showInformationMessage("le concept n'est plus obsolète maintenant");
    }

    public void addReplacedBy() {
        
        if ( searchSelected == null || searchSelected.getIdConcept() == null || searchSelected.getIdConcept().isEmpty()) {
            MessageUtils.showInformationMessage("Pas de concept sélectionné !");
            return;
        }

        conceptService.addReplacedBy(conceptView.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(),
                searchSelected.getIdConcept(), currentUser.getNodeUser().getIdUser());

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(),
                conceptView.getNodeConcept().getConcept().getIdConcept(), currentUser.getNodeUser().getIdUser());

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptView.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConceptForTree(selectedTheso.getCurrentIdTheso(), conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Relation ajoutée avec succès");
        PrimeFaces.current().ajax().update("containerIndex:rightTab:idReplaceBy containerIndex:rightTab:idDeprecatedLabel");
    }
    
    public List<NodeSearchMini> getAutoComplet(String value) {
        List<NodeSearchMini> liste = new ArrayList<>();
        if (selectedTheso.getCurrentIdTheso() != null && conceptView.getSelectedLang() != null){
            liste = searchService.searchAutoCompletionForRelation(value, conceptView.getSelectedLang(),
                    selectedTheso.getCurrentIdTheso(), false);
        }
        return liste;
    }      

    public void deleteReplacedBy(String idConceptDeprecated, String idTheso, String idConceptReplaceBy, int idUser){

        if(idConceptReplaceBy == null || idConceptReplaceBy.isEmpty()) {
            MessageUtils.showErrorMessage("Pas de concept sélectionné !");
            return;              
        }

        conceptService.deleteReplacedBy(idConceptDeprecated, idTheso, idConceptReplaceBy);

        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), idConceptDeprecated, idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idConceptDeprecated)
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());

        conceptView.getConceptForTree(idTheso, idConceptDeprecated, conceptView.getSelectedLang(), currentUser);

        MessageUtils.showInformationMessage("Relation supprimée avec succès");

        PrimeFaces.current().ajax().update("containerIndex:idDeleteReplaceBy");
        reset("");
    }

    public void infosArk() {
        String message = "Permet de générer un identifiant Ark, si l'identifiant existe, il sera mise à jour !!";
        MessageUtils.showInformationMessage(message);
    }

    /**
     * permet de générer l'identifiant Ark, s'il n'existe pas, il sera créé,
     * sinon, il sera mis à jour.
     */
    public void generateArk() {
        ArrayList<String> idConcepts = new ArrayList<>();
        if(roleOnThesaurusBean.getNodePreference() == null) return;
        idConcepts.add(conceptView.getNodeConcept().getConcept().getIdConcept());
        if(roleOnThesaurusBean.getNodePreference().isUseOpenArk()) {
            String apiKeyOpenArk = cryptoService.decrypt(roleOnThesaurusBean.getNodePreference().getApiKeyOpenArk());
            generateOpenArkIds(idConcepts, apiKeyOpenArk, roleOnThesaurusBean.getNodePreference());
        } else {
            generateArkIds(idConcepts);
        }

        conceptView.getConcept(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
    }

    public void deleteArkWithOpenArk() {
        ArrayList<String> idConcepts = new ArrayList<>();
        if(roleOnThesaurusBean.getNodePreference() == null) return;
        idConcepts.add(conceptView.getNodeConcept().getConcept().getIdConcept());
        if(roleOnThesaurusBean.getNodePreference().isUseOpenArk()) {
            String apiKeyOpenArk = cryptoService.decrypt(roleOnThesaurusBean.getNodePreference().getApiKeyOpenArk());
            arkService.deleteArkWithOpenArk(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                    idConcepts, apiKeyOpenArk, roleOnThesaurusBean.getNodePreference());
        }

        conceptView.getConcept(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
    }


    /**
     * permet de générer les identifiants Ark pour les concepts qui n'en ont pas
     * si un identifiant n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void generateArkForConceptWithoutArk() {

        List<String> idConcepts = conceptService.getAllIdConceptOfThesaurusWithoutArk(selectedTheso.getCurrentIdTheso());

        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        generateArkIds(idConcepts);

        conceptView.getConcept(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
    }
    
    /**
     * permet de générer les identifiants Ark pour cette branche, si un
     * identifiant n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void generateArkForThisBranch() {
        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        if (conceptView.getNodeConcept() == null) {
            return;
        }
        nodeIdValues = new ArrayList<>();
        var idConcepts = conceptService.getIdsOfBranch(conceptView.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso());
        nodeIdValues = generateArkIds(idConcepts);

        conceptView.getConcept(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
    }

    /**
     * permet de mettre à jour les URI des identifiants Ark pour cette branche,
     * cette fonction ne fait que la mise à jour de l'URL et ne permet de créer des identifiants ARK
     */
    public void updateUriArkForThisBranch(){
        if(roleOnThesaurusBean.getNodePreference() == null) return;
        if(conceptView.getNodeConcept() == null) return;

        var idConcepts = conceptService.getIdsOfBranch(conceptView.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso());
        updateUriArkIds(idConcepts);
    }

    /**
     * permet de générer la totalité des identifiants Ark, si un identifiant
     * n'existe pas, il sera créé, sinon, il sera mis à jour.
    */
    public void generateAllArk() {

        var idConcepts = conceptService.getAllIdConceptOfThesaurus(selectedTheso.getCurrentIdTheso());

        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        nodeIdValues = new ArrayList<>();
        nodeIdValues = generateArkIds(idConcepts);
    }
    
    /**
     * permet de générer la totalité des identifiants Ark, si un identifiant
     * n'existe pas, il sera créé, sinon, il sera mis à jour.
    */
    public void generateAllArkFast() {

        var idConcepts = conceptService.getAllIdConceptOfThesaurus(selectedTheso.getCurrentIdTheso());

        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        nodeIdValues = arkService.generateArkIdFast(selectedTheso.getCurrentIdTheso(), idConcepts, selectedTheso.getCurrentLang());

        if(nodeIdValues == null) {
            MessageUtils.showInformationMessage("L'opération est terminée avec succès !!");
        } else {
            MessageUtils.showInformationMessage("L'opération est terminée, vérifier le fichier de résultat téléchargé !!");
        }

        conceptView.getConcept(conceptView.getNodeConcept().getConcept().getIdThesaurus(),
                conceptView.getNodeConcept().getConcept().getIdConcept(),
                conceptView.getSelectedLang(), currentUser);
    }

    private List<NodeIdValue> generateOpenArkIds(List<String> idConcepts, String apiKeyOpenArk, Preferences preference) {
        nodeIdValues = conceptAddService.generateOpenArkId(selectedTheso.getCurrentIdTheso(),
                idConcepts, selectedTheso.getCurrentLang(), preference, apiKeyOpenArk);
        if(nodeIdValues != null) {
            MessageUtils.showInformationMessage("L'opération est terminée avec succès !!");
        }
        /*else {
            MessageUtils.showInformationMessage("L'opération est terminée, vérifier le fichier de résultat téléchargé !!");
        }*/
        return nodeIdValues;
    }

    private List<NodeIdValue> generateArkIds(List<String> idConcepts) {
        nodeIdValues = conceptAddService.generateArkId(selectedTheso.getCurrentIdTheso(), idConcepts, selectedTheso.getCurrentLang(), null);
        if(nodeIdValues == null) {
            MessageUtils.showInformationMessage("L'opération est terminée avec succès !!");
        } else {
            MessageUtils.showInformationMessage("L'opération est terminée, vérifier le fichier de résultat téléchargé !!");
        }
        return nodeIdValues;
    }


    private void updateUriArkIds(List<String> idConcepts){

        if(!arkService.updateUriArk(selectedTheso.getCurrentIdTheso(), idConcepts)){
            MessageUtils.showErrorMessage("La mise à jour des URIs Ark a échoué !!");
            return;
        }
        MessageUtils.showInformationMessage("La mise à jour des URIs Ark a réussi !!");
    }

    public StreamedContent getResultOfProcess(){
        if(nodeIdValues == null) return null; 
        /// pour retourner le résultat du traitement
        byte[] datas = csvWriteHelper.writeCsvResultProcess(nodeIdValues, "idConcept", "Résultat");

        try ( ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name("resultat.csv")
                    .stream(() -> input)
                    .build();
        } catch (IOException ex) {
        }
        return new DefaultStreamedContent();            
    }

    public void infosHandle() {
        String message = "Permet de générer un identifiant Handle, si l'identifiant existe, il sera mise à jour !!";
        MessageUtils.showInformationMessage(message);
    }

    public void infosDeleteHandle() {
        String message = "Permet de supprimer un identifiant Handle, il sera définitivement supprimé !!";
        MessageUtils.showInformationMessage(message);
    }

    /**
     * permet de générer l'identifiant Handle, s'il n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void deleteHandle() {

        if (roleOnThesaurusBean.getNodePreference() == null) {
            MessageUtils.showErrorMessage("Pas de préférences pour le thésaurus !!");
            return;
        }
        if (conceptView.getNodeConcept().getConcept().getIdHandle() == null || conceptView.getNodeConcept().getConcept().getIdHandle().isEmpty()) {
            MessageUtils.showErrorMessage("Pas d'identifiant Handle à supprimer !!");
            return;
        }
        
        if(roleOnThesaurusBean.getNodePreference().isUseHandleWithCertificat()) {
            if (!handleHelper.deleteIdHandle(conceptView.getNodeConcept().getConcept().getIdHandle(), roleOnThesaurusBean.getNodePreference())) {
                MessageUtils.showErrorMessage(handleHelper.getMessage());
                MessageUtils.showErrorMessage("La suppression de Handle a échoué !!");
                return;
            }

            handleConceptService.updateHandleIdOfConcept(conceptView.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), "");
            MessageUtils.showErrorMessage("La suppression de Handle a réussi !!");
        } else {
            handleService.applyNodePreference(roleOnThesaurusBean.getNodePreference());
            if(!handleService.connectHandle()){
                MessageUtils.showErrorMessage("La suppression de Handle a échoué !!");
                return;
            }
            try {
                handleService.deleteHandle(conceptView.getNodeConcept().getConcept().getIdHandle());
            } catch (Exception ex) {
                MessageUtils.showErrorMessage("La suppression de Handle a échoué !!");
                return;
            }
            handleConceptService.updateHandleIdOfConcept(conceptView.getNodeConcept().getConcept().getIdConcept(), selectedTheso.getCurrentIdTheso(), "");
            MessageUtils.showInformationMessage("La suppression de Handle a réussi !!");
        }
    }

    /**
     * permet de générer l'identifiant Handle, s'il n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void generateHandle() {

        List<String> idConcepts = new ArrayList<>();
        idConcepts.add(conceptView.getNodeConcept().getConcept().getIdConcept());
        generateHandleIds(idConcepts);
    }

    /**
     * permet de générer les identifiants Handle pour les concepts qui n'en ont
     * pas si un identifiant n'existe pas, il sera créé, sinon, il sera mis à
     * jour.
     */
    public void generateHandleForConceptWithoutHandle() {

        var idConcepts = conceptService.getAllIdConceptsWithoutHandle(selectedTheso.getCurrentIdTheso());
        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        generateHandleIds(idConcepts);
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    /**
     * permet de générer les identifiants Handle pour cette branche, si un
     * identifiant n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void generateHandleForThisBranch() {
        if (roleOnThesaurusBean.getNodePreference() == null) {
            return;
        }
        if (conceptView.getNodeConcept() == null) {
            return;
        }
        var idConcepts = conceptService.getIdsOfBranch(conceptView.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentIdTheso());

        generateHandleIds(idConcepts);
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    /**
     * permet de générer la totalité des identifiants Handle, si un identifiant
     * n'existe pas, il sera créé, sinon, il sera mis à jour.
     */
    public void generateAllHandle() {

        var idConcepts = conceptService.getAllIdConceptOfThesaurus(selectedTheso.getCurrentIdTheso());
        generateHandleIds(idConcepts);
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    private void generateHandleIds(List<String> idConcepts) {
        if (!handleConceptService.generateHandleId(idConcepts, selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("La génération de Handle a échoué !!");
        } else {
            MessageUtils.showErrorMessage("La génération de Handle a réussi !!");
        }
    }

    public boolean isReplaced() {
        return replacedByRTRelation;
    }
}
