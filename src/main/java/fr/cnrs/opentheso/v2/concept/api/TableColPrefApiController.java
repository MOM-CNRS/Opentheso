package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TableColPrefDto;
import fr.cnrs.opentheso.v2.user.model.TableColPref;
import fr.cnrs.opentheso.v2.user.service.TableColPrefService;
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
public class TableColPrefApiController {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final TableColPrefService tableColPrefService;

    public TableColPrefApiController(
            AuthenticatedUserSource authenticatedUserSource,
            TableColPrefService tableColPrefService
    ) {
        this.authenticatedUserSource = authenticatedUserSource;
        this.tableColPrefService = tableColPrefService;
    }

    @GetMapping(value = "/account/table-cols", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TableColPrefDto> getPref() {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toDto(tableColPrefService.getPref(userId)));
    }

    @PutMapping(
            value = "/account/table-cols",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TableColPrefDto> savePref(@RequestBody(required = false) TableColPrefDto body) {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> selected = body == null || body.selected() == null ? List.of() : body.selected();
        return ResponseEntity.ok(toDto(tableColPrefService.savePref(userId, selected)));
    }

    private static TableColPrefDto toDto(TableColPref pref) {
        return new TableColPrefDto(new ArrayList<>(pref.selected()));
    }
}
