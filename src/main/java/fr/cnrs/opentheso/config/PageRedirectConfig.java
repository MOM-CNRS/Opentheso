package fr.cnrs.opentheso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/thesauri.xhtml");
        registry.addViewController("/index").setViewName("forward:/index.xhtml");
        registry.addViewController("/index.xhtml").setViewName("forward:/index.xhtml");
        registry.addViewController("/reset-password").setViewName("forward:/reset-password.xhtml");
        registry.addViewController("/profile").setViewName("forward:/user/compte.xhtml");
        registry.addViewController("/candidat").setViewName("forward:/candidat/candidats.xhtml");
        registry.addViewController("/toolbox/edition").setViewName("forward:/toolbox/atelier.xhtml");
        registry.addViewController("/v2").setViewName("forward:/thesauri.xhtml");
        registry.addViewController("/thesauri").setViewName("forward:/thesauri.xhtml");
        registry.addViewController("/admin/instance").setViewName("forward:/admin/instance.xhtml");
        registry.addViewController("/admin/projets").setViewName("forward:/admin/projets.xhtml");
        registry.addViewController("/admin/thesauri").setViewName("forward:/admin/thesauri.xhtml");
        registry.addViewController("/admin/utilisateurs").setViewName("forward:/admin/utilisateurs.xhtml");
        registry.addViewController("/setting/preference").setViewName("forward:/setting/preference.xhtml");
        registry.addViewController("/setting/identifiants").setViewName("forward:/setting/identifiants.xhtml");
        registry.addViewController("/setting/corpus").setViewName("forward:/setting/corpus.xhtml");
        registry.addViewController("/setting/parametres").setViewName("forward:/setting/parametres.xhtml");
        registry.addViewController("/project/projets").setViewName("forward:/project/projets.xhtml");
        registry.addViewController("/user/compte").setViewName("forward:/user/compte.xhtml");
        registry.addViewController("/user/preference").setViewName("forward:/user/preference.xhtml");
        registry.addViewController("/proposition/propositions").setViewName("forward:/proposition/propositions.xhtml");
        registry.addViewController("/candidat/candidats").setViewName("forward:/candidat/candidats.xhtml");
        registry.addViewController("/toolbox/atelier").setViewName("forward:/toolbox/atelier.xhtml");
        registry.addViewController("/toolbox/maintenance").setViewName("forward:/toolbox/maintenance.xhtml");
        registry.addViewController("/toolbox/statistiques").setViewName("forward:/toolbox/statistiques.xhtml");
        registry.addViewController("/toolbox/synchronisation").setViewName("forward:/toolbox/synchronisation.xhtml");
        registry.addViewController("/toolbox/portail").setViewName("forward:/toolbox/portail.xhtml");
        registry.addViewController("/toolbox/actions-lot").setViewName("forward:/toolbox/actions-lot.xhtml");
        registry.addViewController("/graph/graphe").setViewName("forward:/graph/graphe.xhtml");
        registry.addViewController("/thesaurus/consultation").setViewName("forward:/thesaurus/consultation.xhtml");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
