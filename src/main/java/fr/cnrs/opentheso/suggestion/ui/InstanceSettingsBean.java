package fr.cnrs.opentheso.suggestion.ui;

import fr.cnrs.opentheso.admin.ui.InstanceAdminBean;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.suggestion.model.InstanceFeatures;
import fr.cnrs.opentheso.suggestion.service.FeatureSuggestionMailer;
import fr.cnrs.opentheso.suggestion.service.InstanceFeatureService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;

@Slf4j
@Setter
@ViewScoped
@Named("instanceSettingsBean")
@RequiredArgsConstructor
public class InstanceSettingsBean implements Serializable {

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient InstanceFeatureService instanceFeatureService;
    private final transient FeatureSuggestionMailer featureSuggestionMailer;

    private boolean suggestionEnabled = true;
    private boolean languageVisible = true;
    private boolean restApiVisible = true;
    private boolean graphApiVisible = true;
    private boolean loaded;
    private String message;
    private boolean messageError;

    public boolean isSuggestionEnabled() {
        ensureLoaded();
        return suggestionEnabled;
    }

    public boolean isLanguageVisible() {
        ensureLoaded();
        return languageVisible;
    }

    public boolean isRestApiVisible() {
        ensureLoaded();
        return restApiVisible;
    }

    public boolean isGraphApiVisible() {
        ensureLoaded();
        return graphApiVisible;
    }

    public boolean isMailConfigured() {
        return featureSuggestionMailer.isConfigured();
    }

    public String getMessage() {
        return message;
    }

    public boolean isMessageError() {
        return messageError;
    }

    public void save() {
        message = null;
        if (userSession == null || !userSession.canAccessSuperAdminScreen()) {
            messageError = true;
            message = localeBean.getMsg("admin.prefs.denied");
            return;
        }
        try {
            instanceFeatureService.save(new InstanceFeatures(
                    suggestionEnabled, languageVisible, restApiVisible, graphApiVisible));
            loaded = true;
            messageError = false;
            message = localeBean.getMsg("admin.prefs.saved");
            redirectHome(message);
        } catch (RuntimeException ex) {
            log.warn("Enregistrement des paramètres d'instance impossible", ex);
            messageError = true;
            message = localeBean.getMsg("admin.prefs.error");
        }
    }

    private void redirectHome(String text) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        String home = context.getExternalContext().getRequestContextPath() + "/admin/instance";
        context.getExternalContext().getSessionMap().put(InstanceAdminBean.SESSION_PREFS_FLASH, text);
        try {
            context.getExternalContext().redirect(home);
            context.responseComplete();
        } catch (IOException ex) {
            log.warn("Retour vers l'administration de l'instance impossible", ex);
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        InstanceFeatures features = instanceFeatureService.current();
        suggestionEnabled = features.suggestionEnabled();
        languageVisible = features.languageVisible();
        restApiVisible = features.restApiVisible();
        graphApiVisible = features.graphApiVisible();
        loaded = true;
    }
}
