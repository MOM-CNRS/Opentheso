package fr.cnrs.opentheso.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    private static final int MAX_HTTP_HEADER_SIZE = 262144;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMaxHttpHeaderSizeCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("maxHttpHeaderSize", String.valueOf(MAX_HTTP_HEADER_SIZE));
            connector.setProperty("maxHttpRequestHeaderSize", String.valueOf(MAX_HTTP_HEADER_SIZE));
            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setMaxHttpHeaderSize(MAX_HTTP_HEADER_SIZE);
                protocol.setMaxHttpRequestHeaderSize(MAX_HTTP_HEADER_SIZE);
            }
        });
    }
}
