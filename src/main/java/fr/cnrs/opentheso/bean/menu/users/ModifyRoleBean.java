package fr.cnrs.opentheso.bean.menu.users;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.users.NodeUserRole;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.bean.profile.MyProjectBean;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserRoleGroupService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "modifyRoleBean")
public class ModifyRoleBean implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final MyProjectBean myProjectBean;
    private final ThesaurusService thesaurusService;
    private final UserRoleGroupService userRoleGroupService;
    private final UserService userService;

    private boolean limitOnThesaurus;
    private User nodeSelectedUser, selectedUser;
    private String selectedProject, roleOfSelectedUser;

    private List<NodeUserRoleGroup> allMyRoleProject;
    private List<NodeIdValue> listThesaurusOfProject, myAuthorizedRolesLimited;
    private List<String> selectedThesaurus; 
    private NodeUserRole selectedNodeUserRole;
    private List<NodeUserRole> listeLimitedThesaurusRoleForUser;

    private int oldIdRole;

    /**
     * permet de selectionner l'utilisateur dans la liste avec toutes les informations nécessaires pour sa modification
     */
    public void selectUser(int idUser, int roleOfSelectedUser, String selectedProject) {

        nodeSelectedUser = userService.getById(idUser);
        this.selectedProject = selectedProject;

        this.roleOfSelectedUser = "" + roleOfSelectedUser;
        initAllMyRoleProject();

        limitOnThesaurus = false;
        listThesaurusOfProject = null;
        selectedThesaurus = null;    
        myAuthorizedRolesLimited = null;
    }

    public void updateAjax(){
        int i = 0;
    }
    
    /**
     * permet de selectionner l'utilisateur qui a des droits limités 
     * informations nécessaires pour sa modification
     */
    public void selectUserWithLimitedRole(NodeUserRole selectedNodeUserRole, String selectedProject) {

        this.selectedNodeUserRole = selectedNodeUserRole;
        this.oldIdRole = selectedNodeUserRole.getIdRole();
        this.selectedProject = selectedProject;
        this.limitOnThesaurus = true;
        this.myAuthorizedRolesLimited = null;
        this.selectedThesaurus = userService.getSelectedThesaurus(Integer.parseInt(selectedProject), selectedNodeUserRole.getIdUser());

        toggleLimitThesaurus();
        setLimitedRoleForThisUserByGroup();
    }    
    
    /**
     * permet de récupérer la liste des rôles pour l'utilisateur sur les thésaurus du projet
     */
    private void setLimitedRoleForThisUserByGroup(){

        if (StringUtils.isEmpty(selectedProject)) {
            return;
        }

        this.listeLimitedThesaurusRoleForUser = userService.getLimitedThesaurusForUser(selectedNodeUserRole.getIdUser(),
                selectedProject, selectedNodeUserRole, workLanguage);
        myAuthorizedRolesLimited = myProjectBean.getMyAuthorizedRoles();
    }

    /**
     * met à jour les rôles de l'utilisateur sur les thésaurus du projet ou 
     * si on redonne les droits sur le projet entier
     */
    public void updateLimitedRoleOnThesaurusForUser() {
        
         if(CollectionUtils.isEmpty(listeLimitedThesaurusRoleForUser))  {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné !!!");
            return;              
        }
       // var idRole2 = Integer.parseInt(roleOfSelectedUser);
        int newRole;
        if(limitOnThesaurus){
            newRole = selectedNodeUserRole.getIdRole();
        } else {
            newRole = Integer.parseInt(roleOfSelectedUser);
        }

        var idRole = myProjectBean.getMyAuthorizedRoles().get(0).getId();
            if (userRoleGroupService.updateLimitedRoleOnThesaurusForUser(
                    selectedNodeUserRole.getIdUser(),
                    oldIdRole,
                    newRole,
                    selectedNodeUserRole.getIdTheso(),
                    Integer.parseInt(selectedProject),
                    limitOnThesaurus)) {


            if(limitOnThesaurus) {
                myProjectBean.setSelectedIndex("2");
            } else {
                myProjectBean.setSelectedIndex("1");
            }

            MessageUtils.showInformationMessage("Le rôle a été changé avec succès");
            myProjectBean.resetListUsers();
        } else {
            MessageUtils.showErrorMessage("Erreur pendant le changement du rôle");
        }
    }
    
    /**
     * met à jour les rôles de l'utilisateur sur les thésaurus du projet
     */
    public void updateUserRoleLimitedForSelectedUser() {
        
        if(selectedNodeUserRole == null) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné");
            return;              
        }

        var idGroup = Integer.parseInt(selectedProject);
        var idRole = Integer.parseInt(roleOfSelectedUser);
        if (userRoleGroupService.updateUserRoleLimitedForSelectedUser(selectedNodeUserRole.getIdUser(), idGroup, idRole,
                selectedThesaurus, limitOnThesaurus)) {

            MessageUtils.showInformationMessage("Le rôle a été changé avec succès !!!");
            myProjectBean.resetListUsers();
        } else {
            MessageUtils.showErrorMessage("Erreur pendant lae changement du rôle");
        }
    }       
    
    /**
     * permet de supprimer le rôle de l'utilisateur sur ce thésaurus du projet
     */
    public void removeUserRoleOnThesaurus() {

        if(ObjectUtils.isEmpty(selectedNodeUserRole)) {
            MessageUtils.showErrorMessage("Aucun rôle sélectionné !!!");
            return;              
        }

        if (userRoleGroupService.removeUserRoleOnThesaurus(selectedNodeUserRole.getIdUser(), selectedNodeUserRole.getIdRole(),
                Integer.parseInt(selectedProject), selectedNodeUserRole.getIdTheso())) {
            MessageUtils.showInformationMessage("Le rôle a été supprimé");
            myProjectBean.resetListLimitedRoleUsers();
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la suppression du rôle");
        }
    }      
    
    private void initAllMyRoleProject(){
        allMyRoleProject = userRoleGroupService.getRoleProjectByUser(nodeSelectedUser.getId());
    }
   
    public void toggleLimitThesaurus(){
        if(!limitOnThesaurus) return;
        try {
            listThesaurusOfProject = thesaurusService.getThesaurusOfProject(Integer.parseInt(selectedProject), workLanguage);
        } catch (Exception e) { }
    }  
    
    /**
     * met à jour le nouveau rôle de l'utilisateur sur le projet
     */
    public void updateRoleForSelectedUser () {

        if(ObjectUtils.isEmpty(nodeSelectedUser)) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné !!!");
            return;
        }

        // contrôle si le role est uniquement sur une liste des thésaurus ou le projet entier 
        if(limitOnThesaurus) {
            userRoleGroupService.addUserRoleOnTheso(nodeSelectedUser.getId(), Integer.parseInt(roleOfSelectedUser),
                    Integer.parseInt(selectedProject), selectedThesaurus);
            myProjectBean.setSelectedIndex("2");
        } else {
            userRoleGroupService.updateUserRoleOnGroup(nodeSelectedUser.getId(), Integer.parseInt(roleOfSelectedUser),
                    Integer.parseInt(selectedProject));
            myProjectBean.setSelectedIndex("1");
        }

        MessageUtils.showInformationMessage("Le rôle a été changé avec succès !!!");
        myProjectBean.resetListUsers();
    }

    public void removeUserFromProject () {
        
        if(ObjectUtils.isEmpty(nodeSelectedUser)) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné !!!");
            return;              
        }

        if (userRoleGroupService.deleteByUserAndGroup(nodeSelectedUser.getId(), Integer.parseInt(selectedProject))) {
            MessageUtils.showInformationMessage("L'utilisateur a été supprimé du projet");
            myProjectBean.resetListUsers();
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la suppression de l'utilisateur du projet");
        }
    }    

    public void addUserToProject () {
        
        if(ObjectUtils.isEmpty(selectedUser)) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné !!!");
            return;              
        }

        var roleSelected = Integer.parseInt(roleOfSelectedUser);
        var projectSelected = Integer.parseInt(selectedProject);
        if (userRoleGroupService.addUserRoleOnGroup(selectedUser.getId(), roleSelected, projectSelected)) {
            MessageUtils.showInformationMessage("L'utilisateur a été ajouté avec succès");
            myProjectBean.resetListUsers();
            PrimeFaces.current().ajax().update("containerIndex");
        } else {
            MessageUtils.showErrorMessage("Erreur pendant l'ajout  de l'utilisateur au projet");
        }
    }      

    public List<User> autoCompleteUser(String userName) {
        return userService.getUserByUserNameLike(userName);
    }
}
