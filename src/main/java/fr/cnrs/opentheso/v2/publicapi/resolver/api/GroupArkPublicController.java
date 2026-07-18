package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.GroupArkLookupResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.GroupArkPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("v2PublicGroupArkController")
@RequestMapping("/openapi/v2/public/groups")
@RequiredArgsConstructor
@Tag(name = "Groupes ARK (public)", description = "Résolution de groupes/collections par identifiant ARK (v2, sans authentification)")
public class GroupArkPublicController {

    private final GroupArkPublicService groupArkPublicService;

    @GetMapping(value = "/ark/{naan}/{arkId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Résout un identifiant ARK vers un groupe/collection")
    public GroupArkLookupResponse resolveGroupByArk(
            @PathVariable String naan,
            @PathVariable String arkId
    ) {
        return groupArkPublicService.resolveGroupByArk(naan, arkId);
    }
}
