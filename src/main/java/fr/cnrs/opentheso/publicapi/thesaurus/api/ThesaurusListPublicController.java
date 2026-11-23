package fr.cnrs.opentheso.publicapi.thesaurus.api;

import fr.cnrs.opentheso.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.publicapi.thesaurus.service.ThesaurusPublicReadService;
import fr.cnrs.opentheso.ws.api.RestThesaurusVisibility;
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
@Tag(name = "Thesaurus (public)", description = "Sans clé API : thésaurus publics. Avec une clé : publics et projets du profil, ou tous si le profil est super-administrateur")
public class ThesaurusListPublicController {

    private final ThesaurusPublicReadService thesaurusPublicReadService;
    private final RestThesaurusVisibility restThesaurusVisibility;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste les thésaurus visibles selon la clé API, avec leur type et leurs traductions")
    public List<PublicThesaurusSummaryResponse> listPublicThesauri() {
        Integer userId = restThesaurusVisibility.currentUserId();
        return thesaurusPublicReadService.listByIds(restThesaurusVisibility.visibleIds(userId));
    }
}
