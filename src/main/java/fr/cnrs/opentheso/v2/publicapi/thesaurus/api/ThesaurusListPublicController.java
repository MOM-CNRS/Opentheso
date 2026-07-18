package fr.cnrs.opentheso.v2.publicapi.thesaurus.api;

import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.service.ThesaurusPublicReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("v2PublicThesaurusListController")
@RequestMapping("/openapi/v2/public/thesauri")
@RequiredArgsConstructor
@Tag(name = "Thesaurus (public)", description = "Liste des thésaurus publics (v2, sans authentification)")
public class ThesaurusListPublicController {

    private final ThesaurusPublicReadService thesaurusPublicReadService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste tous les thésaurus publics avec leur type et leurs traductions")
    public List<PublicThesaurusSummaryResponse> listPublicThesauri() {
        return thesaurusPublicReadService.listPublicThesauri();
    }
}
