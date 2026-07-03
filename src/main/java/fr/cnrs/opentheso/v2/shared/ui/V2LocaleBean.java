package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.models.candidats.LanguageEnum;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internationalisation v2 — indépendant de {@code LanguageBean} legacy.
 */
@Getter
@Setter
@Component("v2LocaleBean")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class V2LocaleBean implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private String idLangue;
    private String currentBundle;

    @PostConstruct
    public void init() {
        currentBundle = "langue_" + workLanguage;
        idLangue = workLanguage.toUpperCase();
    }

    public void changeLangue(String languageCode) {
        currentBundle = "langue_" + languageCode;
        idLangue = languageCode.toUpperCase();

        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.getViewRoot().setLocale(new Locale(languageCode));
        facesContext.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "",
                LanguageEnum.valueOf(languageCode.toUpperCase()).getLanguage() + " !"));

        PrimeFaces.current().ajax().update("messageIndex");
        PrimeFaces.current().ajax().update("containerIndex");
        PrimeFaces.current().ajax().update("menuBar");
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    public String getIdLangue() {
        return idLangue.toLowerCase();
    }

    public String getMsg(String key) {
        FacesContext context = FacesContext.getCurrentInstance();
        ResourceBundle bundle = context.getApplication().getResourceBundle(context, currentBundle);
        return bundle.getString(key);
    }
}
