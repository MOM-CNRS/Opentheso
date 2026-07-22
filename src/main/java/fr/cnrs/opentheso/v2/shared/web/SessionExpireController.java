package fr.cnrs.opentheso.v2.shared.web;

import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

/**
 * Point d'entrée HTTP pour l'expiration proactive (idleMonitor) :
 * invalide la session puis redirige vers l'accueil V2.
 */
@Controller
@RequiredArgsConstructor
public class SessionExpireController {

    private final SessionLifecycleService sessionLifecycleService;

    @GetMapping(SessionLifecycleService.EXPIRE_PATH)
    public void expire(HttpServletRequest request, HttpServletResponse response) throws IOException {
        sessionLifecycleService.expireAndRedirect(request, response);
    }
}
