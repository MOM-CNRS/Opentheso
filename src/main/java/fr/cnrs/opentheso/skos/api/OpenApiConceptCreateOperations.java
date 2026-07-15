package fr.cnrs.opentheso.skos.api;

import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import fr.cnrs.opentheso.services.ConceptAddServiceWS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenApiConceptCreateOperations {

    private final ConceptAddServiceWS conceptAddServiceWS;

    public SkosConceptDto createConcept(SkosConceptDto dto, String thesaurusId, int userId) {
        return conceptAddServiceWS.createConcept(dto, thesaurusId, userId);
    }
}
