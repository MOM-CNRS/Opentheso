package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.model.ConceptTableRowsResponse;
import fr.cnrs.opentheso.v2.concept.service.ConceptTableConsultationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class TableApiController {

    private final ConceptTableConsultationService conceptTableConsultationService;

    public TableApiController(ConceptTableConsultationService conceptTableConsultationService) {
        this.conceptTableConsultationService = conceptTableConsultationService;
    }

    @GetMapping(value = "/table-rows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ConceptTableRowsResponse tableRows(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return new ConceptTableRowsResponse(List.of(), false);
        }
        String resolvedLang = StringUtils.firstNonBlank(lang, "fr");
        return conceptTableConsultationService.loadRows(thesaurusId, resolvedLang);
    }
}
