package fr.cnrs.opentheso.v2.stats.intercept;

import fr.cnrs.opentheso.v2.stats.service.StatEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiStatInterceptor implements HandlerInterceptor {

    private final StatEventService statEventService;

    public ApiStatInterceptor(StatEventService statEventService) {
        this.statEventService = statEventService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (uri.contains("/v2/api") || uri.contains("/v2-preview/api")) {
            return true;
        }
        if (uri.contains("/api") || uri.contains("/openapi")) {
            statEventService.logApiCall(request.getRequestURL().toString(), request.getMethod());
        }
        return true;
    }
}
