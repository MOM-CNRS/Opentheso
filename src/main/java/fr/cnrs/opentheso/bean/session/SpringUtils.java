package fr.cnrs.opentheso.bean.session;

import org.springframework.context.ApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import jakarta.faces.context.FacesContext;

public class SpringUtils {

    public static <T> T getBean(Class<T> beanClass) {
        ApplicationContext ctx =
                FacesContextUtils.getWebApplicationContext(
                        FacesContext.getCurrentInstance()
                );
        return ctx.getBean(beanClass);
    }
}
