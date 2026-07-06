package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptIdentifierWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptIdentifierMutationService {

    private final ConceptIdentifierWritePort conceptIdentifierWritePort;

    @Transactional
    public MutationResult generateArk(GenerateArkCommand command) {
        return conceptIdentifierWritePort.generateArk(command);
    }

    @Transactional
    public MutationResult deleteArk(DeleteArkCommand command) {
        return conceptIdentifierWritePort.deleteArk(command);
    }

    @Transactional
    public MutationResult generateHandle(GenerateHandleCommand command) {
        return conceptIdentifierWritePort.generateHandle(command);
    }

    @Transactional
    public MutationResult deleteHandle(DeleteHandleCommand command) {
        return conceptIdentifierWritePort.deleteHandle(command);
    }
}
