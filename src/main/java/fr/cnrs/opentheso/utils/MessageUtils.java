package fr.cnrs.opentheso.utils;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

public class MessageUtils {


    public static void showMessage(FacesMessage.Severity messageType, String titre, String message) {

        FacesMessage msg = new FacesMessage(messageType, titre, message);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null, msg);
        }
        try {
            PrimeFaces.current().ajax().update("messageIndex");
        } catch (Exception ignored) {
            // Hors contexte JSF/PrimeFaces (ex. appel API).
        }
    }

    public static void showWarnMessage(String message) {
        showMessage(FacesMessage.SEVERITY_WARN, "Attention", message);
    }

    public static void showInformationMessage(String message) {
        showMessage(FacesMessage.SEVERITY_INFO, "Information", message);
    }

    public static void showErrorMessage(String message) {
        showMessage(FacesMessage.SEVERITY_ERROR, "Erreur", message);
    }
}
