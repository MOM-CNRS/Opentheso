package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.v2.shared.repository.PasswordResetCommandRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountPasswordResetService {

    private static final int TOKEN_EXPIRATION_MINUTES = 2880;

    private final UserAuthQueryRepository userAuthQueryRepository;
    private final PasswordResetCommandRepository passwordResetCommandRepository;
    private final SystemMailSender systemMailSender;
    private final ApplicationUriService applicationUriService;

    @Transactional
    public void requestPasswordReset(String email, boolean activation) {
        userAuthQueryRepository.findByMail(email).ifPresent(credential -> {
            passwordResetCommandRepository.invalidateActiveTokens(credential.userId());

            String token = UUID.randomUUID().toString().replace("-", "");
            passwordResetCommandRepository.insertToken(
                    credential.userId(),
                    token,
                    V2Dates.nowDateTime().plusMinutes(TOKEN_EXPIRATION_MINUTES)
            );

            String resetLink = applicationUriService.resolveApplicationBaseUrl()
                    + "/reset-password.xhtml?token=" + token;

            if (activation) {
                systemMailSender.sendHtmlMail(
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
                return;
            }

            systemMailSender.sendHtmlMail(
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
        });
    }
}
