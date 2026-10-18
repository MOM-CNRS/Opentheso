package fr.cnrs.opentheso.publicapi.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("publicPingController")
@RequestMapping("/openapi/v2/public/ping")
@Tag(name = "Ping (public)", description = "Vérification de la disponibilité du service")
public class PingPublicController {

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Vérifie que le service fonctionne", description = "Retourne pong")
    public String ping() {
        return "pong";
    }
}
