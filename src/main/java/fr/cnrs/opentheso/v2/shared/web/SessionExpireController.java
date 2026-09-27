package fr.cnrs.opentheso.v2.shared.web;

import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Expiration idle V2 : invalide la session, ou prolonge via keep-alive.
 */
@Controller
@RequiredArgsConstructor
public class SessionExpireController {

    private final SessionLifecycleService sessionLifecycleService;

    @GetMapping("${opentheso.v2.expire-path}")
    public void expire(HttpServletRequest request, HttpServletResponse response) throws IOException {
        sessionLifecycleService.expireAndRedirect(request, response);
    }

    @GetMapping("${opentheso.v2.keepalive-path}")
    @ResponseBody
    public ResponseEntity<Void> keepAlive(HttpServletRequest request) {
        if (!sessionLifecycleService.touch(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.noContent().build();
    }
}
