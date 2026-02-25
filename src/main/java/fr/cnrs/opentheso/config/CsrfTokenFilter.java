package fr.cnrs.opentheso.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;


public class CsrfTokenFilter implements Filter {

    //private static final SecureRandom secureRandom = new SecureRandom();
    private static final String CSRF_TOKEN_ATTRIBUTE = "CSRF_TOKEN";


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();

        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String csrfToken = httpRequest.getParameter("csrfToken");
            String sessionCsrfToken = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE);

            // Valider le token CSRF
            if (sessionCsrfToken == null || !sessionCsrfToken.equals(csrfToken)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        }
        chain.doFilter(httpRequest, httpResponse);
    }
}
