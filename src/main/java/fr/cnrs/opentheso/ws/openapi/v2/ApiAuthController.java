package fr.cnrs.opentheso.ws.openapi.v2;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.shared.auth.SsoTokenService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
Exemple de fonction pour appeler ce controlleur
async function connecter(idConcept, idThesaurus) {
    const response = await fetch('http://localhost:8099/api/v2/auth/token', {
        method: 'POST',
        headers: {
            'X-API-Key': 'VOTRE_CLE_API_ICI',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            idc: idConcept,   // ex: 42470
            idt: idThesaurus  // ex: th3
        })
    });
    const data = await response.json();
    if (response.ok) {
        window.location.href = data.redirectUrl; // contient déjà idc et idt
    }
}
 */


//@CrossOrigin(origins = "*") // à restreindre en production
@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
@Tag(name = "Api v2")
public class ApiAuthController {

    private final SsoTokenService ssoTokenService;

    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Génère un token SSO",
            description = "Permet à une application tierce de générer un token temporaire " +
                    "pour connecter un utilisateur sans passer par l'écran de login."
    )
    public ResponseEntity<Map<String, String>> generateSsoToken(
            HttpServletRequest requestHeader,
            @RequestBody(required = false) Map<String, String> params
    ) {

        // Récupérer l'utilisateur authentifié via l'interceptor existant (X-API-Key)
        User user = (User) requestHeader.getAttribute("authenticatedUser");
        if (user == null) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }

        // Générer et stocker le token SSO en base (expire dans 5 minutes)
        String token = ssoTokenService.createToken(user.getId());

        // Récupérer idc et idt depuis le body
        String idc = params != null ? params.getOrDefault("idc", "") : "";
        String idt = params != null ? params.getOrDefault("idt", "") : "";

        // Construire l'URL avec les paramètres
        StringBuilder redirectUrl = new StringBuilder("/?ssoToken=").append(token);
        if (!idt.isEmpty()) redirectUrl.append("&idt=").append(idt);
        if (!idc.isEmpty()) redirectUrl.append("&idc=").append(idc);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "redirectUrl", redirectUrl.toString(),
                "expiresIn", "300"
        ));
    }
}