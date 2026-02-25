package fr.cnrs.opentheso.bean.profile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import fr.cnrs.opentheso.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;

@Named("appConfigBean")
@ApplicationScoped
public class AppConfigBean {

    @Autowired
    private AppConfig appConfig;

    public boolean isKeycloakEnabled() {
        return appConfig.isKeycloakEnabled();
    }
}
