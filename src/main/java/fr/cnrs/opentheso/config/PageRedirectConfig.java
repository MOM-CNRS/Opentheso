package fr.cnrs.opentheso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/v2/thesauri.xhtml");
        registry.addViewController("/index").setViewName("forward:/index.xhtml");
        registry.addViewController("/reset-password").setViewName("forward:/reset-password.xhtml");
        registry.addViewController("/profile").setViewName("forward:/profile/myAccount.xhtml");
        registry.addViewController("/candidat").setViewName("forward:/candidat/candidat.xhtml");
        registry.addViewController("/toolbox/edition").setViewName("forward:/toolbox/edition.xhtml");
        registry.addViewController("/v2").setViewName("forward:/v2/thesauri.xhtml");
        registry.addViewController("/v2/thesauri").setViewName("forward:/v2/thesauri.xhtml");
        registry.addViewController("/v2/admin/instance").setViewName("forward:/v2/admin/instance.xhtml");
        registry.addViewController("/v2/admin/projets").setViewName("forward:/v2/admin/projets.xhtml");
        registry.addViewController("/v2/admin/thesauri").setViewName("forward:/v2/admin/thesauri.xhtml");
        registry.addViewController("/v2/admin/utilisateurs").setViewName("forward:/v2/admin/utilisateurs.xhtml");
        registry.addViewController("/v2/setting/preference").setViewName("forward:/v2/setting/preference.xhtml");
        registry.addViewController("/v2/setting/identifiants").setViewName("forward:/v2/setting/identifiants.xhtml");
        registry.addViewController("/v2/setting/corpus").setViewName("forward:/v2/setting/corpus.xhtml");
        registry.addViewController("/v2/setting/parametres").setViewName("forward:/v2/setting/parametres.xhtml");
        registry.addViewController("/v2/project/projets").setViewName("forward:/v2/project/projets.xhtml");
        registry.addViewController("/v2/user/compte").setViewName("forward:/v2/user/compte.xhtml");
        registry.addViewController("/v2/user/preference").setViewName("forward:/v2/user/preference.xhtml");
        registry.addViewController("/v2/proposition/propositions").setViewName("forward:/v2/proposition/propositions.xhtml");
        registry.addViewController("/v2/candidat/candidats").setViewName("forward:/v2/candidat/candidats.xhtml");
        registry.addViewController("/v2/toolbox/atelier").setViewName("forward:/v2/toolbox/atelier.xhtml");
        registry.addViewController("/v2/toolbox/maintenance").setViewName("forward:/v2/toolbox/maintenance.xhtml");
        registry.addViewController("/v2/toolbox/statistiques").setViewName("forward:/v2/toolbox/statistiques.xhtml");
        registry.addViewController("/v2/toolbox/synchronisation").setViewName("forward:/v2/toolbox/synchronisation.xhtml");
        registry.addViewController("/v2/toolbox/portail").setViewName("forward:/v2/toolbox/portail.xhtml");
        registry.addViewController("/v2/toolbox/actions-lot").setViewName("forward:/v2/toolbox/actions-lot.xhtml");
        registry.addViewController("/v2/graph/graphe").setViewName("forward:/v2/graph/graphe.xhtml");
        registry.addViewController("/v2/thesaurus/consultation").setViewName("forward:/v2/thesaurus/consultation.xhtml");
        registry.addViewController("/v2-preview").setViewName("forward:/v2/thesauri.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
