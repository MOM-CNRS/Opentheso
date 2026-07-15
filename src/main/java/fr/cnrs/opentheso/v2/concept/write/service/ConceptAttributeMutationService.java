package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptAttributeWritePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptAttributeMutationService {

    private final ConceptAttributeWritePersistence conceptAttributeWritePersistence;

    @Transactional
    public MutationResult updateNotation(UpdateNotationCommand command) {
        return conceptAttributeWritePersistence.updateNotation(command);
    }

    @Transactional
    public MutationResult updateConceptType(UpdateConceptTypeCommand command) {
        return conceptAttributeWritePersistence.updateConceptType(command);
    }
}
