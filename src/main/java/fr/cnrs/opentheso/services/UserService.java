package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.entites.UserRoleOnlyOn;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.userpermissions.NodeThesoRole;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.models.users.NodeUserComplet;
import fr.cnrs.opentheso.models.users.NodeUserGroupUser;
import fr.cnrs.opentheso.models.users.NodeUserRole;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.repositories.*;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.v2.user.policy.RoleLabels;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleGroupRepository userRoleGroupRepository;
    private final UserRoleOnlyOnRepository userRoleOnlyOnRepository;
    private final UserGroupLabelRepository userGroupLabelRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApiKeyService apiKeyService;


    public User getUserByApiKeyBcrypt(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiKeyMissingException();
        }

        Optional<User> userOpt = apiKeyService.findUserByApiKey(apiKey);
        if (userOpt.isEmpty()) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }
        return userOpt.get();
    }

    public Optional<User> findByMail(String mail) {
        return userRepository.findByMail(mail);
    }

    public User findByMailAndPassword(String mail, String password) {
        String matches = MD5Password.getEncodedPassword(password);

        return userRepository.findByMailAndPassword(mail, matches)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByMailAndPasswordMigrated(String mail, String rawPassword) {
        Optional<User> userOpt = userRepository.findByMail(mail);
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        String storedHash = user.getPassword();
        boolean matches = false;
        if(storedHash == null) {
            return null;
        }

        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")) {
            // mot de passe déjà en BCrypt
            matches = passwordEncoder.matches(rawPassword, storedHash);
        } else {
            // mot de passe MD5
            matches = MD5Password.getEncodedPassword(rawPassword).equals(storedHash);

            // Migration vers BCrypt
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }
        }

        return matches ? user : null;
    }

    public Optional<User> findById(Integer idUser) {
        return userRepository.findById(idUser);
    }

    public NodeUser getUserById(Integer userId) {

        log.debug("Rechercher l'utilisateur avec id {}", userId);
        var user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.debug("L'utilisateur avec id {} n'existe pas", userId);
            return null;
        }
        return NodeUser.builder()
                .idUser(user.get().getId())
                .name(user.get().getUsername())
                .mail(user.get().getMail())
                .active(user.get().getActive())
                .alertMail(user.get().getAlertMail())
                .superAdmin(user.get().getIsSuperAdmin())
                .passToModify(user.get().getPassToModify())
                .apiKey(user.get().getApiKey())
                .keyNeverExpire(user.get().getKeyNeverExpire())
                .apiKeyExpireDate(user.get().getKeyExpiresAt())
                .isServiceAccount(user.get().getIsServiceAccount())
                .keyDescription(user.get().getKeyDescription())
                .build();
    }

    public List<NodeUser> searchUserByCriteria(String mail, String username) {
        log.debug("Recherche des utilisateurs avec mail contenant '{}' et nom d'utilisateur contenant '{}'", mail, username);

        // Sécurité minimale contre null
        var safeMail = StringUtils.defaultString(mail);
        var safeUsername = StringUtils.defaultString(username);
        var users = userRepository.searchByMailAndUsername(safeMail, safeUsername);

        log.debug("Nombre d'utilisateurs trouvés : {}", users.size());
        return users.stream().map(user -> {
            NodeUser nodeUser = new NodeUser();
            nodeUser.setIdUser(user.getId());
            nodeUser.setName(user.getUsername());
            nodeUser.setMail(user.getMail());
            nodeUser.setActive(user.getActive());
            nodeUser.setAlertMail(user.getAlertMail());
            nodeUser.setPassToModify(user.getPassToModify());
            nodeUser.setSuperAdmin(user.getIsSuperAdmin());
            nodeUser.setApiKey(user.getApiKey());
            nodeUser.setKeyNeverExpire(user.getKeyNeverExpire());
            nodeUser.setApiKeyExpireDate(user.getKeyExpiresAt());
            nodeUser.setServiceAccount(user.getIsServiceAccount());
            nodeUser.setKeyDescription(user.getKeyDescription());
            return nodeUser;
        }).toList();
    }

    public NodeUser getUser(int idUser) {

        log.debug("Recherche des informations de l'utilisateur avec l'ID : {}", idUser);
        return userRepository.findById(idUser)
                .map(user -> {
                    NodeUser nodeUser = new NodeUser();
                    nodeUser.setIdUser(user.getId());
                    nodeUser.setName(user.getUsername());
                    nodeUser.setActive(user.getActive());
                    nodeUser.setMail(user.getMail());
                    nodeUser.setPassToModify(user.getPassToModify());
                    nodeUser.setAlertMail(user.getAlertMail());
                    nodeUser.setSuperAdmin(user.getIsSuperAdmin());
                    nodeUser.setApiKey(user.getApiKey());
                    nodeUser.setKeyNeverExpire(user.getKeyNeverExpire());
                    nodeUser.setApiKeyExpireDate(user.getKeyExpiresAt());
                    nodeUser.setServiceAccount(user.getIsServiceAccount());
                    nodeUser.setKeyDescription(user.getKeyDescription());
                    log.debug("Utilisateur trouvé : {}", user.getUsername());
                    return nodeUser;
                })
                .orElseGet(() -> {
                    log.warn("Aucun utilisateur trouvé avec l'ID : {}", idUser);
                    return null;
                });
    }

    public User getUserById(int idUser) {
        log.debug("Recherche des informations de l'utilisateur avec l'ID : {}", idUser);
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("Aucun utilisateur avec l'ID : {}", idUser);
            return null;
        }
        return user.get();
    }

    public NodeUserComplet getUserByLoginAndPassword(String login, String password) {

        log.debug("Tentative de connexion pour l'utilisateur : {}", login);
        return userRepository.findByUsernameAndPassword(login, password)
                .map(user -> {
                    NodeUserComplet nodeUser = new NodeUserComplet();
                    nodeUser.setIdUser(user.getId());
                    nodeUser.setName(user.getUsername());
                    nodeUser.setActive(user.getActive());
                    nodeUser.setMail(user.getMail());
                    nodeUser.setAlertMail(user.getAlertMail());
                    nodeUser.setPassToModify(user.getPassToModify());
                    nodeUser.setSuperAdmin(user.getIsSuperAdmin());
                    nodeUser.setApiKey(user.getApiKey());
                    nodeUser.setKeyNeverExpire(user.getKeyNeverExpire());
                    nodeUser.setApiKeyExpireDate(user.getKeyExpiresAt());
                    nodeUser.setServiceAccount(user.getIsServiceAccount());
                    nodeUser.setKeyDescription(user.getKeyDescription());
                    log.debug("Connexion réussie pour l'utilisateur : {}", login);
                    return nodeUser;
                })
                .orElseGet(() -> {
                    log.warn("Échec de la connexion : utilisateur introuvable ou mot de passe incorrect");
                    return null;
                });
    }

    public List<String> getListThesaurusLimitedRoleByUserAsAdmin(int idUser) {

        log.debug("Rechercher les thésaurus ayant des utilisateurs avec des rôles limités");
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", idUser);
            return List.of();
        }

        var role = roleRepository.findById(2);
        if (role.isEmpty()) {
            log.error("Le rôle id 2 n'existe pas");
            return List.of();
        }

        var result = userRoleOnlyOnRepository.findAllByUserAndRoleOrderByThesaurus(user.get(), role.get());
        if (CollectionUtils.isEmpty(result)) {
            log.debug("Aucun thésaurus n'est trouvé !");
            return List.of();
        }

        log.debug("{} thésaurus trouvé pour l'utilisateur id {}", result.size(), idUser);
        return result.stream().map(element -> element.getThesaurus().getIdThesaurus()).toList();
    }

    public User getById(int id) {

        var user = userRepository.findById(id);
        if (user.isEmpty()) {
            log.error("L'utilisateur id {} n'existe pas", id);
            return null;
        }
        return user.get();
    }

    public int getGroupOfThisThesaurus(String idThesaurus) {

        log.debug("Recherche du group id associé au thésaurus id {}", idThesaurus);
        var thesaurus = thesaurusService.getThesaurusById(idThesaurus);
        var groupThesaurus = userGroupThesaurusRepository.findAllByThesaurus(thesaurus);
        return groupThesaurus.map(UserGroupThesaurus::getIdGroup).orElse(-1);
    }

    public Map<String, String> getGroupsOfUser(int userId) {
        log.debug("Recherche des groupes pour l'utilisateur avec ID {}", userId);
        List<UserGroupLabel> groupLabels = userGroupLabelRepository.findAllGroupsOfUser(userId);

        if (groupLabels.isEmpty()) {
            log.warn("Aucun groupe trouvé pour l'utilisateur ID {}", userId);
        } else {
            log.debug("{} groupe(s) trouvé(s) pour l'utilisateur ID {}", groupLabels.size(), userId);
        }

        return groupLabels.stream()
                .collect(Collectors.toMap(g -> String.valueOf(g.getId()), UserGroupLabel::getLabel, (v1, v2) -> v1, LinkedHashMap::new));
    }

    public List<UserGroupLabel> getProjectOfUser(int userId) {
        log.debug("Recherche des projets pour l'utilisateur avec ID {}", userId);
        List<UserGroupLabel> labels = userGroupLabelRepository.findAllGroupsOfUser(userId);
        log.debug("{} projet(s) trouvé(s)", labels.size());
        return labels;
    }

    public List<NodeIdValue> getThesaurusOfProject(int groupId, String lang) {
        log.debug("Chargement des thésaurus pour le projet ID {}, langue = {}", groupId, lang);

        var groups = userGroupThesaurusRepository.findAllByIdGroup(groupId);

        List<NodeIdValue> thesaurusList = new ArrayList<>();
        for (UserGroupThesaurus group : groups) {
            String effectiveLang = Optional.ofNullable(preferenceService.getWorkLanguageOfThesaurus(group.getIdThesaurus())).orElse(lang);
            String title = thesaurusService.getTitleOfThesaurus(group.getIdThesaurus(), effectiveLang);

            NodeIdValue node = new NodeIdValue();
            node.setId(group.getIdThesaurus());
            node.setValue(title);
            node.setStatus(thesaurusService.getThesaurusById(group.getIdThesaurus()).getIsPrivate());
            thesaurusList.add(node);
        }

        log.debug("{} thésaurus chargé(s) pour le projet ID {}", thesaurusList.size(), groupId);
        return thesaurusList;
    }

    public Optional<Integer> getUserGroupId(int userId, String thesoId) {
        log.debug("Recherche de l'identifiant du groupe de l'utilisateur ID {} pour le thésaurus '{}'", userId, thesoId);
        try {
            return userRoleGroupRepository.findGroupIdByUserIdAndThesaurus(userId, thesoId);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du groupe pour l'utilisateur ID {} et thésaurus '{}'", userId, thesoId, e);
            return Optional.empty();
        }
    }

    public List<NodeIdValue> getThesaurusOfProject(int idProject, String idLang, boolean isPrivate) {
        log.debug("Recherche des thésaurus du projet ID={}, visibilité={}, langue={}", idProject, isPrivate, idLang);

        List<String> thesaurusIds = userGroupThesaurusRepository.findThesaurusIdsByGroupAndVisibility(idProject, isPrivate);

        return thesaurusIds.stream().map(idTheso -> {
            String workingLang = preferenceService.getWorkLanguageOfThesaurus(idTheso);
            if (StringUtils.isEmpty(workingLang)) {
                workingLang = idLang;
            }

            String label = thesaurusService.getTitleOfThesaurus(idTheso, workingLang);
            boolean isThesoPrivate = thesaurusService.getThesaurusById(idTheso).getIsPrivate();

            NodeIdValue node = new NodeIdValue();
            node.setId(idTheso);
            node.setValue(label);
            node.setStatus(isThesoPrivate);
            return node;
        }).toList();
    }

    public int getRoleOnThisThesaurus(int idUser, int idGroup, String idTheso) {
        log.debug("Recherche du rôle pour l'utilisateur ID={} sur le thésaurus '{}' et le groupe ID={}", idUser, idTheso, idGroup);

        return userRoleOnlyOnRepository.findRoleFromOnlyOn(idUser, idGroup, idTheso)
                .or(() -> userRoleOnlyOnRepository.findRoleFromGroup(idUser, idGroup, idTheso))
                .orElse(-1);
    }

    public NodeUserRoleGroup getUserRoleOnThisGroup(int idUser, int idGroup) {
        log.debug("Recherche du rôle de l'utilisateur ID={} dans le groupe ID={}", idUser, idGroup);

        return userRoleGroupRepository.findUserRoleOnThisGroup(idUser, idGroup)
                .map(role -> {
                    switch (role.getIdRole()) {
                        case 2 -> role.setAdmin(true);
                        case 3 -> role.setManager(true);
                        case 4 -> role.setContributor(true);
                    }
                    return role;
                })
                .orElse(null);
    }

    public List<NodeUserRole> getUsersRolesByGroup(int idGroup, int idRole) {
        log.debug("Chargement des utilisateurs du groupe ID={} avec rôle >= {}", idGroup, idRole);
        List<NodeUserRole> users = userRoleGroupRepository.findUsersByGroupAndRole(idGroup, idRole);
        log.debug("{} utilisateur(s) récupéré(s) pour le groupe ID={}", users.size(), idGroup);
        return users;
    }

    public List<NodeUserRole> getAllUsersRolesLimitedByTheso(int idGroup) {
        log.debug("Chargement des utilisateurs avec rôles limités par thésaurus dans le groupe ID={}", idGroup);

        var users = userRoleOnlyOnRepository.findAllUsersWithLimitedRoleByGroup(idGroup);

        for (NodeUserRole user : users) {
            String idLang = preferenceService.getWorkLanguageOfThesaurus(user.getIdTheso());
            user.setThesoName(thesaurusService.getTitleOfThesaurus(user.getIdTheso(), idLang));
        }

        log.debug("{} utilisateur(s) trouvé(s) avec des rôles limités par thésaurus dans le groupe ID={}", users.size(), idGroup);
        return users;
    }

    public List<String> getThesaurusOfUser(int idUser) {
        log.debug("Recherche des thésaurus pour l'utilisateur ID={}", idUser);

        List<String> thesaurusIds = userGroupThesaurusRepository.findThesaurusIdsByUserId(idUser);

        log.debug("{} thésaurus trouvé(s) pour l'utilisateur ID={}", thesaurusIds.size(), idUser);
        return thesaurusIds;
    }

    public List<NodeThesoRole> getAllRolesThesosByUserGroup(int idGroup, int idUser) {

        log.debug("Recherche des roles/thesaurus pour l'utilisateur id {} et un group id {}", idUser, idGroup);
        ArrayList<NodeThesoRole> nodeThesoRoles = new ArrayList<>();
        var listThesaurus = getIdThesaurusOfProject(idGroup);
        for (String idThesaurus : listThesaurus) {
            var idRole = getRoleOnThisThesaurus(idUser, idGroup, idThesaurus);
            if (idRole != -1) {
                NodeThesoRole nodeThesoRole = new NodeThesoRole();
                nodeThesoRole.setIdRole(idRole);
                nodeThesoRole.setRoleName(RoleLabels.fromRoleId(idRole));
                nodeThesoRole.setIdTheso(idThesaurus);
                var idLang = preferenceService.getWorkLanguageOfThesaurus(idThesaurus);
                nodeThesoRole.setThesoName(thesaurusService.getTitleOfThesaurus(idThesaurus, idLang));
                nodeThesoRoles.add(nodeThesoRole);
            }
        }
        return nodeThesoRoles;
    }

    private List<String> getIdThesaurusOfProject(int idProject) {

        log.debug("Chargement des thésaurus pour le projet ID={}", idProject);
        List<String> thesaurusIds = userGroupThesaurusRepository.findThesaurusIdsByGroupId(idProject);
        log.debug("{} thésaurus trouvés pour le projet ID={}", thesaurusIds.size(), idProject);
        return thesaurusIds;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> getThesaurusOfUserAsAdmin(int idUser) {
        log.debug("Recherche des thésaurus où l'utilisateur ID={} est administrateur...", idUser);

        List<String> thesaurusIds = userGroupThesaurusRepository.findThesaurusIdsWhereUserIsAdmin(idUser);
        if (thesaurusIds.isEmpty()) {
            log.debug("Aucun thésaurus trouvé pour l'utilisateur ID={} avec un rôle admin.", idUser);
        } else {
            log.debug("{} thésaurus trouvés pour l'utilisateur ID={} avec un rôle admin.", thesaurusIds.size(), idUser);
        }

        return thesaurusIds;
    }

    public boolean updateUserInformation(Integer idUSer, String userName, String password, String email, Boolean alertMail) {
        log.debug("Mise à jour des données utilisateur avec id {}", idUSer);
        var userOpt = userRepository.findById(idUSer);
        if (userOpt.isEmpty()) {
            log.debug("L'utilisateur avec id {} n'existe pas", idUSer);
            return false;
        }

        var user = userOpt.get();

        if (StringUtils.isNotEmpty(userName)) {
            user.setUsername(userName);
        }

        if (StringUtils.isNotEmpty(password)) {
            // Ici, password est déjà encodé en BCrypt
            user.setPassword(password);
        }

        if (StringUtils.isNotEmpty(email)) {
            user.setMail(email);
        }

        if (alertMail != null) {
            user.setAlertMail(alertMail);
        }

        userRepository.save(user);
        return true;
    }

    public User getUserByApiKey(String apiKey) {

        var user = userRepository.findByApiKey(apiKey);
        if (user.isEmpty()) {
            log.debug("Aucun utilisateur n'est trouvé avec l'apiKey {}", apiKey);
            return null;
        }
        return user.get();
    }

    public User getUserByUserName(String userName) {

        var user = userRepository.findByUsername(userName);
        if (user.isEmpty()) {
            log.error("Aucun utilisateur n'est trouvé avec l'userName {}", userName);
            return null;
        }
        return user.get();
    }

    public User findByUsernameAndPassword(String userName, String password) {

       var user = userRepository.findByUsernameAndPassword(userName, MD5Password.getEncodedPassword(password));
       if (user.isEmpty()) {
           log.debug("Aucun utilisateur n'existe avec l'username {} et le password {}", userName, "*****");
           return null;
       }
       return user.get();
    }

    /**
     * Vérifie le mot de passe et migre MD5 vers BCrypt si nécessaire
     */
    public User findByUsernameAndPasswordMigrated(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return null;

        User user = userOpt.get();
        String storedHash = user.getPassword();
        boolean matches = false;
        if(storedHash == null) {
            return null;
        }

        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")) {
            // mot de passe déjà en BCrypt
            matches = passwordEncoder.matches(rawPassword, storedHash);
        } else {
            // mot de passe MD5
            matches = MD5Password.getEncodedPassword(rawPassword).equals(storedHash);

            // Migration vers BCrypt
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }
        }

        return matches ? user : null;
    }


    public List<User> getUserByUserNameLike(String userName) {

        var userList = userRepository.findAllByUsernameLikeIgnoreCase(userName);
        if (CollectionUtils.isEmpty(userList)) {
            log.debug("Aucun utilisateur n'est trouvé avec l'userName {}", userName);
            return null;
        }
        return userList;
    }

    public List<String> getSelectedThesaurus(Integer selectedProject, Integer idUser) {

        log.debug("Recherche des thésaurus rattachés au projet id {} et à l'utilisateur id {}", idUser, selectedProject);
        var result = new ArrayList<String>();
        var nodeUserRoles = userRoleOnlyOnRepository.getListRoleByThesaurusLimited(selectedProject, idUser);
        if (CollectionUtils.isEmpty(nodeUserRoles)) {
            return result;
        }

        for (NodeUserRole nodeUserRole1 : nodeUserRoles) {
            result.add(nodeUserRole1.getIdTheso());
        }
        return result;
    }

    public List<NodeUserRole> getLimitedThesaurusForUser(int idUser, String idProject, NodeUserRole nodeUserRole, String workLanguage) {

        var idGroup = Integer.parseInt(idProject);
        List<NodeUserRole> limitedThesaurusRoleForUser = ObjectUtils.isNotEmpty(nodeUserRole)
                ? userRoleOnlyOnRepository.getListRoleByThesaurusLimited(idGroup, idUser)
                : new ArrayList<>();

        var idThesaurusTemp = limitedThesaurusRoleForUser.stream().map(NodeUserRole::getIdTheso).toList();
        var allThesaurusOfProject = thesaurusService.getThesaurusOfProject(idGroup, workLanguage);

        limitedThesaurusRoleForUser.addAll(allThesaurusOfProject.stream()
                .filter(element -> !idThesaurusTemp.contains(element.getId()))
                .map(element -> NodeUserRole.builder()
                        .idTheso(element.getId())
                        .thesoName(element.getValue())
                        .idRole(-1)
                        .roleName("")
                        .build())
                .toList());

        return limitedThesaurusRoleForUser;
    }

    public List<NodeUserGroupUser> getAllUserGroup() {

        log.debug("Recherche de tous les groups utilisateurs existant");
        List<NodeUserGroupUser> nodeUserGroupUsers = userRoleGroupRepository.getAllGroupUser();
        nodeUserGroupUsers.addAll(userRepository.getAllGroupUserWithoutGroup());
        nodeUserGroupUsers.addAll(userRepository.getAllUsersSuperadmin());
        return nodeUserGroupUsers;
    }

    @Transactional
    public void deleteUserById(int idUser) {

        log.debug("Supprimer l'utilisateur id {}", idUser);
        passwordResetTokenRepository.deleteByUserId(idUser);
        userRepository.deleteById(idUser);
    }

    public User getUserByMail(String mail) {

        log.debug("Recherche de l'utilisateur par mail {}", mail);
        var user = userRepository.findByMail(mail);
        if (user.isEmpty()) {
            log.debug("Aucun utilisateur n'existe avec le mail {}", mail);
            return null;
        }
        return user.get();
    }

    public User saveUser(User user) {
        log.debug("Ajout du nouveau utilisateur");
        return userRepository.save(user);
    }

    public List<UserRoleOnlyOn> getAllThesaurusByUsers(int idUser) {

        var user = getById(idUser);
        return userRoleOnlyOnRepository.findAllByUserOrderByThesaurus(user);
    }
}
