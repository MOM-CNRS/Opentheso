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
 * Texte de la page affichée quand un identifiant ARK ne correspond à rien.
 */
@Named("arkMissingBean")
@RequestScoped
@RequiredArgsConstructor
public class ArkMissingBean implements Serializable {

    private final transient LocaleBean localeBean;

    @Getter
    @Setter
    private String ark;

    public String getDetail() {
        String value = displayedArk(ark);
        if (value == null) {
            value = displayedArk(requestArk());
        }
        if (value == null) {
            return localeBean.getMsg("ark.missing.sub.empty");
        }
        return localeBean.getMsg("ark.missing.sub", value);
    }

    private static String requestArk() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        return facesContext.getExternalContext().getRequestParameterMap().get("ark");
    }

    static String displayedArk(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 200) {
            return null;
        }
        if (!trimmed.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,80}/[A-Za-z0-9][A-Za-z0-9_.:-]{0,120}")) {
            return null;
        }
        return trimmed;
    }
}
