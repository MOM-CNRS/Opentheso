package fr.cnrs.opentheso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/v2/thesaurus/browse.xhtml");
        registry.addViewController("/reset-password").setViewName("forward:/reset-password.xhtml");
        registry.addViewController("/profile").setViewName("forward:/profile/myAccount.xhtml");
        registry.addViewController("/v2/profile").setViewName("forward:/v2/user/my-account.xhtml");
        registry.addViewController("/v2/projects-users").setViewName("forward:/v2/project/my-projects.xhtml");
        registry.addViewController("/v2/admin/users").setViewName("forward:/v2/admin/all-users.xhtml");
        registry.addViewController("/v2/admin/projects").setViewName("forward:/v2/admin/all-projects.xhtml");
        registry.addViewController("/v2/admin/thesauri").setViewName("forward:/v2/admin/all-thesauri.xhtml");
        registry.addViewController("/v2/settings/preference").setViewName("forward:/v2/setting/preference.xhtml");
        registry.addViewController("/v2/settings/identifier").setViewName("forward:/v2/setting/identifier.xhtml");
        registry.addViewController("/v2/settings/corpus").setViewName("forward:/v2/setting/corpus.xhtml");
        registry.addViewController("/v2/toolbox/edition").setViewName("forward:/v2/toolbox/edition.xhtml");
        registry.addViewController("/v2/toolbox/flags").setViewName("forward:/v2/toolbox/flag.xhtml");
        registry.addViewController("/v2/toolbox/workshop").setViewName("forward:/v2/toolbox/workshop.xhtml");
        registry.addViewController("/v2/toolbox/maintenance").setViewName("forward:/v2/toolbox/maintenance.xhtml");
        registry.addViewController("/v2/toolbox/statistics").setViewName("forward:/v2/toolbox/statistics.xhtml");
        registry.addViewController("/candidat").setViewName("forward:/candidat/candidat.xhtml");
        registry.addViewController("/v2/candidat").setViewName("forward:/v2/candidat/candidat.xhtml");
        registry.addViewController("/v2/graph").setViewName("forward:/v2/graph/graph.xhtml");
        registry.addViewController("/v2/graph/visualize/thesaurus").setViewName("forward:/v2/graph/visualize/thesaurus.xhtml");
        registry.addViewController("/v2/graph/visualize/branch").setViewName("forward:/v2/graph/visualize/branch.xhtml");
        registry.addViewController("/v2/graph/visualize/force").setViewName("forward:/v2/graph/visualize/force.xhtml");
        registry.addViewController("/v2/thesaurus").setViewName("forward:/v2/thesaurus/browse.xhtml");
        registry.addViewController("/toolbox/edition").setViewName("forward:/toolbox/edition.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
