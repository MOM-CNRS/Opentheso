package fr.cnrs.opentheso.v2.publicapi.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("v2PublicPingController")
@RequestMapping("/openapi/v2/public/ping")
@Tag(name = "Ping (public)", description = "Vérification de la disponibilité du service (v2)")
public class PingPublicController {

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Vérifie que le service fonctionne", description = "Retourne pong")
    public String ping() {
        return "pong";
    }
}
