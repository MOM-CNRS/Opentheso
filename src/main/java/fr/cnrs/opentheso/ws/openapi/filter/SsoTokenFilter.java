package fr.cnrs.opentheso.ws.openapi.filter;

import fr.cnrs.opentheso.v2.shared.auth.SsoTokenService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class SsoTokenFilter implements Filter {

    private final SsoTokenService ssoTokenService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ssoToken = request.getParameter("ssoToken");

        if (ssoToken != null && !ssoToken.isBlank()) {
            Integer userId = ssoTokenService.validateAndConsumeToken(ssoToken);
            if (userId != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("ssoUserId", userId);
                session.setAttribute("ssoProcessed", false);

                String idc = request.getParameter("idc");
                String idt = request.getParameter("idt");

                if (idc != null && !idc.isBlank()) {
                    session.setAttribute("ssoIdc", idc);
                }
                if (idt != null && !idt.isBlank()) {
                    session.setAttribute("ssoIdt", idt);
                }

                StringBuilder redirect = new StringBuilder(request.getContextPath() + "/v2/thesaurus");
                if (idc != null && !idc.isBlank() && idt != null && !idt.isBlank()) {
                    redirect.append("?idt=").append(idt).append("&idc=").append(idc);
                } else if (idt != null && !idt.isBlank()) {
                    redirect.append("?idt=").append(idt);
                }

                response.sendRedirect(redirect.toString());
                return;
            }

            log.warn("SSO token invalide ou expiré : {}", ssoToken);
            response.sendRedirect(request.getContextPath() + "/v2/thesaurus?ssoError=true");
            return;
        }

        chain.doFilter(req, res);
    }
}
