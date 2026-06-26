package fr.cnrs.opentheso.ws.openapi.filter;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.security.SsoTokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor  // injection via constructeur (Spring)
public class SsoTokenFilter implements Filter {

    private final SsoTokenService ssoTokenService;
    private final UserService userService;

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

                // Récupérer idc et idt depuis l'URL
                String idc = request.getParameter("idc");
                String idt = request.getParameter("idt");

                if (idc != null && !idc.isBlank()) session.setAttribute("ssoIdc", idc);
                if (idt != null && !idt.isBlank()) session.setAttribute("ssoIdt", idt);

                // Construire la redirection finale avec idc et idt
                StringBuilder redirect = new StringBuilder(request.getContextPath() + "/index.xhtml");
                if (idc != null && idt != null) {
                    redirect.append("?idc=").append(idc).append("&idt=").append(idt);
                }

                response.sendRedirect(redirect.toString());
                return;
            }

            log.warn("SSO token invalide ou expiré : {}", ssoToken);
            response.sendRedirect(request.getContextPath() + "/index.xhtml?ssoError=true");
            return;
        }

        chain.doFilter(req, res);
    }
}