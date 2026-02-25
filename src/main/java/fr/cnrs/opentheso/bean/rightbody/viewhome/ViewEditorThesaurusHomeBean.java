package fr.cnrs.opentheso.bean.rightbody.viewhome;

import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusEditService;
import fr.cnrs.opentheso.services.UserRoleGroupService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.statistiques.StatistiqueService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.List;

import fr.cnrs.opentheso.utils.ToolsHelper;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "viewEditorThesaurusHomeBean")
public class ViewEditorThesaurusHomeBean implements Serializable {

    private final UserService userService;
    private final ConceptService conceptService;
    private final StatistiqueService statistiqueService;
    private final ThesaurusEditService thesaurusEditService;
    private final UserRoleGroupService userRoleGroupService;

    private boolean isViewPlainText, isInEditing;
    private String text, colorOfHtmlButton, colorOfTextButton;


    public void reset(){
        isInEditing = false;
        isViewPlainText = false;
        text = null;
    }
    
    public void initText(String idLanguage, String idThesaurus) {

        text = thesaurusEditService.getHtmlPage(idThesaurus, idLanguage);
        ToolsHelper toolsHelper = new ToolsHelper();
        text = toolsHelper.normalizeHtml(text);
        isInEditing = true;
        isViewPlainText = false;
        colorOfHtmlButton = "#F49F66;";
        colorOfTextButton = "#8C8C8C;";        
    }

    public String getThesaurusHomePage(String idLanguage, String idThesaurus){

        var homePage = thesaurusEditService.getHtmlPage(idThesaurus, idLanguage);
        PrimeFaces.current().ajax().update("containerIndex:meta:metadataTheso");
        PrimeFaces.current().ajax().update("containerIndex:thesoHomeData");
        return homePage;
    }
    
    public List<DcElement> meta(String idThesaurus){

        return thesaurusEditService.getThesaurusMetaDatas(idThesaurus);
    }

    /**
     * permet d'ajouter un copyright, s'il n'existe pas, on le créé,sinon, on applique une mise à jour 
     */
    public void updateThesaurusHomePage(String idLanguage, String idThesaurus) {
        ToolsHelper toolsHelper = new ToolsHelper();
        text = toolsHelper.normalizeHtml(text);
        var result = thesaurusEditService.updateThesaurusHomePage(idThesaurus, idLanguage, text);
        if (ObjectUtils.isEmpty(result)){
            MessageUtils.showErrorMessage("L'ajout a échoué !");
            return;               
        }

        MessageUtils.showInformationMessage("Texte ajouté avec succès");

        isInEditing = false;
        isViewPlainText = false;
    }

    public String getTotalConceptOfThesaurus(String idThesaurus){

        return String.valueOf(statistiqueService.countValidConceptsByThesaurus(idThesaurus));
    }

    public String getLastModifiedDate(String idThesaurus){

        var date = conceptService.getLastModification(idThesaurus);
        return ObjectUtils.isEmpty(date) ? "" : date.toString();
    }

    public String getProjectName(String idThesaurus){

        var idProject = userService.getGroupOfThisThesaurus(idThesaurus);
        return (idProject != -1) ? userRoleGroupService.getUserGroupLabelRepository(idProject).getLabel() : "";
    }

    public List<NodeIdValue> getLastModifiedConcepts(String idThesaurus, String idLanguage){
        return conceptService.getLastModifiedConcepts(idThesaurus, idLanguage);
    }        
    
    public void setViewPlainTextTo(boolean status){
        if(status) {
            colorOfHtmlButton = "#8C8C8C;";
            colorOfTextButton = "#F49F66;";
        } else {
            colorOfHtmlButton = "#F49F66;";
            colorOfTextButton = "#8C8C8C;";            
        } 
        isViewPlainText = status;
    }
    
}
