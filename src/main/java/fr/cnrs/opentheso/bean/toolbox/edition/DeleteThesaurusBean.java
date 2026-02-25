package fr.cnrs.opentheso.bean.toolbox.edition;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.IOException;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author miledrousset
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "deleteThesaurusBean")
public class DeleteThesaurusBean implements Serializable {

    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final PreferenceService preferenceService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;

    private String idThesaurusToDelete, valueOfThesaurusToDelete, currentIdThesaurus;
    private boolean isDeleteOn, deletePerennialIdentifiers;

    
    public void init() {

        idThesaurusToDelete = null;
        valueOfThesaurusToDelete = null;
        isDeleteOn = false;
        currentIdThesaurus = null;
        deletePerennialIdentifiers = false;
    }
    
    public void confirmDelete(NodeIdValue nodeThesaurus, String currentIdThesaurus) throws IOException {

        this.idThesaurusToDelete = nodeThesaurus.getId();
        this.valueOfThesaurusToDelete = nodeThesaurus.getValue();
        isDeleteOn = true;
        deletePerennialIdentifiers = false;
        this.currentIdThesaurus = currentIdThesaurus;
    }
    
    /**
     * Permet de supprimer un thésaurus 
     */
    public void deleteThesaurus(CurrentUser currentUser) throws IOException {

        if(idThesaurusToDelete == null) return;

        var nodePreference = preferenceService.getThesaurusPreferences(idThesaurusToDelete);
        if(nodePreference != null) {
            if(deletePerennialIdentifiers) {
                conceptService.deleteAllIdHandle(idThesaurusToDelete);
            }
        }

        thesaurusService.deleteDroitByThesaurus(idThesaurusToDelete);

        if(!thesaurusService.deleteThesaurus(idThesaurusToDelete)){
            MessageUtils.showErrorMessage("Erreur pendant la suppression !!!");
            return;
        }

        // vérification si le thésaurus supprimé est en cours de consultation, alors il faut nettoyer l'écran
        if(idThesaurusToDelete.equalsIgnoreCase(currentIdThesaurus)) {
            selectedTheso.setSelectedIdTheso("");
            selectedTheso.setSelectedLang(null);
            selectedTheso.setSelectedTheso();
            selectedTheso.setProjectIdSelected("-1");
            selectedTheso.setSelectedProject();
        }

        init();
        MessageUtils.showInformationMessage("Thesaurus supprimé avec succès");
        roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
    }
}
