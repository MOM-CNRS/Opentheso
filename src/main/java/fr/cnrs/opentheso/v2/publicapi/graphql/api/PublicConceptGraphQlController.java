package fr.cnrs.opentheso.v2.publicapi.graphql.api;

import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptNode;
import fr.cnrs.opentheso.v2.publicapi.graphql.service.PublicGraphQlConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PublicConceptGraphQlController {

    private final PublicGraphQlConceptService publicGraphQlConceptService;

    @QueryMapping
    public PublicConceptNode publicConcept(
            @Argument String thesaurusId,
            @Argument String conceptId,
            @Argument String lang
    ) {
        return publicGraphQlConceptService.getConcept(thesaurusId, conceptId, lang).orElse(null);
    }

    @QueryMapping
    public List<PublicConceptNode> publicConceptSearch(
            @Argument String thesaurusId,
            @Argument String value,
            @Argument List<String> groupIds,
            @Argument String lang
    ) {
        return publicGraphQlConceptService.searchConcepts(thesaurusId, value, groupIds, lang);
    }
}
