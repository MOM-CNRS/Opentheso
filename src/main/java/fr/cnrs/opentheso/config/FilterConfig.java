package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.security.SsoTokenService;
import fr.cnrs.opentheso.ws.openapi.filter.SsoTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final SsoTokenService ssoTokenService;
    private final UserService userService;

    @Bean
    public FilterRegistrationBean<SsoTokenFilter> ssoTokenFilter() {
        FilterRegistrationBean<SsoTokenFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SsoTokenFilter(ssoTokenService, userService)); // injection manuelle
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // priorité haute
        return bean;
    }
}
