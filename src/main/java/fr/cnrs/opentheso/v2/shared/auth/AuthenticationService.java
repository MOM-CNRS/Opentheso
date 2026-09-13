package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.v2.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserCredentialRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserAuthQueryRepository userAuthQueryRepository;
    private final UserCommandRepository userCommandRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Optional<AuthenticatedUser> authenticate(String usernameOrEmail, String password) {
        if (StringUtils.isAnyBlank(usernameOrEmail, password)) {
            return Optional.empty();
        }
        String credential = usernameOrEmail.trim();
        return userAuthQueryRepository.findByUsername(credential)
                .or(() -> userAuthQueryRepository.findByMail(credential))
                .flatMap(row -> verifyPassword(row, password));
    }

    private Optional<AuthenticatedUser> verifyPassword(UserCredentialRow row, String rawPassword) {
        String storedHash = row.passwordHash();
        if (storedHash == null) {
            return Optional.empty();
        }

        boolean matches;
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")) {
            matches = passwordEncoder.matches(rawPassword, storedHash);
        } else {
            matches = MD5Password.getEncodedPassword(rawPassword).equals(storedHash);
            if (matches) {
                userCommandRepository.updatePassword(row.userId(), passwordEncoder.encode(rawPassword));
            }
        }

        return matches
                ? Optional.of(touchLastLogin(new AuthenticatedUser(row.userId(), row.username())))
                : Optional.empty();
    }

    private AuthenticatedUser touchLastLogin(AuthenticatedUser user) {
        try {
            userCommandRepository.updateLastLogin(user.id());
        } catch (RuntimeException ex) {
            log.warn("Connexion ok, mais last_login n'a pas pu être mis à jour pour id={}", user.id(), ex);
        }
        return user;
    }
}
