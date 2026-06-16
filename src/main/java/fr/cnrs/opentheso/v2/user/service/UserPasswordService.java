package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.user.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPasswordService {

    private final UserLookupService userLookupService;
    private final UserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(int userId, String password, String confirmation) {
        PasswordPolicy.validate(password, confirmation);

        UserEntity user = userLookupService.requireEntity(userId);
        user.setPassword(passwordEncoder.encode(password));
        userProfileService.saveEntity(user);
        log.info("Mot de passe mis à jour pour l'utilisateur id={}", userId);
    }
}
