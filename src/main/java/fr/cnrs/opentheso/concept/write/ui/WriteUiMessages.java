package fr.cnrs.opentheso.concept.write.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * Messages d'écriture V2 : bundle i18n, repli français si le contexte JSF manque (tests).
 */
public final class WriteUiMessages {

    public static final String UNAUTHORIZED_KEY = "write.unauthorized";
    public static final String UNAUTHORIZED_FALLBACK = "Action non autorisée";

    private WriteUiMessages() {
    }

    public static String msg(LocaleBean locale, String key, String fallback) {
        if (locale != null) {
            String value = locale.getMsg(key);
            if (StringUtils.isNotBlank(value) && !value.equals(key)) {
                return value;
            }
        }
        return fallback;
    }

    public static String msg(LocaleBean locale, String key, String fallback, Object... args) {
        return MessageFormat.format(msg(locale, key, fallback), args);
    }

    public static String unauthorized(LocaleBean locale) {
        return msg(locale, UNAUTHORIZED_KEY, UNAUTHORIZED_FALLBACK);
    }
}
