package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptAttributeWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptAttributeMutationService {

    private final ConceptAttributeWritePort conceptAttributeWritePort;

    @Transactional
    public MutationResult updateNotation(UpdateNotationCommand command) {
        return conceptAttributeWritePort.updateNotation(command);
    }

    @Transactional
    public MutationResult updateConceptType(UpdateConceptTypeCommand command) {
        return conceptAttributeWritePort.updateConceptType(command);
    }
}
