package fr.cnrs.opentheso.v2.setting.ui;

import jakarta.faces.context.FacesContext;
import org.apache.commons.lang3.StringUtils;

final class RequestParams {

    private RequestParams() {
    }

    static String param(String name) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || StringUtils.isBlank(name)) {
            return null;
        }
        return context.getExternalContext().getRequestParameterMap().get(name);
    }

    static int intParam(String name) {
        return parseInt(param(name));
    }

    static int parsePositiveInt(String raw) {
        int value = parseInt(raw);
        return value > 0 ? value : 0;
    }

    static int parseInt(String raw) {
        if (StringUtils.isBlank(raw)) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
