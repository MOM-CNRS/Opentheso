package fr.cnrs.opentheso.v2.shared.ui;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Internationalisation v2 — indépendant de {@code LanguageBean} legacy.
 */
@Getter
@Setter
@Component("v2LocaleBean")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class V2LocaleBean implements Serializable {

    private static final Set<String> SUPPORTED = Set.of("fr", "en", "de", "es", "ar");

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private String idLangue;
    private String currentBundle;
    private String pendingLang;

    @PostConstruct
    public void init() {
        applyCode(normalize(workLanguage));
    }

    public void applyToView() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getViewRoot() == null) {
            return;
        }
        facesContext.getViewRoot().setLocale(Locale.forLanguageTag(getIdLangue()));
    }

    public void applyPendingLang() {
        changeLangue(pendingLang);
    }

    public void changeLangue(String languageCode) {
        String code = normalize(languageCode);
        if (code == null) {
            return;
        }
        applyCode(code);
        applyToView();
    }

    public String getIdLangue() {
        return idLangue == null ? "fr" : idLangue.toLowerCase(Locale.ROOT);
    }

    public boolean currentLangIs(String languageCode) {
        return getIdLangue().equals(normalize(languageCode));
    }

    public String getFlagEmoji() {
        return flagEmoji(getIdLangue());
    }

    public String flagEmoji(String languageCode) {
        String code = normalize(languageCode);
        if (code == null) {
            code = "fr";
        }
        return switch (code) {
            case "en" -> "🇬🇧";
            case "de" -> "🇩🇪";
            case "es" -> "🇪🇸";
            case "ar" -> "🇸🇦";
            default -> "🇫🇷";
        };
    }

    public String getMsg(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context == null) {
                return key;
            }
            ResourceBundle bundle = context.getApplication().getResourceBundle(context, currentBundle);
            if (bundle == null || !bundle.containsKey(key)) {
                return key;
            }
            return bundle.getString(key);
        } catch (RuntimeException e) {
            return key;
        }
    }

    private void applyCode(String code) {
        String safe = code == null ? "fr" : code;
        currentBundle = "langue_" + safe;
        idLangue = safe.toUpperCase(Locale.ROOT);
        pendingLang = safe;
    }

    private static String normalize(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return null;
        }
        String code = languageCode.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED.contains(code) ? code : null;
    }
}
