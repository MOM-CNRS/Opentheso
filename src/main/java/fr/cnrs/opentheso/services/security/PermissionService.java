package fr.cnrs.opentheso.services.security;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.repositories.UserRoleGroupRepository;
import fr.cnrs.opentheso.repositories.UserRoleOnlyOnRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserRightsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service central de résolution des droits utilisateur.
 * Ordre de priorité : SuperAdmin (global) > rôle only_on (exception sur un thésaurus précis)
 * > rôle de groupe (hérité du projet auquel appartient le thésaurus).
 */
@Service
public class PermissionService {

    private final UserRepository userRepo;
    private final UserRoleGroupRepository roleGroupRepo;
    private final UserRoleOnlyOnRepository roleOnlyOnRepo;
    private final UserGroupThesaurusRepository groupThesoRepo;
    private final UserRightsRepository userRightsRepo;

    @Autowired
    public PermissionService(UserRepository userRepo,
                             UserRoleGroupRepository roleGroupRepo,
                             UserRoleOnlyOnRepository roleOnlyOnRepo,
                             UserGroupThesaurusRepository groupThesoRepo,
                             UserRightsRepository userRightsRepo) {
        this.userRepo = userRepo;
        this.roleGroupRepo = roleGroupRepo;
        this.roleOnlyOnRepo = roleOnlyOnRepo;
        this.groupThesoRepo = groupThesoRepo;
        this.userRightsRepo = userRightsRepo;
    }

    public Integer findRoleIdOnTheso(int idUser, String idTheso) {
        return userRightsRepo.findRoleIdOnTheso(idUser, idTheso);
    }

    /**
     * Résout le RoleType effectif d'un utilisateur sur un thésaurus.
     * Retourne Optional.empty() si l'utilisateur n'a aucun droit sur ce thésaurus.
     */
    public Optional<RoleType> getEffectiveRole(int idUser, String idTheso) {
        Integer idRole = findRoleIdOnTheso(idUser, idTheso);
        if (idRole == null) {
            return Optional.empty();
        }
        return Optional.of(RoleType.fromId(idRole));
    }
}