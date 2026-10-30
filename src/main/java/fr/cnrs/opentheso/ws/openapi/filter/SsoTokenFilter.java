package fr.cnrs.opentheso.ws.openapi.filter;

import fr.cnrs.opentheso.shared.auth.SsoTokenService;
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

        String ssoToken = firstText(
                request.getParameter("ssoToken"),
                fr.cnrs.opentheso.config.JsfPrettyUrlFilter.pathParameter(request, "ssoToken"));

        if (ssoToken != null) {
            Integer userId = ssoTokenService.validateAndConsumeToken(ssoToken);
            if (userId != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("ssoUserId", userId);
                session.setAttribute("ssoProcessed", false);

                String idc = firstText(
                        request.getParameter("idc"),
                        fr.cnrs.opentheso.config.JsfPrettyUrlFilter.pathParameter(request, "idc"));
                String idt = firstText(
                        request.getParameter("idt"),
                        fr.cnrs.opentheso.config.JsfPrettyUrlFilter.pathParameter(request, "idt"));

                if (idc != null) {
                    session.setAttribute("ssoIdc", idc);
                }
                if (idt != null) {
                    session.setAttribute("ssoIdt", idt);
                }

                String path = fr.cnrs.opentheso.config.JsfPrettyUrlFilter.conceptBrowserPath(idt, idc);
                response.sendRedirect(request.getContextPath() + (path == null
                        ? "/index.xhtml"
                        : path));
                return;
            }

            log.warn("Jeton SSO invalide ou expiré");
            response.sendRedirect(request.getContextPath() + "/index.xhtml?ssoError=true");
            return;
        }

        chain.doFilter(req, res);
    }

    private static String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
