package fr.cnrs.opentheso.user.service;

import fr.cnrs.opentheso.entites.PasswordResetToken;
import fr.cnrs.opentheso.repositories.PasswordResetTokenRepository;
import fr.cnrs.opentheso.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.shared.repository.PasswordResetCommandRepository;
import fr.cnrs.opentheso.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.shared.web.ApplicationUriService;
import fr.cnrs.opentheso.user.exception.PasswordResetException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountPasswordResetService {

    private static final int TOKEN_EXPIRATION_MINUTES = 2880;

    private final UserAuthQueryRepository userAuthQueryRepository;
    private final PasswordResetCommandRepository passwordResetCommandRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserPasswordService userPasswordService;
    private final UserCommandRepository userCommandRepository;
    private final SystemMailSender systemMailSender;
    private final ApplicationUriService applicationUriService;

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return findUsableToken(token).isPresent();
    }

    @Transactional
    public void resetPassword(String token, String password, String confirmation) {
        PasswordResetToken row = findUsableToken(token)
                .orElseThrow(() -> new PasswordResetException("Lien invalide ou expiré."));
        int userId = row.getUser().getId();
        userPasswordService.changePassword(userId, password, confirmation);
        userCommandRepository.activatePendingPassword(userId);
        passwordResetCommandRepository.markUsed(row.getToken());
    }

    /**
     * @return {@code false} seulement si le compte existe et que l'envoi du mail a échoué
     */
    @Transactional
    public boolean requestPasswordReset(String email, boolean activation) {
        var found = userAuthQueryRepository.findByMail(email);
        if (found.isEmpty()) {
            return true;
        }
        var credential = found.get();
        passwordResetCommandRepository.invalidateActiveTokens(credential.userId());

        String token = UUID.randomUUID().toString().replace("-", "");
        passwordResetCommandRepository.insertToken(
                credential.userId(),
                token,
                Dates.nowDateTime().plusMinutes(TOKEN_EXPIRATION_MINUTES)
        );

        String resetLink = applicationUriService.resolveApplicationBaseUrl()
                + "/reset-password.xhtml?token=" + token;

        if (activation) {
            return systemMailSender.sendHtmlMail(
                    credential.mail(),
                    "Activation de votre compte Opentheso",
                    String.format(
                            "Bonjour,<br/><br/>" +
                                    "Un compte \"%s\" a été créé pour vous sur <strong>Opentheso</strong> \"%s\".<br/>" +
                                    "Pour activer votre compte et définir votre mot de passe, cliquez sur le lien ci-dessous (valable %d minutes) :<br/><br/>" +
                                    "<a href=\"%s\">Activer mon compte</a><br/><br/>" +
                                    "Si vous n’êtes pas à l’origine de cette demande, vous pouvez ignorer ce mail.<br/><br/>" +
                                    "Cordialement,<br/>L'équipe Opentheso",
                            credential.username(),
                            applicationUriService.resolveApplicationBaseUrl(),
                            TOKEN_EXPIRATION_MINUTES,
                            resetLink
                    )
            );
        }

        return systemMailSender.sendHtmlMail(
                credential.mail(),
                "Réinitialisation de votre mot de passe Opentheso",
                String.format(
                        "Bonjour,<br><br>" +
                                "Vous avez fait (\"%s\") une demande de réinitialisation de votre mot de passe sur Opentheso.<br>" +
                                "Pour définir un nouveau mot de passe, cliquez sur le lien suivant : " +
                                "<a href=\"%s\">Réinitialiser mon mot de passe</a><br>" +
                                "Ce lien est valable %d minutes.<br><br>" +
                                "Si vous n’êtes pas à l’origine de cette demande, merci d’ignorer ce message.<br>" +
                                "Celui-ci a été généré automatiquement, merci de ne pas y répondre.",
                        credential.username(),
                        resetLink,
                        TOKEN_EXPIRATION_MINUTES
                )
        );
    }

    private Optional<PasswordResetToken> findUsableToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        return passwordResetTokenRepository.findByToken(token.trim())
                .filter(row -> row.getUser() != null && row.getUser().getId() != null)
                .filter(row -> !Boolean.TRUE.equals(row.getUsed()))
                .filter(row -> row.getExpiresAt() != null && row.getExpiresAt().isAfter(Dates.nowDateTime()));
    }
}
