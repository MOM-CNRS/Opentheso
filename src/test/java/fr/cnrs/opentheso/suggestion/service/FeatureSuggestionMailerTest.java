package fr.cnrs.opentheso.suggestion.service;

import fr.cnrs.opentheso.shared.mail.SystemMailSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeatureSuggestionMailerTest {

    @Test
    void blankDestinationDoesNotSend() {
        SystemMailSender sender = mock(SystemMailSender.class);
        FeatureSuggestionMailer mailer = new FeatureSuggestionMailer(sender, "  ", Runnable::run);

        assertFalse(mailer.isConfigured());
        assertFalse(mailer.dispatch("Sujet", "<p>Bonjour</p>", "ada@example.fr"));
        verifyNoInteractions(sender);
    }

    @Test
    void configuredDestinationSendsWithReplyTo() {
        SystemMailSender sender = mock(SystemMailSender.class);
        when(sender.sendHtmlMail("desk@example.org", "ada@example.fr", "Sujet", "<p>Bonjour</p>")).thenReturn(true);
        FeatureSuggestionMailer mailer = new FeatureSuggestionMailer(sender, " desk@example.org ", Runnable::run);

        assertTrue(mailer.dispatch("Sujet", "<p>Bonjour</p>", "ada@example.fr"));
        verify(sender).sendHtmlMail("desk@example.org", "ada@example.fr", "Sujet", "<p>Bonjour</p>");
    }

    @Test
    void emptyBodyDoesNotSend() {
        SystemMailSender sender = mock(SystemMailSender.class);
        FeatureSuggestionMailer mailer = new FeatureSuggestionMailer(sender, "desk@example.org", Runnable::run);

        assertFalse(mailer.dispatch("Sujet", "  ", "ada@example.fr"));
        verify(sender, never()).sendHtmlMail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
