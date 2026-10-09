package fr.cnrs.opentheso.bean.group;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.rightbody.viewgroup.GroupView;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "traductionGroupBean")
public class TraductionGroupBean implements Serializable {
    
    private final GroupView groupView;
    private final SelectedTheso selectedTheso;
    private final GroupService groupService;

    private String selectedLang;
    private List<NodeLangTheso> nodeLangs, nodeLangsFiltered;
    private List<NodeGroupTraductions> nodeGroupTraductionses;
    private String traductionValue;
    
    
    public void reset() {
        nodeLangs = selectedTheso.getNodeLangs();
        nodeLangsFiltered = new ArrayList<>();
        nodeGroupTraductionses = groupView.getNodeGroupTraductions();

        selectedLang = null;
        traductionValue = "";
    }

    public void infos() {
        MessageUtils.showInformationMessage("Rédiger une aide ici pour Traduction de groupe !");
    }

    public void setLangWithNoTraduction() {
        
        nodeLangsFiltered.addAll(nodeLangs);

        // les langues à ignorer
        List<String> langsToRemove = new ArrayList<>();
        langsToRemove.add(groupView.getNodeGroup().getIdLang());
        for (NodeGroupTraductions nodeGroupTraductions : groupView.getNodeGroupTraductions()) {
            langsToRemove.add(nodeGroupTraductions.getIdLang());
        }
        for (NodeLangTheso nodeLang : nodeLangs) {
            if (langsToRemove.contains(nodeLang.getCode())) {
                nodeLangsFiltered.remove(nodeLang);
            }
        }
        if (nodeLangsFiltered.isEmpty()) {
            MessageUtils.showWarnMessage("Le Groupe est traduit déjà dans toutes les langues du thésaurus !!!");
        }
    }
    
    public void addNewTraduction(int idUser) {
        
        PrimeFaces pf = PrimeFaces.current();
        if (traductionValue == null || traductionValue.isEmpty()) {
            MessageUtils.showErrorMessage(" Une valeur est obligatoire !");
            return;
        }
        if (selectedLang == null || selectedLang.isEmpty()) {
            MessageUtils.showErrorMessage(" Pas de langue choisie !");
            return;
        }

        if (groupService.isDomainExist(traductionValue, selectedLang, selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage(" Ce nom existe déjà dans cette langue !");
            return;
        }

        groupService.addGroupTraduction(groupView.getNodeGroup().getConceptGroup().getIdGroup(), selectedTheso.getCurrentIdTheso(),
                selectedLang, traductionValue);
        groupService.updateModifiedDate(groupView.getNodeGroup().getConceptGroup().getIdGroup(), selectedTheso.getCurrentIdTheso());

        groupView.getGroup(selectedTheso.getCurrentIdTheso(),
                groupView.getNodeGroup().getConceptGroup().getIdGroup(),
                groupView.getNodeGroup().getIdLang());

        MessageUtils.showInformationMessage(" traduction ajoutée avec succès !");
        reset();
        setLangWithNoTraduction();
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }
    
    public void updateTraduction(NodeGroupTraductions nodeGroupTraductions, int idUser) {

        if (nodeGroupTraductions == null || nodeGroupTraductions.getTitle().isEmpty()) {
            MessageUtils.showErrorMessage("Veuillez saisir une valeur !");
            return;
        }

        if (groupService.isDomainExist(nodeGroupTraductions.getTitle(), nodeGroupTraductions.getIdLang(), selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Ce nom existe déjà dans cette langue !");
            return;
        }

        if (!groupService.renameGroup(nodeGroupTraductions.getTitle(), nodeGroupTraductions.getIdLang(),
                groupView.getNodeGroup().getConceptGroup().getIdGroup(), selectedTheso.getCurrentIdTheso(), idUser)) {
            MessageUtils.showErrorMessage("Erreur pendant la modification de la traduction !");
            return;
        }

        groupView.getGroup(selectedTheso.getCurrentIdTheso(), groupView.getNodeGroup().getConceptGroup().getIdGroup(),
                groupView.getNodeGroup().getIdLang());

        MessageUtils.showInformationMessage("Traduction modifiée avec succès !");
        reset();
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }
    
    public void deleteTraduction(NodeGroupTraductions nodeGroupTraductions, int idUser) {
        
        if (nodeGroupTraductions == null || nodeGroupTraductions.getIdLang().isEmpty()) {
            MessageUtils.showErrorMessage(" Erreur de sélection de tradcution !");
            return;
        }

        if (!groupService.deleteGroupTraduction(groupView.getNodeGroup().getConceptGroup().getIdGroup(),
                selectedTheso.getCurrentIdTheso(), nodeGroupTraductions.getIdLang())) {
            MessageUtils.showErrorMessage(" La suppression a échoué !");
            return;
        }

        groupView.getGroup(selectedTheso.getCurrentIdTheso(), groupView.getNodeGroup().getConceptGroup().getIdGroup(),
                groupView.getNodeGroup().getIdLang());

        MessageUtils.showInformationMessage("Traduction supprimée avec succès !");
        reset();
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }
}
