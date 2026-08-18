package fr.cnrs.opentheso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.xhtml");
        registry.addViewController("/reset-password").setViewName("forward:/reset-password.xhtml");
        registry.addViewController("/profile").setViewName("forward:/profile/myAccount.xhtml");
        registry.addViewController("/candidat").setViewName("forward:/candidat/candidat.xhtml");
        registry.addViewController("/toolbox/edition").setViewName("forward:/toolbox/edition.xhtml");
        registry.addViewController("/v2").setViewName("forward:/v2/index.xhtml");
        registry.addViewController("/v2-preview").setViewName("forward:/v2/index.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
