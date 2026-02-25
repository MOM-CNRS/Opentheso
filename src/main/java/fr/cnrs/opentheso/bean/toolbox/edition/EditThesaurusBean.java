package fr.cnrs.opentheso.bean.toolbox.edition;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.bean.menu.connect.MenuBean;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.services.EditThesaurusService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.data.domain.Sort;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "editThesaurusBean")
public class EditThesaurusBean implements Serializable {

    private final MenuBean menuBean;
    private final CurrentUser currentUser;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final ThesaurusMetadataAdd thesaurusMetadataAdd;

    private final GroupService groupService;
    private final PreferenceService preferenceService;
    private final ThesaurusService thesaurusService;
    private final EditThesaurusService editThesaurusService;
    private final LanguageRepository languageRepository;
    private final RoleOnThesaurusBean roleOnTheso;

    private List<LanguageIso639> allLangs;
    private List<NodeLangTheso> languagesOfThesaurus;
    private TreeNode<NodeGroup> groupRoot;
    private NodeLangTheso langSelected;
    private NodeIdValue nodeIdValueOfThesaurus;
    private boolean isPrivateThesaurus;
    private int activeTabIndex;
    private String title, selectedLang, preferredLang, arkIdOfThesaurus, newIdThesaurus;


    public void init(String idThesaurus) throws IOException {

        activeTabIndex = 0;
        nodeIdValueOfThesaurus = new NodeIdValue();
        nodeIdValueOfThesaurus.setId(idThesaurus);

        var thesaurus = thesaurusService.getThesaurusById(idThesaurus);
        if (idThesaurus != null) {
            arkIdOfThesaurus = thesaurus.getIdArk();
        }

        init();
        menuBean.redirectToEditionPage();
        thesaurusMetadataAdd.init(nodeIdValueOfThesaurus.getId());
    }    
    
    public void init(NodeIdValue nodeIdValueOfTheso) {
        this.nodeIdValueOfThesaurus = nodeIdValueOfTheso;
        activeTabIndex = 0;
        var thesaurus = thesaurusService.getThesaurusById(nodeIdValueOfThesaurus.getId());
        if (thesaurus != null) {
            arkIdOfThesaurus = thesaurus.getIdArk();
        }
        init();
        thesaurusMetadataAdd.init(nodeIdValueOfTheso.getId());
    }

    private void init() {

        activeTabIndex = 0;

        var thesaurus = thesaurusService.getThesaurusById(nodeIdValueOfThesaurus.getId());
        isPrivateThesaurus = thesaurus.getIsPrivate();

        var nodePreference = preferenceService.getThesaurusPreferences(nodeIdValueOfThesaurus.getId());
        if(nodePreference == null) {
            preferenceService.initPreferences(thesaurus.getIdThesaurus(), preferredLang);
            nodePreference = preferenceService.getThesaurusPreferences(nodeIdValueOfThesaurus.getId());
            if(nodePreference == null) {
                return;
            }
        }
        preferredLang = nodePreference.getSourceLang();
        allLangs = languageRepository.findAll(Sort.by(Sort.Direction.ASC, "iso6391"));
        languagesOfThesaurus = thesaurusService.getAllUsedLanguagesOfThesaurusNode(nodeIdValueOfThesaurus.getId(), preferredLang);
        selectedLang = null;
        langSelected = new NodeLangTheso();
        title = "";

        groupRoot = new DefaultTreeNode<>(new NodeGroup(), null);
        var elements = groupService.getListRootConceptGroup(nodeIdValueOfThesaurus.getId(), preferredLang, false, false);
        setGroupTree(groupRoot, elements);
    }

    private void setGroupTree(TreeNode<NodeGroup> elementTree, List<NodeGroup> elements) {
        for (NodeGroup nodeGroup : elements) {
            var fils = new DefaultTreeNode(nodeGroup, elementTree);
            var tmp = groupService.getListChildsOfGroup(nodeGroup.getConceptGroup().getIdGroup(),
                    nodeIdValueOfThesaurus.getId(), preferredLang, false);

            if (CollectionUtils.isNotEmpty(tmp)) {
                setGroupTree(fils, tmp);
            }
        }
    }

    public void updateCollectionStatus(NodeGroup group) {

        if(ObjectUtils.isEmpty(group) || ObjectUtils.isEmpty(group.getConceptGroup())) {
            MessageUtils.showErrorMessage("Erreur : Groupe invalide");
            return;
        }

        groupService.setGroupVisibility(group.getConceptGroup().getIdGroup(), group.getConceptGroup().getIdThesaurus(),
                group.isGroupPrivate());

        if (group.isHaveChildren()) {
            var nodes = editThesaurusService.getTreeNode(groupRoot.getChildren(), group);
            editThesaurusService.updateCollectionsStatus(nodes, group.isGroupPrivate());
        }

        MessageUtils.showInformationMessage(String.format("La collection %s est maintenant %s", group.getLexicalValue(),
                group.isGroupPrivate() ? "privée" : "publique"));
    }

    public void reset() {
        selectedLang = null;
        title = "";
        preferredLang = null;
        newIdThesaurus = "";
    }

    public void generateArkId(){
        arkIdOfThesaurus = editThesaurusService.generateArkIdForThesaurus(nodeIdValueOfThesaurus.getId());
    }

