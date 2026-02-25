package fr.cnrs.opentheso.bean.session;

import jakarta.faces.context.FacesContext;

public class FacesUtils {
    public static String getRequestParam(String name) {
        return FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .get(name);
    }
}
