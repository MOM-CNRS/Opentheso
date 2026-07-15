package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.stats.services.StatEventService;
import fr.cnrs.opentheso.services.ThesaurusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private final ThesaurusService thesaurusService;
    private final StatEventService statEventService;

    public LoggingInterceptor(ThesaurusService thesaurusService, StatEventService statEventService) {
        this.thesaurusService = thesaurusService;
        this.statEventService = statEventService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String url = request.getRequestURL().toString();
        if(url == null) return true;

        /* permet de savoir si le thésaurus est privé, on interdit le téléchargement */
        if (url.contains("openapi/v1/thesaurus/")){
            // reste à faire la condition pour le deuxième cas
            var str = url.split("openapi/v1/thesaurus/");
            Boolean isPrivate = thesaurusService.isPrivateThesaurus(str[1].split("/")[0]);
            if(isPrivate == null || isPrivate) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"FORBIDDEN : the thesaurus is privated or not found\"}");
                response.getWriter().flush();
                return false;
            }
        }

        if (url.contains("/api") || url.contains("/openapi")) {
            Map<String, String[]> parameterMap = request.getParameterMap(); // Récupérer tous les paramètres
            String parameters = parameterMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining(", "));
            log.info("Request URL: {}, Method: {}, Query Parameters: {}, IP: {}", request.getRequestURL(), request.getMethod(), parameters, request.getRemoteAddr());
            statEventService.logApiCall(url, request.getMethod());
        }
        return true; // Continue la chaîne des handlers
    }
}
