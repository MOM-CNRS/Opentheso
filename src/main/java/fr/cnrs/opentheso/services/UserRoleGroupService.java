package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.Roles;
import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.entites.UserRoleGroup;
import fr.cnrs.opentheso.entites.UserRoleOnlyOn;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.users.NodeUserRole;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.repositories.RoleRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserGroupLabelRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.repositories.UserRoleOnlyOnRepository;
import fr.cnrs.opentheso.repositories.UserRoleGroupRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class UserRoleGroupService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ThesaurusRepository thesaurusRepository;
    private final UserRoleGroupRepository userRoleGroupRepository;
    private final UserRoleOnlyOnRepository userRoleOnlyOnRepository;
    private final UserGroupLabelRepository userGroupLabelRepository;
    private final ThesaurusService thesaurusService;
    private final UserService userService;
    private final PreferenceService preferenceService;


    @Transactional
    public boolean addUserRoleOnGroup(Integer idUSer, Integer idRole, Integer idGroup) {

        var user = userRepository.findById(idUSer);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUSer);
            return false;
        }

        var role = roleRepository.findById(idRole);
        if (role.isEmpty()) {
            log.error("Le rôle id {} n'existe pas", idRole);
            return false;
        }

        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        var isSuperAdmin = role.get().getId() == 1;
        if (isSuperAdmin) {
            userRoleGroupRepository.deleteByUserAndGroup(user.get(), group.get());
        } else {
            var userRoleGroup = UserRoleGroup.builder()
                    .user(user.get())
                    .role(role.get())
                    .group(group.get())
                    .build();
            userRoleGroupRepository.save(userRoleGroup);
        }

        user.get().setIsSuperAdmin(isSuperAdmin);
        userRepository.save(user.get());
        return true;
    }

    @Transactional
    public boolean addUserRoleOnTheso(int idUser, int idRole, int idGroup, List<String> idThesaurusList) {

        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return false;
        }

        var role = roleRepository.findById(idRole);
        if (role.isEmpty()) {
            log.error("Le rôle id {} n'existe pas", idRole);
            return false;
        }

        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        userRoleGroupRepository.deleteByUserAndGroup(user.get(), group.get());

        for (String idThesaurus : idThesaurusList) {
            var thesaurus = thesaurusRepository.findById(idThesaurus);
            if (thesaurus.isEmpty()) {
                log.error("Le thésaurus id {} n'existe pas", idThesaurus);
                return false;
            }

            var userRoleGroupOn = UserRoleOnlyOn.builder()
                    .user(user.get())
                    .role(role.get())
                    .group(group.get())
                    .thesaurus(thesaurus.get())
                    .build();
            userRoleOnlyOnRepository.save(userRoleGroupOn);
        }

        return true;
    }

    @Transactional
    public boolean updateUserRoleOnGroup(int idUser, int idRole, int idGroup) {

        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return false;
        }

        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        if (idRole == 1) {
            userRoleGroupRepository.deleteByUserAndGroup(user.get(), group.get());
        } else {
            var role = roleRepository.findById(idRole);
            if (role.isEmpty()) {
                log.error("Le rôle id {} n'existe pas", idRole);
                return false;
            }
            userRoleGroupRepository.updateUserRole(user.get(), group.get(), role.get());
        }

        var isSuperAdmin = (idRole == 1);
        user.get().setIsSuperAdmin(isSuperAdmin);
        userRepository.save(user.get());
        return true;
    }

    @Transactional
    public boolean updateLimitedRoleOnThesaurusForUser(int idUser,
                                                       int oldIdRole, int newIdRole,
                                                       String idTheso, int idGroup,
                                                       boolean limitOnThesaurus) {
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return false;
        }
        if(newIdRole == -1) {
            log.error("Le role n'est pas connue {} ", newIdRole);
            return false;
        }
        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        if(limitOnThesaurus) {
            var role_old = roleRepository.findById(oldIdRole);
            if (role_old.isEmpty()) {
                log.error("Le rôle id {} n'existe pas", oldIdRole);
                return false;
            }
            var thesaurus = thesaurusService.getThesaurusById(idTheso);

            // supprimer l'ancien role
            userRoleOnlyOnRepository.deleteByUserAndGroupAndRoleAndThesaurus(
                    user.get(),
                    group.get(),
                    role_old.get(),
                    thesaurus
            );
            var role_new = roleRepository.findById(newIdRole);
            if (role_old.isEmpty()) {
                log.error("Le rôle id {} n'existe pas", newIdRole);
                return false;
            }
            userRoleOnlyOnRepository.save(UserRoleOnlyOn.builder()
                    .user(user.get())
                    .role(role_new.get())
                    .group(group.get())
                    .thesaurus(thesaurus)
                    .build());
        } else {
            userRoleOnlyOnRepository.deleteByUserAndGroup(user.get(), group.get());
            addUserRoleOnGroup(user.get().getId(), newIdRole, idGroup);
        }
        return true;
    }

    public boolean deleteByUserAndGroup(int idUser, int idGroup) {

        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return false;
        }
        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        userRoleGroupRepository.deleteByUserAndGroup(user.get(), group.get());
        return true;
    }

    public boolean removeUserRoleOnThesaurus(int idUser, int idRole, int idGroup, String idThesaurus) {

        log.debug("Suppression de l'utilisateur {} avec le rôle {} du thésaurus {} et le group {}", idUser, idRole, idThesaurus, idGroup);
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return false;
        }

        var role = roleRepository.findById(idRole);
        if (role.isEmpty()) {
            log.error("Le rôle id {} n'existe pas", idRole);
            return false;
        }

        var group = userGroupLabelRepository.findById(idGroup);
        if (group.isEmpty()) {
            log.error("Le group id {} n'existe pas", idGroup);
            return false;
        }

        var thesaurus = thesaurusService.getThesaurusById(idThesaurus);
        if (thesaurus == null) {
            log.error("Le thésaurus id {} n'existe pas", idThesaurus);
            return false;
        }

        userRoleOnlyOnRepository.deleteByUserAndGroupAndRoleAndThesaurus(user.get(), group.get(), role.get(), thesaurus);
        return true;
    }

    @Transactional
    public boolean updateUserRoleLimitedForSelectedUser(int idUser, int idGroup, int idRole,
                                                        List<String> idThesaurusList, boolean limitOnThesaurus) {
        // contrôle si le role est uniquement sur une liste des thésaurus ou le projet entier
        if(limitOnThesaurus) {
            var user = userService.getById(idUser);
            var userGroupLabel = userGroupLabelRepository.findById(idGroup);
            if (userGroupLabel.isEmpty()) {
                log.error("Aucun user group label existe avec l'id group {}", idGroup);
                return false;
            }
            userRoleOnlyOnRepository.deleteByUserAndGroup(user, userGroupLabel.get());
            return addUserRoleOnTheso(idUser, idRole, idGroup, idThesaurusList);
        } else {
            return updateUserRoleOnGroup(idUser, idRole, idGroup);
        }
    }

    public List<NodeUserRoleGroup> getRoleProjectByUser(int idUser) {
        return userRoleGroupRepository.getUserRoleGroup(idUser);
    }


    public List<NodeUserRole> getListRoleByThesaurusLimited(int idGroup, int idUser) {

        var roles = userRoleOnlyOnRepository.getListRoleByThesaurusLimited(idGroup, idUser);
        roles.forEach(role -> {
            var idLang = preferenceService.getWorkLanguageOfThesaurus(role.getIdTheso());
            var thesaurusTitle = thesaurusService.getTitleOfThesaurus(role.getIdTheso(), idLang);
            role.setThesoName(thesaurusTitle);
        });
        return roles;
    }

    public List<Roles> getAllRoles() {
        log.debug("Recherche de tous les roles disponible");
        return roleRepository.findAll();
    }

    public List<UserGroupLabel> findAllUserRoleGroup() {

        log.debug("Recherche de tous les user role group disponible");
        var users = userGroupLabelRepository.findAll();
        if (users.isEmpty()) {
        //    log.error("Aucun group n'est trouvé");
            return List.of();
        }
        return users;
    }

    public UserGroupLabel getUserGroupLabelRepository(int idGroup) {

        log.debug("Recherche du group utilisateur id {}", idGroup);
        var userGroup = userGroupLabelRepository.findById(idGroup);
        if (userGroup.isEmpty()) {
            log.error("Aucun group utilisateur trouvé avec id {}", idGroup);
            return null;
        }
        return userGroup.get();
    }

    public UserRoleOnlyOn getRole(User user, UserGroupLabel group, Thesaurus thesaurus) {

        return userRoleOnlyOnRepository.findByUserAndGroupAndThesaurus(user, group, thesaurus);
    }

    public Roles getRoleById(int idRole) {

        var role = roleRepository.findById(idRole);
        if (role.isEmpty()) {
            log.error("Aucun rôle n'est trouvé avec l'id {}", idRole);
            return null;
        }
        return role.get();
    }

    public List<NodeIdValue> getRolesByIdGreaterThanEqual(int idRoleFrom) {

        log.debug("Recherche des rôles avec un id supérieur à {}", idRoleFrom);
        var roles = roleRepository.findAllByIdGreaterThanEqual(idRoleFrom);
        if (roles.isEmpty()) {
            log.error("Aucun rôle n'est trouvé avec un id supérieur à {}", idRoleFrom);
            return List.of();
        }
        return roles.stream()
                .map(element -> NodeIdValue.builder().id(element.getId() + "").value(element.getName()).build())
                .toList();
    }
}
