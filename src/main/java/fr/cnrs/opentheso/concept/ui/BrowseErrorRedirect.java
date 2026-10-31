package fr.cnrs.opentheso.concept.ui;

import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialViewContext;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirige une navigation navigateur vers une page d'erreur, sans corps de fiche.
 */
final class BrowseErrorRedirect {

    private BrowseErrorRedirect() {
    }

    static boolean alreadyHandled() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext != null && facesContext.getResponseComplete();
    }

    static void toPrivateThesaurus() {
        send("/acces-refuse.xhtml");
    }

    static void toConceptMissing(String thesaurusId, String conceptId) {
        send("/concept-introuvable.xhtml?theso="
                + encode(thesaurusId)
                + "&concept="
                + encode(conceptId));
    }

    private static void send(String path) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        PartialViewContext partial = facesContext.getPartialViewContext();
        if (partial != null && partial.isAjaxRequest()) {
            return;
        }
        try {
            String context = facesContext.getExternalContext().getRequestContextPath();
            if (context == null || "/".equals(context)) {
                context = "";
            }
            facesContext.getExternalContext().redirect(context + path);
            facesContext.responseComplete();
        } catch (IOException | IllegalStateException ignored) {
            // La page courante reste le repli si la redirection échoue.
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
