package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.ConceptBlockLayoutDto;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import fr.cnrs.opentheso.v2.user.service.ConceptBlockLayoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class ConceptBlockLayoutApiController {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final ConceptBlockLayoutService conceptBlockLayoutService;

    public ConceptBlockLayoutApiController(
            AuthenticatedUserSource authenticatedUserSource,
            ConceptBlockLayoutService conceptBlockLayoutService
    ) {
        this.authenticatedUserSource = authenticatedUserSource;
        this.conceptBlockLayoutService = conceptBlockLayoutService;
    }

    @GetMapping(value = "/account/concept-blocks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConceptBlockLayoutDto> getLayout() {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toDto(conceptBlockLayoutService.getLayout(userId)));
    }

    @PutMapping(
            value = "/account/concept-blocks",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConceptBlockLayoutDto> saveLayout(@RequestBody(required = false) ConceptBlockLayoutDto body) {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> order = body == null ? List.of() : nullToEmpty(body.order());
        List<String> collapsed = body == null ? List.of() : nullToEmpty(body.collapsed());
        return ResponseEntity.ok(toDto(conceptBlockLayoutService.saveLayout(userId, order, collapsed)));
    }

    private static ConceptBlockLayoutDto toDto(ConceptBlockLayout layout) {
        return new ConceptBlockLayoutDto(layout.order(), new ArrayList<>(layout.collapsed()));
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
