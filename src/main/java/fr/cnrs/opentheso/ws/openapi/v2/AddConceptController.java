package fr.cnrs.opentheso.ws.openapi.v2;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import fr.cnrs.opentheso.skos.api.OpenApiConceptCreateOperations;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusWriteAuthorizationService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException;
import org.springframework.http.MediaType;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v2/thesaurus/{idThesaurus}/concepts")
@RequiredArgsConstructor
@Tag(name = "Api v2")
public class AddConceptController {
    private final OpenApiConceptCreateOperations openApiConceptCreateOperations;
    private final ThesaurusWriteAuthorizationService thesaurusWriteAuthorizationService;

    /* exemple complet d'objet Json
{
  "uri": "https://opentheso.huma-num.fr/concept/C_003",
  "localUri": "C_003",
  "identifier": "C_003",
  "permanentId": "ark:/12345/abc123",
  "notation": "B001",
  "resourceType": "CONCEPT",
  "deprecated": false,
  "conceptType": "concept",

  "creatorName": "CNRS",
  "contributorName": [
    "Jean Dupont",
    "Marie Martin"
  ],

  "created": "2024-01-10T12:00:00",
  "modified": "2024-01-10T12:00:00",

  "latitude": 45.74846,
  "longitude": 4.84671,
  "geoGps": "45.74846,4.84671",

  "translations": {
    "prefLabel": {
      "fr": "Archéologie",
      "en": "Archaeology",
      "es": "Arqueología"
    },

    "altLabel": {
      "fr": "Archéo##Science archéologique",
      "en": "Archaeological science"
    },

    "hiddenLabel": {
      "fr": "archaeo"
    },

    "definition": {
      "fr": "Science des sociétés anciennes par l'étude des vestiges",
      "en": "Study of past societies through material remains"
    },

    "scopeNote": {
      "fr": "Terme utilisé pour décrire l'étude scientifique des civilisations passées"
    },

    "historyNote": {
      "fr": "Concept introduit dans la version 2 du thésaurus"
    }
  },

  "rawColumns": {

    "skos:broader": [
      "https://opentheso.huma-num.fr/concept/C_001"
    ],

    "skos:narrower": [
      "https://opentheso.huma-num.fr/concept/C_010",
      "https://opentheso.huma-num.fr/concept/C_011"
    ],

    "skos:related": [
      "https://opentheso.huma-num.fr/concept/C_020"
    ],

    "skos:exactMatch": [
      "http://id.loc.gov/authorities/subjects/sh85006580"
    ],

    "foaf:Image": [
      "rdf:about=https://opentheso.huma-num.fr/images/archaeology1.jpg@@dc:title=Fouille archéologique@@dc:creator=CNRS",
      "rdf:about=https://opentheso.huma-num.fr/images/archaeology2.jpg@@dc:title=Site antique@@dc:creator=INRAP"
    ]
  }
}

     */

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Ajoute un Concept",
            description = "Permet d'ajouter un nouveau concept au thésaurus. si l'identifiant n'est pas fourni, le serveur en génère un automatiquement.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )

    // Le JSON envoyé dans le body est converti en objet SkosConceptDto (@RequestBody),
    // puis validé automatiquement selon les annotations de validation du DTO (@Valid : @NotNull, @NotBlank, etc.)
    public ResponseEntity<SkosConceptDto> createConcept(
            HttpServletRequest requestHeader,
            @PathVariable String idThesaurus,
            @Valid @RequestBody SkosConceptDto dto // @RequestBody convertit le JSON (Jackson) en objet Java et @Valid déclenche la validation des contraintes définies dans SkosConceptDto
    ) {
        // Récupérer l'utilisateur authentifié depuis l'interceptor
        User user = (User) requestHeader.getAttribute("authenticatedUser");
        if (user == null) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }
        if (!thesaurusWriteAuthorizationService.canUserWrite(user.getId(), idThesaurus)) {
            throw new UserCantWriteOnThesaurusException();
        }
        SkosConceptDto saved = openApiConceptCreateOperations.createConcept(dto, idThesaurus, user.getId());
        return ResponseEntity
                .created(URI.create(
                        "/api/v2/thesaurus/" + idThesaurus +
                                "/concepts/" + saved.getIdentifier()))
                .body(saved);
    }
}


