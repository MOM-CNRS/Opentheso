package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TreeStatusPrefDto;
import fr.cnrs.opentheso.v2.user.model.TreeStatusPref;
import fr.cnrs.opentheso.v2.user.service.TreeStatusPrefService;
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
public class TreeStatusPrefApiController {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final TreeStatusPrefService treeStatusPrefService;

    public TreeStatusPrefApiController(
            AuthenticatedUserSource authenticatedUserSource,
            TreeStatusPrefService treeStatusPrefService
    ) {
        this.authenticatedUserSource = authenticatedUserSource;
        this.treeStatusPrefService = treeStatusPrefService;
    }

    @GetMapping(value = "/account/tree-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TreeStatusPrefDto> getPref() {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toDto(treeStatusPrefService.getPref(userId)));
    }

    @PutMapping(
            value = "/account/tree-status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TreeStatusPrefDto> savePref(@RequestBody(required = false) TreeStatusPrefDto body) {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> selected = body == null || body.selected() == null ? List.of() : body.selected();
        return ResponseEntity.ok(toDto(treeStatusPrefService.savePref(userId, selected)));
    }

    private static TreeStatusPrefDto toDto(TreeStatusPref pref) {
        return new TreeStatusPrefDto(new ArrayList<>(pref.selected()));
    }
}
