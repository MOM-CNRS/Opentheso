package fr.cnrs.opentheso.bean.menu.connect;

import java.io.Serializable;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Data
@Slf4j
@Configuration
public class Connect implements Serializable{

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Value("${info.application.version:dev}")
    private String buildVersion;

    //Retourne la version actuelle d'Opentheso d'après le WAR
    public String getOpenthesoVersion() {
        return buildVersion;
    }

    public String getLocalUri() {
        var facesContext = FacesContext.getCurrentInstance();
        var request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        var protocol = request.isSecure() ? "https://" : "http://";
        var host = request.getHeader("host");
        var contextPath = request.getContextPath();

        return protocol + host + contextPath + "/";
    }

    public String status(){
        return "OK! Connected";
    }

}