    public void modifyIdOfThesaurus() {

        if(!thesaurusService.changeIdOfThesaurus(nodeIdValueOfThesaurus.getId(), newIdThesaurus)) {
            MessageUtils.showErrorMessage("Erreur de changement d'identifiant, vérifiez que l'identifant est unique !!!");
            return;
        }

        nodeIdValueOfThesaurus.setId(newIdThesaurus);
        roleOnThesaurusBean.showListThesaurus(currentUser, newIdThesaurus);
        MessageUtils.showInformationMessage("Le changement d'identifiant a réussi, veuillez recharger les thésaurus");
    }

    public void changeSourceLang(){

        if (ObjectUtils.isEmpty(nodeIdValueOfThesaurus) || StringUtils.isEmpty(nodeIdValueOfThesaurus.getId())) {
            MessageUtils.showErrorMessage("Pas de thésaurus sélectionné !!!");
            return;
        }

        if (StringUtils.isEmpty(preferredLang)) {
            MessageUtils.showErrorMessage("La langue source est obligatoire !!!");
            return;
        }
        
        if (!preferenceService.setWorkLanguageOfThesaurus(preferredLang, nodeIdValueOfThesaurus.getId())) {
            MessageUtils.showErrorMessage("Erreur pendant la modification de la langue source !!!");
            return;
        }

        MessageUtils.showInformationMessage("Langue source modifiée avec succès");
        init(nodeIdValueOfThesaurus);
    }
    
    /**
     * permet de changer le status du thésaurus entre public et privé
     */
    public void changeStatus() {

        thesaurusService.setThesaurusVisibility(nodeIdValueOfThesaurus.getId(), isPrivateThesaurus);

        if (isPrivateThesaurus) {
            MessageUtils.showInformationMessage("Le thésaurus est maintenant privé");
        } else {
            MessageUtils.showInformationMessage("Le thésaurus est maintenant public");
        }

        init(nodeIdValueOfThesaurus);
        roleOnThesaurusBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        PrimeFaces.current().ajax().update("toolBoxForm:idLangToModify");
    }

    /**
     * Permet de supprimer un thésaurus
     */
    public void addNewLang() {

        if (ObjectUtils.isEmpty(nodeIdValueOfThesaurus) || StringUtils.isEmpty(nodeIdValueOfThesaurus.getId())) {
            MessageUtils.showErrorMessage("Pas de thésaurus sélectionné !!!");
            return;
        }

        if (StringUtils.isEmpty(title)) {
            MessageUtils.showErrorMessage("Le label est obligatoire !!!");
            return;
        }

        if (StringUtils.isEmpty(selectedLang)) {
            MessageUtils.showErrorMessage("La langue est obligatoire !!!");
            return;
        }

        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setCreator(currentUser.getNodeUser().getName());
        thesaurus.setContributor(currentUser.getNodeUser().getName());
        thesaurus.setId_thesaurus(nodeIdValueOfThesaurus.getId());
        thesaurus.setTitle(title);
        thesaurus.setLanguage(selectedLang);
        thesaurusService.addThesaurusTraductionRollBack(thesaurus);

        roleOnThesaurusBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        MessageUtils.showInformationMessage("Langue ajoutée avec succès");
        init(nodeIdValueOfThesaurus);
        PrimeFaces.current().ajax().update("containerIndex");
    }
    
    public void updateLang(){

        if (ObjectUtils.isEmpty(nodeIdValueOfThesaurus) || StringUtils.isEmpty(nodeIdValueOfThesaurus.getId())) {
            MessageUtils.showErrorMessage("Pas de thésaurus sélectionné !!!");
            return;
        }

        if (ObjectUtils.isEmpty(langSelected) || StringUtils.isEmpty(langSelected.getValue())) {
            MessageUtils.showErrorMessage("Le label est obligatoire !!!");
            return;
        }

        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setCreator(currentUser.getNodeUser().getName());
        thesaurus.setContributor(currentUser.getNodeUser().getName());
        thesaurus.setId_thesaurus(nodeIdValueOfThesaurus.getId());
        thesaurus.setTitle(langSelected.getLabelTheso());
        thesaurus.setLanguage(langSelected.getCode());

        if (!thesaurusService.updateThesaurus(thesaurus)) {
            MessageUtils.showErrorMessage("Erreur pendant la modification !!!");
            return;
        }

        roleOnThesaurusBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        MessageUtils.showInformationMessage("Langue modifiée avec succès");

        var sourceLang = preferenceService.getWorkLanguageOfThesaurus(nodeIdValueOfThesaurus.getId());
        languagesOfThesaurus = thesaurusService.getAllUsedLanguagesOfThesaurusNode(nodeIdValueOfThesaurus.getId(), sourceLang);
        PrimeFaces.current().ajax().update("containerIndex");
    }
    
    public void deleteLangFromThesaurus(String idLang){

        if (ObjectUtils.isEmpty(nodeIdValueOfThesaurus) || StringUtils.isEmpty(nodeIdValueOfThesaurus.getId())) {
            MessageUtils.showErrorMessage("Pas de thésaurus sélectionné !!!");
            return;
        }

        if (StringUtils.isEmpty(idLang)) {
            MessageUtils.showErrorMessage("Pas de langue sélectionnée !!!");
            return;
        }

        thesaurusService.deleteThesaurusTraduction(nodeIdValueOfThesaurus.getId(), idLang);

        MessageUtils.showInformationMessage("Langue supprimée avec succès");
        init(nodeIdValueOfThesaurus);
    }

    public void setLangSelected(NodeLangTheso langSelected) {
        if(langSelected == null) langSelected = new NodeLangTheso();
        this.langSelected = langSelected;
    }
}
