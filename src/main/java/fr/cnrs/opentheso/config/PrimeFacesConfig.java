package fr.cnrs.opentheso.config;

import com.sun.faces.config.ConfigureListener;
import jakarta.faces.lifecycle.Lifecycle;
import jakarta.faces.lifecycle.LifecycleFactory;
import jakarta.servlet.ServletContext;
import jakarta.faces.webapp.FacesServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.context.ServletContextAware;


@Configuration
public class PrimeFacesConfig implements ServletContextAware {

    @Override
    public void setServletContext(ServletContext servletContext) {
        servletContext.setInitParameter("com.sun.faces.forceLoadConfiguration", Boolean.TRUE.toString());
        servletContext.setInitParameter("javax.faces.FACELETS_SKIP_COMMENTS", Boolean.TRUE.toString());

        servletContext.setInitParameter("facelets.DEVELOPMENT", Boolean.TRUE.toString());

        servletContext.setInitParameter("javax.faces.DEFAULT_SUFFIX", ".xhtml");
        servletContext.setInitParameter("javax.faces.PROJECT_STAGE", "Development");
        servletContext.setInitParameter("javax.faces.FACELETS_REFRESH_PERIOD", "1");

        servletContext.setInitParameter("primefaces.CLIENT_SIDE_VALIDATION", Boolean.TRUE.toString());
        servletContext.setInitParameter("primefaces.THEME", "saga");
    }

    @Bean
    public ServletRegistrationBean<FacesServlet> facesServletRegistration() {
        ServletRegistrationBean<FacesServlet> registrationBean = new ServletRegistrationBean<>(new FacesServlet(), "*.xhtml");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    @Bean
    public ServletListenerRegistrationBean<ConfigureListener> jsfConfigureListener() {
        return new ServletListenerRegistrationBean<>(new ConfigureListener());
    }

    // permet de gérer le multilingue
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:language/site"); // Base du chemin pour les fichiers de propriétés
        messageSource.setDefaultEncoding("UTF-8"); // Support des caractères spéciaux
        messageSource.setCacheSeconds(3600); // Cache les fichiers pendant 1 heure
        return messageSource;
    }

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            // Activation de la protection CSRF de PrimeFaces
            servletContext.setInitParameter("primefaces.CSRF", "true");
        };
    }
}
