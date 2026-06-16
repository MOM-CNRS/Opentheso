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
        registry.addViewController("/profile").setViewName("forward:/profile/myAccount.xhtml");
        registry.addViewController("/v2/profile").setViewName("forward:/v2/user/my-account.xhtml");
        registry.addViewController("/toolbox/edition").setViewName("forward:/toolbox/edition.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

}
