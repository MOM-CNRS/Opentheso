package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.primefaces.model.TreeNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class EditThesaurusService {

    private final GroupService groupService;
    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;


    public String addNewThesaurus(String title, String selectedLang, String selectedProject, String userName) {

        int idProject = -1;
        try {
            if (!StringUtils.isEmpty(selectedProject)) {
                idProject = Integer.parseInt(selectedProject);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalide project id {}", selectedProject);
        }

        // création du thésaurus
        var idNewThesaurus = thesaurusService.addThesaurusRollBack();
        if(idNewThesaurus == null) {
            MessageUtils.showErrorMessage("Erreur pendant la création");
            return null;
        }

        var thesaurus = new fr.cnrs.opentheso.models.thesaurus.Thesaurus();
        thesaurus.setCreator(userName);
        thesaurus.setContributor(userName);
        thesaurus.setId_thesaurus(idNewThesaurus);
        thesaurus.setTitle(title);
        thesaurus.setLanguage(selectedLang);
        thesaurusService.addThesaurusTraductionRollBack(thesaurus);

        // ajouter le thésaurus dans le group de l'utilisateur
        if (idProject != -1) { // si le groupeUser = - 1, c'est le cas d'un SuperAdmin, alors on n'intègre pas le thésaurus dans un groupUser
            var userGroupThesaurus = UserGroupThesaurus.builder().idThesaurus(idNewThesaurus).idGroup(idProject).build();
            groupService.saveUserGroupThesaurus(userGroupThesaurus);
        }

        // écriture des préférences en utilisant le thésaurus en cours pour duppliquer les infos
        preferenceService.initPreferences(idNewThesaurus, selectedLang);

        // création des Dc-terms automatiquement
        createAndSaveDcTerm(idNewThesaurus, DCMIResource.CREATOR, userName, "", "string");
        createAndSaveDcTerm(idNewThesaurus, DCMIResource.TITLE, title, selectedLang, "string");
        createAndSaveDcTerm(idNewThesaurus, DCMIResource.LANGUAGE, selectedLang, "", "string");
        createAndSaveDcTerm(idNewThesaurus, DCMIResource.CREATED,
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()), "", "date");

        return idNewThesaurus;
    }

    private void createAndSaveDcTerm(String idThesaurus, String name, String value, String language, String type) {
        DcElement dcElement = new DcElement(name, value, language, type);
        try {
            ThesaurusDcTerm tmp = thesaurusDcTermRepository.save(
                    ThesaurusDcTerm.builder()
                            .idThesaurus(idThesaurus)
                            .name(dcElement.getName())
                            .value(dcElement.getValue())
                            .language(dcElement.getLanguage())
                            .dataType(dcElement.getType())
                            .build()
            );
            dcElement.setId(tmp.getId().intValue());
        } catch (DataIntegrityViolationException e) {
            log.debug("DC Term déjà existant, insertion ignorée : {} {} {}",
                    idThesaurus, name, value);
        }
    }

    public String generateArkIdForThesaurus(String idThesaurus) {

        log.debug("Regénération d'un identifiant Ark pour le thésaurus id {}", idThesaurus);
        var preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferences == null) {
            log.error("Erreur: Veuillez paramétrer les préférences pour ce thésaurus !!");
            return null;
        }

        var nodeThesaurus = thesaurusService.getNodeThesaurus(idThesaurus);
        if (preferences.isUseArk()) {
            ArkHelper2 arkHelper2 = new ArkHelper2(preferences);
            if (!arkHelper2.login()) {
                log.error("Erreur de connexion !!");
                MessageUtils.showErrorMessage("Erreur de connexion Ark !!");
                return null;
            }

            if (!preferences.isUseArk()) {
                log.error("Erreur: Veuillez activer Ark dans les préférences !!");
                return null;
            }
            var nodeMetaData = new NodeMetaData();
            nodeMetaData.setDcElementsList(new ArrayList<>());
            nodeMetaData.setTitle(nodeThesaurus.getIdThesaurus());
            nodeMetaData.setSource(preferences.getPreferredName());
            nodeMetaData.setCreator("");
            var privateUri = "?idt=" + idThesaurus;
            if (StringUtils.isEmpty(nodeThesaurus.getIdArk())) {
                if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
                    log.error(arkHelper2.getMessage() + "  idThesaurus = " + nodeThesaurus.getIdThesaurus());
                    log.error("La création Ark a échoué ici : " + nodeThesaurus.getIdThesaurus());
                    return null;
                }
                if (thesaurusService.updateIdArkOfThesaurus(idThesaurus, arkHelper2.getIdArk())) {
                    return null;
                }
                return nodeThesaurus.getIdArk();
            }
            return arkHelper2.getIdArk();
        }
        if (preferences.isUseArkLocal()) {
            String idArk = nodeThesaurus.getIdArk();
            if (StringUtils.isEmpty(idArk)) {
                idArk = ToolsHelper.getNewId(preferences.getSizeIdArkLocal(), preferences.isUppercaseForArk(), true);
                idArk = preferences.getNaanArkLocal() + "/" + preferences.getPrefixArkLocal() + idArk;
                if (thesaurusService.updateIdArkOfThesaurus(idThesaurus, idArk)) {
                    return null;
                }
                MessageUtils.showInformationMessage("L'identifiant Ark a bien été généré !!");
            } else {
                MessageUtils.showInformationMessage("Ark existe déjà, pas de changement !!");
            }

            return StringUtils.isEmpty(idArk) ? "" : idArk;
        }
        return null;
    }

    public void updateCollectionsStatus(TreeNode<NodeGroup> element, boolean newStatus) {
        for (TreeNode<NodeGroup> group : element.getChildren()) {
            groupService.setGroupVisibility(group.getData().getConceptGroup().getIdGroup(),
                    group.getData().getConceptGroup().getIdThesaurus(), newStatus);
            group.getData().setGroupPrivate(newStatus);

            if (CollectionUtils.isNotEmpty(element.getChildren())) {
                for (TreeNode<NodeGroup> tmp : element.getChildren()) {
                    updateCollectionsStatus(tmp, newStatus);
                }
            }
        }
    }

    public TreeNode<NodeGroup> getTreeNode(List<TreeNode<NodeGroup>> nodes, NodeGroup group) {

        TreeNode<NodeGroup> tmp = null;
        for (TreeNode<NodeGroup> node : nodes) {
            if (node.getData().getConceptGroup().getIdGroup().equals(group.getConceptGroup().getIdGroup())) {
                return node;
            }
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                tmp = getTreeNode(node.getChildren(), group);
            }
        }
        return tmp;
    }
}
