package fr.cnrs.opentheso.shared.mail;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMailSender {

    private final JavaMailSender mailSender;

    @Value("${smpt.mailFrom}")
    private String mailFrom;

    public boolean sendHtmlMail(String recipient, String subject, String htmlBody) {
        return sendHtmlMail(recipient, null, subject, htmlBody);
    }

    public boolean sendHtmlMail(String recipient, String replyTo, String subject, String htmlBody) {
        if (ObjectUtils.isEmpty(mailFrom) || ObjectUtils.isEmpty(recipient)) {
            log.warn("Envoi de mail ignoré : expéditeur ou destinataire manquant");
            if (FacesContext.getCurrentInstance() != null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_FATAL, "",
                        "Adresse de l'expéditeur ou du destinataire manquante"));
            }
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(recipient);
            if (StringUtils.isNotBlank(replyTo)) {
                helper.setReplyTo(replyTo.trim());
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Erreur pendant l'envoi du mail vers {}", recipient, ex);
            if (FacesContext.getCurrentInstance() != null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_FATAL, "", "Erreur pendant l'envoi du mail"));
            }
            return false;
        }
    }
}
