package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.v2.shared.auth.SsoTokenService;
import fr.cnrs.opentheso.ws.openapi.filter.SsoTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final SsoTokenService ssoTokenService;

    @Bean
    public FilterRegistrationBean<SsoTokenFilter> ssoTokenFilter() {
        FilterRegistrationBean<SsoTokenFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SsoTokenFilter(ssoTokenService));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
