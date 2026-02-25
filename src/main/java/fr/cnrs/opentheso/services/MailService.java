package fr.cnrs.opentheso.services;

import java.io.Serializable;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Data
@Service
public class MailService implements Serializable {

    // Paramètres SMTP récupérés depuis ton YAML
    @Value("${smpt.mailFrom}")
    private String mailFrom;

    @Value("${smpt.hostname}")
    private String hostname;

    @Value("${smpt.portNumber}")
    private String portNumber;

    @Value("${smpt.protocol}")
    private String protocolMail;

    @Value("${smpt.authorization}")
    private String authorization;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envoie un mail HTML.
     *
     * @param sendTo  destinataire
     * @param subject sujet
     * @param message contenu HTML
     * @return true si l'envoi réussit, false sinon
     */
    public boolean sendMail(String sendTo, String subject, String messageContent) {
        if (ObjectUtils.isEmpty(mailFrom) || ObjectUtils.isEmpty(sendTo)) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_FATAL, "",
                    "Adresse de l'expéditeur ou du destinataire manquante");
            FacesContext.getCurrentInstance().addMessage(null, fm);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Expéditeur et destinataire
            helper.setFrom(mailFrom);
            helper.setTo(sendTo);

            // Sujet et contenu HTML
            helper.setSubject(subject);
            helper.setText(messageContent, true);

            // Envoi
            mailSender.send(message);
            return true;

        } catch (MessagingException e) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_FATAL, "", e.toString());
            FacesContext.getCurrentInstance().addMessage(null, fm);
            return false;
        }
    }
}