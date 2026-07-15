package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptIdentifierWritePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptIdentifierMutationService {

    private final ConceptIdentifierWritePersistence conceptIdentifierWritePersistence;

    @Transactional
    public MutationResult generateArk(GenerateArkCommand command) {
        return conceptIdentifierWritePersistence.generateArk(command);
    }

    @Transactional
    public MutationResult deleteArk(DeleteArkCommand command) {
        return conceptIdentifierWritePersistence.deleteArk(command);
    }

    @Transactional
    public MutationResult generateHandle(GenerateHandleCommand command) {
        return conceptIdentifierWritePersistence.generateHandle(command);
    }

    @Transactional
    public MutationResult deleteHandle(DeleteHandleCommand command) {
        return conceptIdentifierWritePersistence.deleteHandle(command);
    }
}
