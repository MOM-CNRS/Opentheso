package fr.cnrs.opentheso.suggestion.service;

import fr.cnrs.opentheso.shared.mail.SystemMailSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Envoie une proposition d'évolution sans bloquer la requête JSF.
 * L'adresse de destination vient de {@code opentheso.suggestion.mail-to}.
 */
@Slf4j
@Component
public class FeatureSuggestionMailer {

    private final SystemMailSender mailSender;
    private final String mailTo;
    private final Executor executor;

    @Autowired
    public FeatureSuggestionMailer(
            SystemMailSender mailSender,
            @Value("${opentheso.suggestion.mail-to:}") String mailTo) {
        this(mailSender, mailTo, FeatureSuggestionMailer::startDaemon);
    }

    FeatureSuggestionMailer(SystemMailSender mailSender, String mailTo, Executor executor) {
        this.mailSender = mailSender;
        this.mailTo = StringUtils.trimToEmpty(mailTo);
        this.executor = executor;
    }

    public boolean isConfigured() {
        return !mailTo.isBlank();
    }

    public boolean dispatch(String subject, String html, String replyTo) {
        if (!isConfigured() || StringUtils.isBlank(subject) || StringUtils.isBlank(html)) {
            return false;
        }
        String recipient = mailTo;
        String reply = StringUtils.trimToNull(replyTo);
        executor.execute(() -> {
            try {
                boolean sent = mailSender.sendHtmlMail(recipient, reply, subject, html);
                if (!sent) {
                    log.warn("La proposition d'évolution n'a pas été remise au relais de messagerie");
                }
            } catch (RuntimeException ex) {
                log.warn("Envoi de la proposition d'évolution interrompu", ex);
            }
        });
        return true;
    }

    private static void startDaemon(Runnable task) {
        Thread thread = new Thread(task, "feature-suggestion-mail");
        thread.setDaemon(true);
        thread.start();
    }
}
