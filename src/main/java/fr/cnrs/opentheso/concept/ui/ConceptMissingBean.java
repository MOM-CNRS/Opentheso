package fr.cnrs.opentheso.concept.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Texte de la page affichée quand le couple thésaurus / concept n'existe pas.
 */
@Named("conceptMissingBean")
@RequestScoped
@RequiredArgsConstructor
public class ConceptMissingBean implements Serializable {

    private final transient LocaleBean localeBean;

    @Getter
    @Setter
    private String theso;

    @Getter
    @Setter
    private String concept;

    public String getDetail() {
        String thesaurusId = displayedId(first(theso, requestValue("theso")));
        String conceptId = displayedId(first(concept, requestValue("concept")));
        if (thesaurusId == null || conceptId == null) {
            return localeBean.getMsg("concept.missing.sub.empty");
        }
        return localeBean.getMsg("concept.missing.sub", thesaurusId, conceptId);
    }

    static String displayedId(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 80) {
            return null;
        }
        if (!trimmed.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,79}")) {
            return null;
        }
        return trimmed;
    }

    private static String first(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private static String requestValue(String name) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        return facesContext.getExternalContext().getRequestParameterMap().get(name);
    }
}
