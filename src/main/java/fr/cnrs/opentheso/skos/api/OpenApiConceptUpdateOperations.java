package fr.cnrs.opentheso.skos.api;

import fr.cnrs.opentheso.models.skos.SkosConceptUpdateDto;
import fr.cnrs.opentheso.services.ConceptUpdateServiceWS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenApiConceptUpdateOperations {

    private final ConceptUpdateServiceWS conceptUpdateServiceWS;

    public boolean exists(String conceptId, String thesaurusId) {
        return conceptUpdateServiceWS.exists(conceptId, thesaurusId);
    }

    public SkosConceptUpdateDto updateConcept(
            SkosConceptUpdateDto dto,
            String thesaurusId,
            String conceptId,
            int userId
    ) {
        return conceptUpdateServiceWS.updateConcept(dto, thesaurusId, conceptId, userId);
    }
}
