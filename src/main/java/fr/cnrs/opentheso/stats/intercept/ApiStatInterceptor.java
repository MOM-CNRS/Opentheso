package fr.cnrs.opentheso.stats.intercept;

import fr.cnrs.opentheso.stats.service.ConsultationViewLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiStatInterceptor implements HandlerInterceptor {

    private final ConsultationViewLogger consultationViewLogger;

    public ApiStatInterceptor(ConsultationViewLogger consultationViewLogger) {
        this.consultationViewLogger = consultationViewLogger;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri != null && (uri.contains("/openapi") || uri.contains("/api/v2"))) {
            consultationViewLogger.logApiCall(request);
        }
        return true;
    }
}
