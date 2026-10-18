package fr.cnrs.opentheso.publicapi.thesaurus.api;

import fr.cnrs.opentheso.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.publicapi.thesaurus.service.ThesaurusPublicReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("publicThesaurusListController")
@RequestMapping("/openapi/v2/public/thesauri")
@RequiredArgsConstructor
@Tag(name = "Thesaurus (public)", description = "Liste des thésaurus publics (sans authentification)")
public class ThesaurusListPublicController {

    private final ThesaurusPublicReadService thesaurusPublicReadService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste tous les thésaurus publics avec leur type et leurs traductions")
    public List<PublicThesaurusSummaryResponse> listPublicThesauri() {
        return thesaurusPublicReadService.listPublicThesauri();
    }
}
